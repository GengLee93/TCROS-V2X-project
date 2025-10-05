package CoreModule;

import CommonClass.DrivingRecord;
import CommonClass.EvaClass.*;
import CommonClass.SrmClass.Requests;
import CommonClass.TimerQueueEntry;
import CommonEnum.*;
import CommonUtil.ObjectExportUtil;
import CommonUtil.TcrosBuilder.EvaBuilder;
import CommonUtil.TcrosBuilder.SrmBuilder;
import Configurations.ObuConfiguration;
import Tcros2MosaicProtocol.TcrosProtocolV2xMessage;
import TcrosProtocols.*;
import Util.TimeUtil;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.eclipse.mosaic.fed.application.ambassador.SimulationKernel;
import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.road.INode;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleData;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleRoute;
import org.eclipse.mosaic.rti.TIME;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ObuControlCore {
    private final Path logPath;                    // 日誌儲存路徑

    // ==================== 車輛識別與狀態 ====================
    private final int vehicleId;                   // 車輛唯一識別碼
    private Double heading;                        // 車輛行進方向（角度）

    // ==================== 全局模擬時間位置 ===================
    private Long simTime;                          // 模擬時間戳

    // ==================== 廣播控制時間 ====================
    private final Long stopBroadcastStartTime;     // 停止廣播起始時間
    private final Long stopBroadcastEndTime;       // 停止廣播結束時間
    private final Long errorSrmBroadcastStartTime; // 錯誤 SRM 廣播起始時間
    private final Long errorSrmBroadcastEndTime;   // 錯誤 SRM 廣播結束時間

    // ==================== 車輛行駛紀錄 ====================
    private final List<Double> speedRecords;       // 速度紀錄
    private final List<DrivingRecord> drivingRecords; // 駕駛行為紀錄

    // ==================== V2X 訊息紀錄 ====================
    private final List<SignalRequestMessage> srmRecords; // SRM 訊息紀錄
    private final List<SignalStatusMessage> ssmRecords;  // SSM 訊息紀錄
    private final List<EmergencyVehicleAlert> evaRecords;// EVA 訊息紀錄

    // ==================== 計時器模組 ====================
    private TimerQueueEntry<SPaTData> spatTimer;         // SPaT 訊息計時器
    private TimerQueueEntry<V2XMapData> mapTimer;        // MAP 訊息計時器

    // ==================== 路線與節點資訊 ====================
    private GeoPoint currentPoint;                 // 當前地理位置
    private INode upcomingNode;                   // 即將抵達的節點
    private INode previousNode;                   // 剛通過的節點
    private VehicleRoute vehicleRoute;            // 車輛路線資訊
    private final List<String> routeConnections;  // 路線連接資訊
    private double lateralLanePosition;           // 車子偏離當前車道距離 (單位: 公尺)
    private int currentLaneIndex;                 // 當前車輛處在的車道
    private int totalLanesCount;                  // 當前路線車道總數
    private int routeLanesIndex;                  // 整條 route 中的 第幾段 connection 的索引位置

    // ==================== 模擬常數 ====================
    private static final int RECEIVED_TIME_OUT_LIMIT = 5; // 接收超時限制（秒）
    private int evaMsgCnt = 0;                    // EVA::MsgCnt

    // ==================== 避讓動作狀態 ====================
    private final Set<String> yieldedToEvIds = new HashSet<>();
    public enum YieldAction { COOPERATE, CHANGE_LANE_LEFT, CHANGE_LANE_RIGHT, STOP, NONE }
    private YieldAction lastYieldAction = YieldAction.NONE; // 上一次執行的避讓動作
    EvPassingDetector detector = new EvPassingDetector();
    private final Map<String, EvPassingDetector> evDetectors = new HashMap<>();

    public ObuControlCore(String vid, ObuConfiguration configuration, Path lPath){
        logPath = Path.of(lPath.toString());
        vehicleId = Integer.parseInt(vid.substring(vid.indexOf("_")+1));
        stopBroadcastStartTime = configuration.stopBroadcastStartTime * TIME.SECOND;
        stopBroadcastEndTime = configuration.stopBroadcastEndTime * TIME.SECOND;
        errorSrmBroadcastEndTime = configuration.errorSrmBroadcastEndTime * TIME.SECOND;
        errorSrmBroadcastStartTime = configuration.stopBroadcastStartTime * TIME.SECOND;
        speedRecords = new ArrayList<>();
        upcomingNode = null;
        previousNode = null;
        vehicleRoute = null;
        currentPoint = null;
        routeConnections = new ArrayList<>();
        totalLanesCount = 0;
        currentLaneIndex = -1;
        routeLanesIndex = -1;
        spatTimer = TimerQueueEntry.emptyEntry();
        mapTimer = TimerQueueEntry.emptyEntry();
        srmRecords = new ArrayList<>();
        ssmRecords = new ArrayList<>();
        drivingRecords = new ArrayList<>();
        evaRecords = new ArrayList<>();
    }

    public static class EvPassingDetector {
        private static final double EARTH_RADIUS = 6378137.0; // WGS84 (meters)
        private double forward = Double.MAX_VALUE;
        private double lateral = Double.MAX_VALUE;
        private double previousForwardComponent = Double.NaN;

        public enum RelativePosition { FRONT, BACK, LEFT, RIGHT }

        /**
         * 將經緯度差轉換成公尺差
         */
        private double[] toMeterDelta(GeoPoint origin, GeoPoint target) {
            double dLat = Math.toRadians(target.getLatitude() - origin.getLatitude());
            double dLon = Math.toRadians(target.getLongitude() - origin.getLongitude());
            double meanLat = Math.toRadians((origin.getLatitude() + target.getLatitude()) / 2.0);

            double x = EARTH_RADIUS * dLon * Math.cos(meanLat); // 東向距離
            double y = EARTH_RADIUS * dLat;                     // 北向距離
            return new double[]{x, y};
        }

        /**
         * 計算緊急車輛相對位置（基於轉公尺後的向量）
         */
        public RelativePosition getRelativePosition(GeoPoint myPos, double myHeading, GeoPoint evPos) {
            double[] delta = toMeterDelta(myPos, evPos);
            double vx = delta[0];  // 東向距離（正為向東）
            double vy = delta[1];  // 北向距離（正為向北）

            // Heading: 北為 0°, 順時針為正 (與真實車輛 heading 相同)
            double hx = Math.sin(Math.toRadians(myHeading));
            double hy = Math.cos(Math.toRadians(myHeading));

            forward = vx * hx + vy * hy;        // 沿行駛方向分量
            lateral = -vx * hy + vy * hx;       // 垂直方向分量（左正右負）

            if (Math.abs(forward) > Math.abs(lateral)) {
                return forward > 0 ? RelativePosition.FRONT : RelativePosition.BACK;
            } else {
                return lateral > 0 ? RelativePosition.LEFT : RelativePosition.RIGHT;
            }
        }

        /**
         * 緊急車輛是否在我車前方（以轉公尺向量計算）
         */
        public boolean isFrontend(GeoPoint myPos, double myHeading, GeoPoint evPos) {
            double[] delta = toMeterDelta(myPos, evPos);
            double hx = Math.sin(Math.toRadians(myHeading));
            double hy = Math.cos(Math.toRadians(myHeading));
            forward = delta[0] * hx + delta[1] * hy;
            return forward > 0;
        }

        /**
         * 緊急車輛是否在我車後方
         */
        public boolean isBehind(GeoPoint myPos, double myHeading, GeoPoint evPos) {
            double[] delta = toMeterDelta(myPos, evPos);
            double hx = Math.sin(Math.toRadians(myHeading));
            double hy = Math.cos(Math.toRadians(myHeading));
            forward = delta[0] * hx + delta[1] * hy;
            return forward < 0;
        }

        /**
         * 緊急車輛是否在我左側
         */
        public boolean isLeftOfMe(GeoPoint myPos, double myHeading, GeoPoint evPos) {
            double[] delta = toMeterDelta(myPos, evPos);
            double hx = Math.sin(Math.toRadians(myHeading));
            double hy = Math.cos(Math.toRadians(myHeading));
            double lateral = -delta[0] * hy + delta[1] * hx;
            return lateral > 0;
        }

        /**
         * 緊急車輛是否在我右側
         */
        public boolean isRightOfMe(GeoPoint myPos, double myHeading, GeoPoint evPos) {
            double[] delta = toMeterDelta(myPos, evPos);
            double hx = Math.sin(Math.toRadians(myHeading));
            double hy = Math.cos(Math.toRadians(myHeading));
            double lateral = -delta[0] * hy + delta[1] * hx;
            return lateral < 0;
        }

        /** 重置狀態 */
        public void reset() { previousForwardComponent = Double.NaN; }
    }

    public void updateVehicleData(@NotNull VehicleData newVehicleData,Long sTime) {
        simTime = sTime;
        speedRecords.add(newVehicleData.getSpeed());
        currentPoint = newVehicleData.getPosition();
        lateralLanePosition = newVehicleData.getRoadPosition().getLateralLanePosition();
        currentLaneIndex = newVehicleData.getRoadPosition().getLaneIndex();
        totalLanesCount = newVehicleData.getRoadPosition().getConnection().getLanes();

        INode nextNode = newVehicleData.getRoadPosition().getConnection().getEndNode();
        if(!Objects.equals(upcomingNode, nextNode)){
            if(upcomingNode != null) {
                drivingRecords.add(
                    new DrivingRecord(upcomingNode.getId(), sTime / TIME.SECOND)
                );
            }
            updateUpcomingNode(nextNode);
        }else{
            mapTimer.updateTimer();
            spatTimer.updateTimer();
        }
        updateRouteInfo(newVehicleData);
        heading = newVehicleData.getHeading();
    }

    private void updateRouteInfo(@NotNull VehicleData vehicleData){
        if(vehicleRoute == null){
            vehicleRoute = SimulationKernel.SimulationKernel.getRoutes().get(vehicleData.getRouteId());
            createRouteLanes();
        }else{
            updateRouteLaneIndex(vehicleData.getRoadPosition().getConnectionId());
        }
    }

    private void createRouteLanes(){
        if(vehicleRoute == null)
            return;
        routeConnections.addAll(vehicleRoute.getConnectionIds());
        routeLanesIndex = 0;
    }

    private void updateRouteLaneIndex(String connectionId){
        for(int i = 0; i < routeConnections.size() ; i++){
            if(routeConnections.get(i).equals(connectionId)){
                routeLanesIndex = i;
                return;
            }
        }
        routeLanesIndex = -1;
    }

    private String getCurrentLane(){
        if(routeLanesIndex >=0 && routeLanesIndex < routeConnections.size() ){
            return routeConnections.get(routeLanesIndex).split("_")[0];
        }
        return null;
    }

    private String getNextLane(){
        if(routeLanesIndex >=0 && routeLanesIndex < (routeConnections.size()-1) ){
            return routeConnections.get(routeLanesIndex+1).split("_")[0];
        }
        return null;
    }

    private void updateUpcomingNode(INode nextNode){
        previousNode = upcomingNode;
        upcomingNode = nextNode;
        if( !spatTimer.isEmpty() &&
            !spatTimer.getMessage().containNode(nextNode.getId())){
            spatTimer = TimerQueueEntry.emptyEntry();
        }
        if( !mapTimer.isEmpty() &&
            !mapTimer.getMessage().containNode(nextNode.getId())) {
            mapTimer = TimerQueueEntry.emptyEntry();
        }
    }

    public INode getUpcomingNode(){return upcomingNode;}
    public INode getPreviousNode(){return previousNode;}
    public TimerQueueEntry<SPaTData> getSpatTimer(){
        return spatTimer;
    }
    public TimerQueueEntry<V2XMapData> getMapTimer(){
        return mapTimer;
    }
    public boolean needSendSrm(){
        if(inStopDuration())
            return false;
        else
            return needSendPreviousRequest() || needSendUpcomingRequest();
    }
    private boolean inStopDuration(){
        return simTime >= stopBroadcastStartTime && simTime <= stopBroadcastEndTime;
    }
    private boolean inErrorDuration(){return simTime >= errorSrmBroadcastStartTime && simTime <= errorSrmBroadcastEndTime; }
    private boolean needSendUpcomingRequest(){
        return upcomingNode != null &&
               !spatTimer.isEmpty() && !mapTimer.isEmpty() &&
               !spatTimer.isExpired() && !mapTimer.isExpired();
    }
    private boolean needSendPreviousRequest(){
        return previousNode != null;
    }
    public void handleMessage(TcrosProtocolV2xMessage<?> message) {
        String protocolClassName = message.getProtocolClassName();
        if (protocolClassName.equals(SPaTData.class.getName())) {
            handleSpatData((TcrosProtocolV2xMessage<SPaTData>) message);
        } else if (protocolClassName.equals(V2XMapData.class.getName())) {
            handleMapData((TcrosProtocolV2xMessage<V2XMapData>) message);
        } else if (protocolClassName.equals(SignalStatusMessage.class.getName())) {
            handleSsm((TcrosProtocolV2xMessage<SignalStatusMessage>) message);
        }  else if (protocolClassName.equals(EmergencyVehicleAlert.class.getName())) {
            handleEva((TcrosProtocolV2xMessage<EmergencyVehicleAlert>) message);
        } else if (protocolClassName.equals(RoadSideAlert.class.getName())) {
            handleRsa((TcrosProtocolV2xMessage<RoadSideAlert>) message);
        }
    }

    private void handleSpatData(TcrosProtocolV2xMessage<SPaTData> message){
        SPaTData spatData = message.getTcrosProtocol();
        if(spatData.containNode(upcomingNode.getId())){
            spatTimer = new TimerQueueEntry<>(spatData,RECEIVED_TIME_OUT_LIMIT,1);
        }
    }

    private void handleMapData(TcrosProtocolV2xMessage<V2XMapData> message){
        V2XMapData mapData = message.getTcrosProtocol();
        if(mapData.containNode(upcomingNode.getId())){
            mapTimer = new TimerQueueEntry<>(mapData,RECEIVED_TIME_OUT_LIMIT,1);
        }
    }

    private void handleSsm(TcrosProtocolV2xMessage<SignalStatusMessage> message){
        SignalStatusMessage ssm = message.getTcrosProtocol();
        if(ssm.getRequestStatus(upcomingNode.getId(),vehicleId) != null){
            ssmRecords.add(ssm);
        }
    }

    private void handleEva(TcrosProtocolV2xMessage<EmergencyVehicleAlert> message) {
        EmergencyVehicleAlert eva = message.getTcrosProtocol();
        if (eva.rsaMsg().typeEvent() == ITISCode.EMERGENCY_VEHICLE
                && eva.details().responseType() == ResponseType.emergency) {
            GeoPoint evPosition = GeoPoint.latLon(eva.rsaMsg().position().lat() / 1e7
                    , eva.rsaMsg().position().lon() / 1e7);
            handleEmergencyVehicle(evPosition, eva.rsaMsg().position().heading() * 0.0125,
                    eva.id());
        }
    }

    private void handleRsa(TcrosProtocolV2xMessage<RoadSideAlert> message) {
//        RoadSideAlert rsa = message.getTcrosProtocol();
//        if (rsa.typeEvent() == ITISCode.EMERGENCY_VEHICLE) {
//            GeoPoint evPosition = GeoPoint.latLon(rsa.position().lat() / 1e7
//                    , rsa.position().lon() / 1e7);
////            String evId = "RSA_" + rsa.msgCnt();
//            handleEmergencyVehicle(evPosition, rsa.position().heading() * 0.0125, null);
//        }
    }

    private void handleEmergencyVehicle(GeoPoint evPosition, Double evHeading, String evId) {
        if (hasAlreadyYieldedTo(evId)) { lastYieldAction = YieldAction.NONE; return;}
        if (evPosition == null || currentPoint == null || heading == null || evHeading == null) {
            lastYieldAction = YieldAction.NONE;
            return;
        }

//        boolean passed = detector.hasPassed(currentPoint, heading, evPosition);
        EvPassingDetector.RelativePosition pos = detector.getRelativePosition(currentPoint, heading, evPosition);
//        System.out.println("EV 目前在我 " + pos);
//        if (passed) {
//            detector.reset();
//            System.out.println("EV 已成功超車！");
//        }
//        else { System.out.println("EV 目前在我 " + pos); }

        double distance = currentPoint.distanceTo(evPosition);  // 自身與緊急車輛的距離
        double headingDiff = Math.abs(heading - evHeading);     // 自身車輛與緊急車輛的角度差異

        if (distance > 100 || headingDiff > 30) {               // 與事件無關
            System.out.printf("vh%d: 與事件無關%n", vehicleId);
            lastYieldAction = YieldAction.NONE;
            return;
        }

        if (detector.isBehind(currentPoint, heading, evPosition)) {  // 緊急車輛在自身車輛的後方
            System.out.printf("vh%d: EV 在後方%n", vehicleId);

            double myLateralPos = lateralLanePosition;
            double lateralDiff = Math.abs(detector.lateral);  // 公尺距離

            boolean isBlocking = lateralDiff < 1.75 && Math.abs(myLateralPos) < 0.5;

            if (totalLanesCount == 1) {
                // 單線道：若擋到就停靠路邊
                if (isBlocking) {
                    System.out.printf("vh%d: 單線道，正在擋道 -> 臨停%n", vehicleId);
                    lastYieldAction = YieldAction.STOP;
                } else {
                    System.out.printf("vh%d: 單線道但未擋道 -> 不動作", vehicleId);
                    lastYieldAction = YieldAction.NONE;
                }
            } else {
                // 多線道：若擋到則嘗試變換車道
                if (isBlocking) {
                    System.out.printf("vh%d: 多線道且擋道 -> 嘗試變換車道", vehicleId);
                    if (canChangeLaneLeft()) {
                        lastYieldAction = YieldAction.CHANGE_LANE_LEFT;
                    } else if (canChangeLaneRight()) {
                        lastYieldAction = YieldAction.CHANGE_LANE_RIGHT;
                    } else {
                        lastYieldAction = YieldAction.STOP;  // 無法變道，停下
                    }
                } else {
                    System.out.printf("vh%d: 多線道但未擋道 -> 不動作", vehicleId);
                    lastYieldAction = YieldAction.NONE;
                }
            }
        }
    }
    private boolean canChangeLaneLeft() { return  currentLaneIndex < (totalLanesCount - 1); }
    private boolean canChangeLaneRight() { return currentLaneIndex > 0; }

    public void markYieldedTo(String evId) { yieldedToEvIds.add(evId); }
    private boolean hasAlreadyYieldedTo(String evId) {
//         TODO: 判斷是否已經在車輛視界
        return yieldedToEvIds.contains(evId);
    }

//    public void setLastYieldAction(YieldAction lastYieldAction) { this.lastYieldAction = lastYieldAction; }
    public YieldAction getLastYieldAction() { return lastYieldAction; }

    public void clearYieldMemoryIfEvGone(String evId, GeoPoint evPosition) {
        double distance = currentPoint.distanceTo(evPosition);
            yieldedToEvIds.remove(evId);
    }

    public SignalRequestMessage createSRM(long simOffsetTimeMs){
        if(currentPoint != null) {
            SrmBuilder srmBuilder;
            srmBuilder = new SrmBuilder(simOffsetTimeMs, vehicleId, currentPoint);
            srmBuilder.setSequenceNumber(inErrorDuration() ? -1 : srmRecords.size());
            if(needSendUpcomingRequest()) {
                srmBuilder.addUpcomingNodeRequest(
                    upcomingNode.getId(),
                    getUpcomingNodeETCms(),
                    getCurrentLane(),
                    getNextLane(),
                    nextUpcomingRequestType()
                );
            }

            if(needSendPreviousRequest()){
                Requests previousRequest = getNodePreviousRequest(previousNode.getId());
                if(previousRequest != null)
                    srmBuilder.addPreviousNodeRequest(previousRequest);
            }
            return srmBuilder.create();
        }
        return null;
    }

    public void addSrmRecord(SignalRequestMessage srm){ srmRecords.add(srm); }

    private int nextEvaMsgCnt() {
        int current = evaMsgCnt;
        evaMsgCnt = (evaMsgCnt + 1) % 128;
        return current;
    }

    public boolean needSendEva() {
        // TODO: 未實做
        return false;
    }

    public EmergencyVehicleAlert createEva(long simOffsetTimeMs) {
        EvaBuilder evaBuilder = new EvaBuilder(simOffsetTimeMs);

        // 車輛 ID 與基本類型
        evaBuilder.setId("" + vehicleId);
        evaBuilder.setBasicType(BasicType.special);

        // 經緯度轉換為 J2735 格式（1/10 微度）
        long lat10MicroDeg = Math.round(currentPoint.getLatitude() * 10_000_000);
        long lon10MicroDeg = Math.round(currentPoint.getLongitude() * 10_000_000);

        // 高程轉換為 10cm 單位（若無資料則設為 0）
        long elevationDeciMeter = Math.round(currentPoint.getAltitude() * 10);

        // RSA 訊息建構
        evaBuilder.rsaBuilder
                .setMsgCnt(nextEvaMsgCnt())
                .setTypeEvent(ITISCode.EMERGENCY_VEHICLE)
                .setPriority(RsaPriority.PRIORITY_7)
                .setHeadingDegree(heading)
                .setPosition(TimeUtil.toUtcTime(simOffsetTimeMs), lon10MicroDeg, lat10MicroDeg, elevationDeciMeter)
                .setSpeed(TransmissionState.UNAVAILABLE, speedRecords.get(speedRecords.size() - 1));

        // EVA 額外欄位
        evaBuilder.setResponseType(ResponseType.emergency);
        evaBuilder.setDetails(
                new Details(
                        SirenUse.inUse,
                        LightUse.inUse,
                        Multi.singleVehicle,
                        new Events(Event.peEmergencySoundActive),
                        ResponseType.emergency
                )
        );
        evaBuilder.setMass(400);
        evaBuilder.setBasicType(BasicType.special);

        return evaBuilder.create();
    }

    public void addEvaRecord(EmergencyVehicleAlert eva) { evaRecords.add(eva); }

    private Requests getNodePreviousRequest(String nodeId){
        if(!srmRecords.isEmpty()){
            for (int i = srmRecords.size()-1; i >=0 ; i--){
                SignalRequestMessage srm = srmRecords.get(i);
                Requests requests = srm.getRequest(nodeId);
                if(requests != null){
                    return requests;
                }
            }
        }
        return null;
    }

    private RequestType getNodePreviousRequestType(String nodeId){
        if(!srmRecords.isEmpty()){
            for (int i = srmRecords.size()-1; i >=0 ; i--){
                SignalRequestMessage srm = srmRecords.get(i);
                RequestType requestType = srm.getRequestType(nodeId);
                if(requestType != null){
                    return requestType;
                }
            }
        }
        return null;
    }

    private RequestType nextUpcomingRequestType(){
        String nodeId = upcomingNode.getId();
        RequestType previousRequestType = getNodePreviousRequestType(nodeId);
        if(previousRequestType == null){
            return RequestType.priorityRequest;
        } else {
            return RequestType.priorityRequestUpdate;
        }
    }

    public double getAverageSpeed(){
        if (speedRecords.isEmpty()) {
            return 0.0;
        }
        return speedRecords.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    public double getUpcomingNodeETCms(){
        if(upcomingNode != null){
            double averageSpeed = getAverageSpeed();
            if(averageSpeed != 0){
                return (currentPoint.distanceTo(upcomingNode.getPosition())/averageSpeed) * 1000 ;
            }else{
                return Double.POSITIVE_INFINITY;
            }
        }else{
            return Double.NaN;
        }
    }

    public GeoPoint getCurrentPoint(){ return currentPoint; }

    public OptionalLong getTotalDrivingDurationSeconds() {
        if (drivingRecords.size() < 2) return OptionalLong.empty();
        long start = drivingRecords.get(0).sTime();
        long end = drivingRecords.get(drivingRecords.size() - 1).sTime();
        return OptionalLong.of(end - start);
    }

    public void exportSentMessage() throws IOException {
        File outputFile = logPath.resolve("SrmRecords.json").toFile();
        ObjectExportUtil.exportTcrosBaseMessage(outputFile,srmRecords);
        outputFile = logPath.resolve("EvaRecords.json").toFile();
        ObjectExportUtil.exportTcrosBaseMessage(outputFile,evaRecords);
    }

    public void exportDrivingRecords() throws IOException {
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = mapper
                .schemaFor(DrivingRecord.class)
                .withHeader()
                .withColumnSeparator(',');
        String logPrefix = logPath.getParent()
                .getParent()
                .getFileName()
                .toString()
                .replace("log-","");

        File outputFile = logPath.resolve(logPrefix+"_drivingRecord.csv").toFile();
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), StandardCharsets.UTF_8)) {
            mapper.writer(schema).writeValues(writer).writeAll(drivingRecords);
        }
    }
}