package CoreModule;

import CommonClass.DrivingRecord;
import CommonClass.EvaClass.*;
import CommonClass.RsaClass.*;
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
import org.eclipse.mosaic.lib.objects.road.*;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ObuControlCore {
    private final int vehicleId;
    private final Long stopBroadcastStartTime;
    private final Long stopBroadcastEndTime;
    private final Long errorSrmBroadcastStartTime;
    private final Long errorSrmBroadcastEndTime;
    private final List<Double> speedRecords;
    private final List<SignalRequestMessage> srmRecords;
    private final List<SignalStatusMessage> ssmRecords;
    private final List<EmergencyVehicleAlert> evaRecords;
    private final List<DrivingRecord> drivingRecords;
    private final Path logPath;
    private Long simTime;
    private GeoPoint currentPoint;
    private TimerQueueEntry<SPaTData> spatTimer;
    private TimerQueueEntry<V2XMapData> mapTimer;
    private INode upcomingNode;
    private INode previousNode;
    private VehicleRoute vehicleRoute;
    private final List<String> routeConnections;
    private int routeLanesIndex;
    private static final int RECEIVED_TIME_OUT_LIMIT = 5;
    private int evaMsgCnt = 0;
    private Double heading;
    private final String evaVehicleId;

    //給切換的預設值
    private Integer pendingLaneChangeDelta = null; // -1=向右, +1=向左
    private Integer currentLaneIndex = null;
    private String  currentConnectionId = null;

    private static final double AHEAD_BUFFER_METERS = 5.0; // 超過這個距離才算「確定在前面」
    private static final long   EMERGENCY_INFO_TTL  = 5 * TIME.SECOND; // 緊急資訊有效時間

    // 目前所在 connection 的起點/終點（每次 onVehicleUpdated 會更新）
    private GeoPoint currentConnStart = null;
    private GeoPoint currentConnEnd = null;


    public ObuControlCore(String vid, ObuConfiguration configuration, Path lPath){
        logPath = Path.of(lPath.toString());
        vehicleId = Integer.parseInt(vid.substring(vid.indexOf("_")+1));
        stopBroadcastStartTime = configuration.stopBroadcastStartTime * TIME.SECOND;
        stopBroadcastEndTime = configuration.stopBroadcastEndTime * TIME.SECOND;
        errorSrmBroadcastStartTime = configuration.errorSrmBroadcastStartTime * TIME.SECOND;
        errorSrmBroadcastEndTime = configuration.errorSrmBroadcastEndTime * TIME.SECOND;
        speedRecords = new ArrayList<>();
        upcomingNode = null;
        previousNode = null;
        vehicleRoute = null;
        currentPoint = null;
        routeConnections = new ArrayList<>();
        routeLanesIndex = -1;
        spatTimer = TimerQueueEntry.emptyEntry();
        mapTimer = TimerQueueEntry.emptyEntry();
        srmRecords = new ArrayList<>();
        ssmRecords = new ArrayList<>();
        drivingRecords = new ArrayList<>();
        evaRecords = new ArrayList<>();
        evaVehicleId = vid;
    }

    public void updateVehicleData(@NotNull VehicleData newVehicleData, Long sTime) {
        simTime = sTime;
        speedRecords.add(newVehicleData.getSpeed());
        currentPoint = newVehicleData.getPosition();

        IRoadPosition roadPosition   = newVehicleData.getRoadPosition();
        IConnection connection = roadPosition.getConnection();
        INode nextNode = connection.getEndNode();

        currentConnStart = connection.getStartNode().getPosition();
        currentConnEnd = connection.getEndNode().getPosition();
        currentLaneIndex    = roadPosition.getLaneIndex();
        currentConnectionId = roadPosition.getConnectionId();

        heading = newVehicleData.getHeading();
        if (!Objects.equals(upcomingNode, nextNode)) {
            if (upcomingNode != null) {
                drivingRecords.add(new DrivingRecord(upcomingNode.getId(), sTime / TIME.SECOND));
            }
            updateUpcomingNode(nextNode);
        } else {
            mapTimer.updateTimer();
            spatTimer.updateTimer();
        }
        updateRouteInfo(newVehicleData);
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
            // TODO: Refactor this logic post-deadline for better separation of concerns
            String conn = eva.rsaMsg().senderConnectionId();
            Integer lane = eva.rsaMsg().senderLaneIndex();
            PositionInfo pos = eva.rsaMsg().position();

            // 從 TCROS::EVA 中提取緊急車輛位置訊息
            double lat = (pos.lat() == null || pos.lat() == 900000001L) ? 0.0 : pos.lat() / 1e7d;
            double lon = (pos.lon() == null || pos.lon() == 1800000001L) ? 0.0 : pos.lon() / 1e7d;
            double alt = (pos.elevation() == null) ? 0.0 : pos.elevation() / 10.0;

            GeoPoint evPos = GeoPoint.latLon(lat, lon, alt);    // 緊急車輛位置

            lastEmergency = new EmergencyContext(conn, lane, evPos, simTime);
            handleEmergencyVehicle();
        }
    }

    private void handleRsa(TcrosProtocolV2xMessage<RoadSideAlert> message) {
        RoadSideAlert rsa = message.getTcrosProtocol();
        if (rsa.typeEvent() == ITISCode.EMERGENCY_VEHICLE) {
            String conn = rsa.senderConnectionId();   // 如果 RSA 也帶了，沿用；否則可略過不存
            Integer lane = rsa.senderLaneIndex();
            PositionInfo pos = rsa.position();

            double latDeg = (pos.lat() == null) ? Double.NaN : pos.lat() / 1e7d;
            double lonDeg = (pos.lon() == null) ? Double.NaN : pos.lon() / 1e7d;
            double altM   = (pos.elevation() == null) ? 0.0 : pos.elevation() / 10.0;

            GeoPoint evPos = GeoPoint.latLon(latDeg, lonDeg, altM);

            lastEmergency = new EmergencyContext(conn, lane, evPos, simTime);
            handleEmergencyVehicle();
        }
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

    public void addSrmRecord(SignalRequestMessage srm){
        srmRecords.add(srm);
    }

    private void handleEmergencyVehicle() {
        if (!isEmergencyInfoAlive()) return;              // 你的 TTL 檢查
        if (currentConnectionId == null || currentLaneIndex == null) return;

        EmergencyContext ec = lastEmergency;

        if (!currentConnectionId.equals(ec.connId)) return;
        if (!Objects.equals(currentLaneIndex, ec.laneIdx)) return;

        if (currentConnStart == null || currentConnEnd == null || currentPoint == null) return;

        double myProg = projectProgressOnConnection(currentConnStart, currentConnEnd, currentPoint);
        double evProg = projectProgressOnConnection(currentConnStart, currentConnEnd, ec.pos);

        double gap = myProg - evProg;
        if (gap >= AHEAD_BUFFER_METERS) {
            // 往右讓 (-1)
            requestLaneChange(-1);
        }
    }

    private int nextEvaMsgCnt() {
        int current = evaMsgCnt;
        evaMsgCnt = (evaMsgCnt + 1) % 128;
        return current;
    }

    public boolean needSendEva() {
        return needSendSrm();
    }

    public EmergencyVehicleAlert createEva(long simOffsetTimeMs){
        EvaBuilder evaBuilder = new EvaBuilder(simOffsetTimeMs);

        // 安全取值
        int hdg = (heading == null) ? 0 : (int)Math.round(heading);
        double lastSpeed = speedRecords.isEmpty() ? 0.0 : speedRecords.get(speedRecords.size()-1);

        evaBuilder.setId(evaVehicleId);
        evaBuilder.setBasicType(BasicType.special);

        evaBuilder.rsaBuilder
                .setMsgCnt(nextEvaMsgCnt())
                .setTypeEvent(ITISCode.EMERGENCY_VEHICLE)
                .setPriority(RsaPriority.PRIORITY_7)
                .setHeadingByDegree(hdg)
                .setPosition(
                        TimeUtil.toUtcTime(simOffsetTimeMs),
                        (long)Math.round(currentPoint.getLongitude() * 1e7), // lonE7
                        (long)Math.round(currentPoint.getLatitude()  * 1e7), // latE7
                        (long)Math.round(currentPoint.getAltitude()  * 10)   // 0.1m
                )
                .setSpeed(TransmissionState.UNAVAILABLE, lastSpeed)
                //   新增：把 sender 的所在連結/車道放進去
                .setSenderConnectionId(currentConnectionId)
                .setSenderLaneIndex(currentLaneIndex);

        evaBuilder.setResponseType(ResponseType.emergency);
        evaBuilder.setDetails(
                new Details(
                        SirenUse.inUse, LightUse.inUse, Multi.singleVehicle,
                        new Events(Event.peEmergencySoundActive),
                        ResponseType.emergency
                )
        );
        evaBuilder.setMass(400);

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

    //避讓
    public boolean hasPendingLaneChange() { return pendingLaneChangeDelta != null; }

    public int consumeLaneChangeDelta() {
        int d = pendingLaneChangeDelta;
        pendingLaneChangeDelta = null;
        return d;
    }

    private void requestLaneChange(int delta) { pendingLaneChangeDelta = delta; }

    private EmergencyContext lastEmergency = null;
    private record EmergencyContext(
            String connId,
            Integer laneIdx,
            GeoPoint pos,
            long recvSimTime) {
    }

    /**
     * 計算某一點在指定道路連線上的投影進度（以公尺為單位）。
     *
     * @param start  道路連線的起點座標
     * @param end    道路連線的終點座標
     * @param point  要投影的目標點座標
     * @return       該點在連線上的投影距離（單位：公尺）
     */
    private static double projectProgressOnConnection(GeoPoint start, GeoPoint end, GeoPoint point) {
        // 取得起點、終點與目標點的經緯度座標
        double startLon = start.getLongitude(), startLat = start.getLatitude();
        double endLon = end.getLongitude(), endLat = end.getLatitude();
        double pointLon = point.getLongitude(), pointLat = point.getLatitude();

        // 計算起點到終點的向量 (dx, dy)
        double dx = endLon - startLon;
        double dy = endLat - startLat;

        // 計算起點到目標點的向量 (px, py)
        double px = pointLon - startLon;
        double py = pointLat - startLat;

        // 若起點與終點相同，則無法計算投影，直接回傳 0
        double segmentLengthSquared = dx * dx + dy * dy;
        if (segmentLengthSquared == 0) { return 0.0; }

        // 計算目標點在連線上的投影比例（0 表示起點，1 表示終點）
        double projectionRatio = (px * dx + py * dy) / segmentLengthSquared;

        // 將比例轉換為實際距離（單位：公尺）
        double segmentLengthMeters = start.distanceTo(end);
        return projectionRatio * segmentLengthMeters;
    }

    private static INode connStartNode(VehicleData vd) {
        return vd.getRoadPosition().getConnection().getStartNode();
    }
    private static INode connEndNode(VehicleData vd) {
        return vd.getRoadPosition().getConnection().getEndNode();
    }

    /**
     * 判斷目前是否仍有有效的緊急資訊可用。
     * @return true 表示緊急資訊仍在有效期內；false 表示已過期或尚未接收
     */
    private boolean isEmergencyInfoAlive() {
        return lastEmergency != null && (simTime - lastEmergency.recvSimTime) <= EMERGENCY_INFO_TTL;
    }
}