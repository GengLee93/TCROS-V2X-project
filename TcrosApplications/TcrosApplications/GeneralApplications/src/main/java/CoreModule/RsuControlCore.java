package CoreModule;

import CommonClass.SrmAssertedType;
import CommonClass.SrmClass.Requestor;
import CommonClass.SrmClass.Requests;
import CommonClass.SsmClass.SigStatus;
import CommonClass.SsmClass.Status;
import CommonClass.TimeQueueManager;
import CommonClass.TimerQueueEntry;
import CommonEnum.RequestStatus;
import CommonEnum.RequestType;
import CommonUtil.ObjectExportUtil;
import CommonUtil.TcrosBuilder.MapDataBuilder;
import CommonUtil.TcrosBuilder.SpatBuilder;
import CommonUtil.TcrosBuilder.SsmBuilder;
import CommonUtil.TcrosBuilder.RsaBuilder;
import CommonUtil.TcrosValidator.TcrosValidator;
import Configurations.RsuConfiguration;
import Configurations.TrafficLightInfo;
import Tcros2MosaicProtocol.TcrosProtocolV2xMessage;
import Tcros2MosaicProtocol.TrafficLightMessage.RsuTrafficLightMessage;
import Tcros2MosaicProtocol.TrafficLightMessage.TrafficLightControlInfo;
import Tcros2MosaicProtocol.TrafficLightMessage.TrafficLightStatusMessage;
import Tcros2MosaicProtocol.TrafficLightMessage.TrafficLightsStateInfo;
import TcrosProtocols.*;
import Util.PositionUtil;
import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.geo.MutableGeoPoint;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.rti.TIME;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * RSU 控制核心
 * 主要負責：
 * 1. 交通號誌控制
 * 2. 車輛請求管理
 * 3. V2X 消息處理
 */
public class RsuControlCore {
    private final GeoPoint geoPoint;
    private final RsuConfiguration rsuConfiguration;
    private final Long priorityStartTime;
    private final Long priorityEndTime;
    private final Path logPath;
    private Long simTime;

    // 時間佇列管理器，負責管理多個不同狀態的車輛請求佇列
    private TimeQueueManager<SignalRequestMessage> timeQueueManager;
    private TimeQueueManager<RoadSideAlert> rsaTimeQueueManager;

    // 交通號誌狀態資訊對應表，key 為節點 ID，value 為號誌狀態
    private Map<String, TrafficLightsStateInfo> trafficLightsStateInfoMap;

    // ==================== 訊息記錄 ====================
    private final List<V2XMapData> mapDataSentRecords;
    private final List<SPaTData> spatSentRecords;
    private final List<SignalStatusMessage> ssmSentRecords;
    private final List<RoadSideAlert> rsaSentRecords;

    // ==================== 系統常數 ====================
    private static final int IN_QUEUE_TIME_LIMIT = 3;
    private static final int WAIT_QUEUE_TIME_LIMIT = 3;
    private static final int REJECT_QUEUE_TIME_LIMIT = 10;
    private static final int TIMER_INTERVAL = 1;

    // ==================== 佇列名稱常數 ====================
    private static final String GRANTED_QUEUE = "grantedQueue";     // 已授權狀態，表示請求已被接受並獲得優先權
    private static final String WAIT_QUEUE = "waitQueue";           // 等待狀態，已收到請求等待處理
    private static final String REQUESTED_QUEUE = "requestedQueue"; // 初始請求狀態，車輛首次發送優先號誌請求
    private static final String PASSED_QUEUE = "passedQueue";       // 已通過狀態，車輛已通過路口
    private static final String REJECT_QUEUE = "rejectQueue";       // 拒絕狀態，請求被拒絕或超時
    private static final String RSA_QUEUE = "rsaQueue";             // 路側事件警告(Road Side Alert) 事件池

    /**
     * @param point RSU 地理位置
     * @param configuration RSU 設定資訊
     * @param
     */
    public RsuControlCore( GeoPoint point, RsuConfiguration configuration,Path lPath){
        geoPoint = point;
        simTime = 0L;
        logPath = Path.of(lPath.toString());
        rsuConfiguration = configuration;
        priorityStartTime = configuration.priorityStartTime * TIME.SECOND;
        priorityEndTime = configuration.priorityEndTime * TIME.SECOND;
        mapDataSentRecords = new ArrayList<>();
        spatSentRecords = new ArrayList<>();
        ssmSentRecords = new ArrayList<>();
        rsaSentRecords = new ArrayList<>();
        initTimerManager();
        initTrafficLightConfiguration();
    }

    /**
     * 初始化時間佇列管理器
     * 註冊所有必要的佇列:
     */
    private void initTimerManager() {
        timeQueueManager = new TimeQueueManager<>();
        timeQueueManager.registerTimeQueue(GRANTED_QUEUE);
        timeQueueManager.registerTimeQueue(REQUESTED_QUEUE);
        timeQueueManager.registerTimeQueue(PASSED_QUEUE);
        timeQueueManager.registerTimeQueue(WAIT_QUEUE);
        timeQueueManager.registerTimeQueue(REJECT_QUEUE);

        // RSA Queue 用來處理多筆緊急事件
        rsaTimeQueueManager = new TimeQueueManager<>();
        rsaTimeQueueManager.registerTimeQueue(RSA_QUEUE);
    }

    /**
     * 初始化交通號誌配置
     * 為每個交通號誌節點創建狀態 Mapping
     */
    private void initTrafficLightConfiguration(){
        trafficLightsStateInfoMap = new HashMap<>();
        for (TrafficLightInfo trafficLightInfo : rsuConfiguration.trafficLightInfoList){
            trafficLightsStateInfoMap.put(trafficLightInfo.nodeId,null);
        }
    }

    public void handleMessage(V2xMessage receivedMessage){
        if(receivedMessage instanceof TcrosProtocolV2xMessage<?> message &&
            Objects.equals(message.getProtocolClassName(), SignalRequestMessage.class.getName())){
            handleSRM((TcrosProtocolV2xMessage<SignalRequestMessage>) message);
        }
        else if(receivedMessage instanceof TcrosProtocolV2xMessage<?> message &&
                Objects.equals(message.getProtocolClassName(), EmergencyVehicleAlert.class.getName())) {
            handleEVA((TcrosProtocolV2xMessage<EmergencyVehicleAlert>) message);
        }
        else if(receivedMessage instanceof TrafficLightStatusMessage message){
            handleTrafficLightStatusMessage(message);
        }
    }

    /**
     * 處理優先號誌請求訊息(SRM)
     * 根據請求類型將車輛分配到相應佇列
     *
     * @param message 收到的SRM消息
     */
    private void handleSRM(TcrosProtocolV2xMessage<SignalRequestMessage> message){
        // 獲取車輛ID和SRM消息
        String vehicleId = message.getSenderId();
        SignalRequestMessage srm = message.getTcrosProtocol();

        // 判斷請求類型
        SrmAssertedType assertedType = assertRequestTypeBySrm(srm);
        if(assertedType != null) {
            if (assertedType == SrmAssertedType.PASSED) {
                addToPassedQueue(vehicleId, srm);
            } else if (assertedType == SrmAssertedType.REQUESTED) {
                addToRequestedQueue(vehicleId, srm);
            } else if (assertedType == SrmAssertedType.UPDATE) {
                addToWaitQueue(vehicleId, srm);
            } else if (assertedType == SrmAssertedType.REJECT) {
                addToRejectedQueue(vehicleId, srm);
            }
        }
    }

    /**
     * 將車輛狀態設置為「已通過」
     * 狀態轉換規則：
     * - 允許從：授權/等待/請求佇列 轉入
     * - 不允許：車輛在 REJECT_QUEUE 中
     * - 互斥條件：一旦進入此狀態，會自動從其他狀態移除
     *
     * @param vehicleId 車輛ID
     * @param srm 信號請求消息
     */
    public void addToPassedQueue(String vehicleId,SignalRequestMessage srm){
        timeQueueManager.addTimeQueueEntryCondition(
            PASSED_QUEUE,
            vehicleId,
            createTimeQueueEntry(srm,IN_QUEUE_TIME_LIMIT),
            List.of(REJECT_QUEUE),
            List.of(GRANTED_QUEUE,WAIT_QUEUE,REQUESTED_QUEUE)
        );
    }

    /**
     * 將車輛狀態設置為「初始請求」
     * 條件：
     * - 僅允許：新進入系統的車輛
     * - 不允許：已在任何其他佇列中的車輛
     * - 互斥條件：車輛必須不在任何其他狀態中
     *
     * @param vehicleId 車輛ID
     * @param srm 信號請求消息
     */
    public void addToRequestedQueue(String vehicleId,SignalRequestMessage srm){
        timeQueueManager.addTimeQueueEntryCondition(
            REQUESTED_QUEUE,
            vehicleId,
            createTimeQueueEntry(srm,IN_QUEUE_TIME_LIMIT),
            List.of(REJECT_QUEUE,GRANTED_QUEUE,WAIT_QUEUE,PASSED_QUEUE),
            List.of()
        );
    }

    /**
     * 將車輛狀態設置為「等待中」
     * 條件：
     * - 允許從：請求佇列 轉入
     * - 特殊處理：如果車輛已獲授權，則更新授權狀態
     * - 互斥條件：不能同時在拒絕/通過/授權狀態
     *
     * @param vehicleId 車輛ID
     * @param srm 信號請求消息
     */
    public void addToWaitQueue(String vehicleId,SignalRequestMessage srm){
        if (isVehicleGranted(vehicleId)) {
            setGrantedVehicle(vehicleId,srm );
        } else {
            timeQueueManager.addTimeQueueEntryCondition(
                WAIT_QUEUE,
                vehicleId,
                createTimeQueueEntry(srm,WAIT_QUEUE_TIME_LIMIT),
                List.of(REJECT_QUEUE,PASSED_QUEUE,GRANTED_QUEUE),
                List.of(REQUESTED_QUEUE)
            );
        }
    }

    /**
     * 將車輛狀態設置為「已拒絕」
     * 條件：
     * - 允許從：任何其他狀態 轉入
     * - 強制處理：進入此狀態會清除車輛在所有其他佇列的記錄
     * - 互斥條件：此狀態具有最高優先級，會強制解除其他所有狀態
     *
     * @param vehicleId 車輛ID
     * @param srm 信號請求消息
     */
    public void addToRejectedQueue(String vehicleId,SignalRequestMessage srm){
        timeQueueManager.addTimeQueueEntryCondition(
            REJECT_QUEUE,
            vehicleId,
            createTimeQueueEntry(srm,REJECT_QUEUE_TIME_LIMIT),
            List.of(),
            List.of(REQUESTED_QUEUE,WAIT_QUEUE,PASSED_QUEUE,GRANTED_QUEUE)
        );
    }

    public RsuConfiguration getRsuConfiguration(){
        return rsuConfiguration;
    }

    /**
     * 創建標準時間佇列條目
     *
     * @param srm 信號請求消息
     * @return 設置了標準超時時間的佇列條目
     */
    private TimerQueueEntry<SignalRequestMessage> createTimeQueueEntry(SignalRequestMessage srm,int inQueueLimit){
        return new TimerQueueEntry<>(srm,inQueueLimit,TIMER_INTERVAL);
    }

    /**
     * 分析SRM請求類型
     * 根據請求內容返回相應的斷言類型:
     * - PASSED: 取消請求
     * - UPDATE: 更新請求
     * - REQUESTED: 新請求
     *
     * @param srm 信號請求消息
     * @return 請求類型的斷言結果
     */
    public SrmAssertedType assertRequestTypeBySrm(SignalRequestMessage srm){
        List<String> requestTlIdList = getRequestTrafficLightsBySrm(srm);
        //只將對RSU控制的紅綠燈發出的要求加入Queue
        if(requestTlIdList.isEmpty())
            return null;

        if(inPriority() && isSrmValidate(srm)) {
            if (needCancellation(srm, requestTlIdList)) {
                return SrmAssertedType.PASSED;
            } else {
                //當存在一個要求為priorityRequestUpdate時，皆視為priorityRequestUpdate
                //當存在priorityRequest且無priorityRequestUpdate時，視為priorityRequest
                for (String requestTlId : requestTlIdList) {
                    RequestType requestType = srm.getRequestType(requestTlId);
                    if (requestType == RequestType.priorityRequestUpdate) {
                        return SrmAssertedType.UPDATE;
                    }
                }
                return SrmAssertedType.REQUESTED;
            }
        }else{
            return SrmAssertedType.REJECT;
        }
    }

    private boolean inPriority() {
        return simTime >= priorityStartTime && simTime <= priorityEndTime;
    }

    private boolean needCancellation(SignalRequestMessage srm, List<String> requestTlIdList){
        if(requestTlIdList.size() == 1){
            return RequestType.priorityCancellation.equals(srm.getRequestType(requestTlIdList.get(0)));
        }else{
            return false;
        }
    }

    private boolean isSrmValidate(SignalRequestMessage srm){
        return TcrosValidator.validate(srm).isEmpty();
    }

    private void handleTrafficLightStatusMessage(TrafficLightStatusMessage message) {
        TrafficLightsStateInfo trafficLightsStateInfo = message.getTrafficLightStateInfo();
        trafficLightsStateInfoMap.put(
            trafficLightsStateInfo.getNodeId(),
            trafficLightsStateInfo
        );
    }

    /**
     * 更新所有系統狀態
     * - 更新計時器狀態
     * - 更新已授權車輛狀態
     * - 清理過期記錄
     */
    public void updateAllState(Long sTime){
        simTime = sTime;
        updateTimer();
        updateGrantedVehicle();
    }

    private void updateTimer(){
        timeQueueManager.updateAllQueue();
        updateRejectVehicle();
        timeQueueManager.removeAllExpired();
    }

    /**
     * 檢查所有佇列中過期的請求,將其添加到拒絕列表
     *  WAIT_QUEUE      → REJECT_QUEUE
     *  REQUESTED_QUEUE → REJECT_QUEUE
     */
    private void updateRejectVehicle() {
        Map<String,SignalRequestMessage> rejectMap = new HashMap<>();
        /* addToRejectedQueue 操作會影響其他queue，所以先列出所有需要reject的車輛後，再一次reject*/
        for(Map.Entry<String,TimerQueueEntry<SignalRequestMessage>> entry : timeQueueManager.getTimeQueue(WAIT_QUEUE).entrySet()){
            if(entry.getValue().isExpired()){
                rejectMap.put(entry.getKey(),entry.getValue().getMessage());
            }
        }
        for(Map.Entry<String,TimerQueueEntry<SignalRequestMessage>> entry : timeQueueManager.getTimeQueue(REQUESTED_QUEUE).entrySet()){
            if(entry.getValue().isExpired()){
                rejectMap.put(entry.getKey(),entry.getValue().getMessage());
            }
        }
        Map.Entry<String,TimerQueueEntry<SignalRequestMessage>> grantedEntry = getGrantedVehicleEntry();
        if( grantedEntry != null && grantedEntry.getValue().isExpired()){
            rejectMap.put(grantedEntry.getKey(),grantedEntry.getValue().getMessage());
        }
        for(Map.Entry<String,SignalRequestMessage> entry : rejectMap.entrySet()){
            addToRejectedQueue(entry.getKey(),entry.getValue());
        }

    }

    /**
     * 更新已授權車輛的狀態，主要功能是在沒有已授權車輛時，
     * 從 WAIT_QUEUE 或 REQUESTED_QUEUE 取一個授權車輛
     */
    private void updateGrantedVehicle() {
        if(getGrantedVehicleEntry() == null) {
            Map.Entry<String, TimerQueueEntry<SignalRequestMessage>> nearestVehicle = getNearestVehicleFromWaitQueue();
            if (nearestVehicle != null) {
                setGrantedVehicle(nearestVehicle.getKey(), nearestVehicle.getValue().getMessage());
            }
        }
    }

    /**
     * 返回第一個（也是唯一的）授權車輛
     */
    private Map.Entry<String,TimerQueueEntry<SignalRequestMessage>> getGrantedVehicleEntry() {
        Iterator<Map.Entry<String, TimerQueueEntry<SignalRequestMessage>>> iterator = timeQueueManager.getTimeQueue(GRANTED_QUEUE).entrySet().iterator();
        if (iterator.hasNext())
            return iterator.next();
        return null;
    }

    public String getGrantedVehicleId(){
        Map.Entry<String,TimerQueueEntry<SignalRequestMessage>> grantedVehicleEntry = getGrantedVehicleEntry();
        return grantedVehicleEntry == null ? null : grantedVehicleEntry.getKey() ;
    }

    public Set<String> getRequestedVehicleIds(){
        return timeQueueManager.getTimeQueue(REQUESTED_QUEUE).keySet();
    }

    public Set<String> getWaitVehicleIds(){
        return timeQueueManager.getTimeQueue(WAIT_QUEUE).keySet();
    }

    public Set<String> getPassedVehicleIds(){
        return timeQueueManager.getTimeQueue(PASSED_QUEUE).keySet();
    }

    public Set<String> getRejectedVehicleIds(){
        return timeQueueManager.getTimeQueue(REJECT_QUEUE).keySet();
    }

    private void setGrantedVehicle(String vehicleId,SignalRequestMessage srm) {
        TimerQueueEntry<SignalRequestMessage> grantedEntry = createTimeQueueEntry(srm,WAIT_QUEUE_TIME_LIMIT);
        timeQueueManager.clearQueue(GRANTED_QUEUE);
        timeQueueManager.addTimeQueueEntryCondition(
                GRANTED_QUEUE,
                vehicleId,
                grantedEntry,
                List.of(REJECT_QUEUE,PASSED_QUEUE),
                List.of(WAIT_QUEUE,REQUESTED_QUEUE)
        );
    }

    private boolean isVehicleGranted(String vehicleId){
        return timeQueueManager.isKeyInQueue(GRANTED_QUEUE,vehicleId);
    }

    public boolean needSendSsm(){
        return  timeQueueManager.isQueueNotEmpty(REQUESTED_QUEUE) ||
                timeQueueManager.isQueueNotEmpty(WAIT_QUEUE) ||
                timeQueueManager.isQueueNotEmpty(REJECT_QUEUE) ||
                getGrantedVehicleEntry() != null;
    }

    private List<SignalRequestMessage> getMessageList( Map<String, TimerQueueEntry<SignalRequestMessage>> messageMap) {
        List<SignalRequestMessage> srmList = new ArrayList<>();
        for (TimerQueueEntry<SignalRequestMessage> message : messageMap.values()) {
            srmList.add(message.getMessage());
        }
        return srmList;
    }

    private List<String> getRequestTrafficLightsBySrm(SignalRequestMessage srm){
        List<String> requestTLs = new ArrayList<>();
        for (Requests requests :srm.requests()){
            String tlId = String.valueOf(requests.getRequestIntersectionId());
            if(isTrafficLightInControl(tlId)){
                requestTLs.add(tlId);
            }
        }
        return requestTLs;
    }

    private boolean isTrafficLightInControl(String nodeId){
        for (TrafficLightInfo trafficLightInfo : rsuConfiguration.trafficLightInfoList){
            if (trafficLightInfo.nodeId.equals(nodeId)){
                return true;
            }
        }
        return false;
    }

    public SPaTData creatSPaTData(long simOffsetTimeMs){
        SpatBuilder spatBuilder = new SpatBuilder();
        spatBuilder.setIntersections(
                simOffsetTimeMs,
                rsuConfiguration.trafficLightInfoList,
                trafficLightsStateInfoMap
        );
        return spatBuilder.create();
    }

    public void addSpatRecord(SPaTData spatData){
        spatSentRecords.add(spatData);
    }

    public V2XMapData createMapData(){
        MapDataBuilder mapDataBuilder = new MapDataBuilder();
        mapDataBuilder.setMsgIssueRevision(mapDataSentRecords.size())
                      .setIntersection(
                        geoPoint,
                        rsuConfiguration.trafficLightInfoList
                      );
        return mapDataBuilder.create();
    }

    public void addMapDataRecord(V2XMapData mapData){
        mapDataSentRecords.add(mapData);
    }

    public SignalStatusMessage createSsm(long simOffsetTimeMs){
        SsmBuilder ssmBuilder = new SsmBuilder(
            simOffsetTimeMs,
            rsuConfiguration.trafficLightInfoList
        );
        Map.Entry<String, TimerQueueEntry<SignalRequestMessage>> grantedVehicleEntry = getGrantedVehicleEntry();
        if(grantedVehicleEntry != null && grantedVehicleEntry.getValue() != null){
            ssmBuilder.setGrantedVehicle(grantedVehicleEntry.getValue().getMessage());
        }
        ssmBuilder.setRejectVehicle(getMessageList(timeQueueManager.getTimeQueue(REJECT_QUEUE)))
                  .setWaitVehicles(getMessageList(timeQueueManager.getTimeQueue(WAIT_QUEUE)))
                  .setRequestedVehicle(getMessageList(timeQueueManager.getTimeQueue(REQUESTED_QUEUE)))
                  .setSequenceNumber(ssmSentRecords.size());
        return ssmBuilder.create();
    }

    public void addSsmRecord(SignalStatusMessage ssm){
        ssmSentRecords.add(ssm);
    }

    public RsuTrafficLightMessage createTrafficLightMessage(MessageRouting routing,SignalStatusMessage ssm){
        List<TrafficLightControlInfo> trafficLightControlInfoList = new ArrayList<>();
        for(TrafficLightInfo trafficLightInfo : rsuConfiguration.trafficLightInfoList ){
            Status status = ssm.getRequestNodeStatus(trafficLightInfo.nodeId);
            if(status != null) {
                for (SigStatus sigStatus : status.sigStatus()) {
                    if (RequestStatus.granted.equals(sigStatus.status())) {
                        trafficLightControlInfoList.add(
                            new TrafficLightControlInfo(
                                trafficLightInfo.nodeId,
                                sigStatus.inboundOn() == null ? -1 : sigStatus.inboundOn().lane(),
                                sigStatus.outboundOn() == null ? -1 : sigStatus.outboundOn().lane()
                            )
                        );
                    }
                }
            }
        }
        return new RsuTrafficLightMessage(routing,trafficLightControlInfoList);
    }

    private Map.Entry<String,TimerQueueEntry<SignalRequestMessage>> getNearestVehicleFromWaitQueue(){
        double minDistance = Double.MAX_VALUE;
        Map.Entry<String,TimerQueueEntry<SignalRequestMessage>> nearestVehicle = null;
        for (Map.Entry<String,TimerQueueEntry<SignalRequestMessage>> srmEntry : timeQueueManager.getTimeQueue(WAIT_QUEUE).entrySet()){
            SignalRequestMessage srm = srmEntry.getValue().getMessage();
            double distance =  getRequestorDistancePointBySrm(srm);
            if (!Double.isNaN(distance) && distance < minDistance) {
                minDistance = distance;
                nearestVehicle = srmEntry;
            }
        }
        return nearestVehicle;
    }

    private double getRequestorDistancePointBySrm(SignalRequestMessage srm){
        for (TrafficLightInfo tlInfo : rsuConfiguration.trafficLightInfoList){
            Requests requests = srm.getRequest(tlInfo.nodeId);
            TrafficLightsStateInfo requestTrafficLightsStateInfo = trafficLightsStateInfoMap.get(tlInfo.nodeId);
            if(     requests != null &&
                    requestTrafficLightsStateInfo != null &&
                    RequestType.priorityRequestUpdate.equals(requests.request().requestType())){
                Requestor requestor = srm.requestor();
                GeoPoint requestorPoint = new MutableGeoPoint(
                        PositionUtil.toFullDegrees(requestor.position().position().lat()),
                        PositionUtil.toFullDegrees(requestor.position().position().lon())
                );
                GeoPoint requestPoint = requestTrafficLightsStateInfo.getGeoPoint();
                return requestorPoint.distanceTo(requestPoint);
            }
        }
        return Double.NaN;
    }

    /**
     * @param message EVA 消息
     * 分析緊急情況，生成相應的 RSA
     */
    private void handleEVA(TcrosProtocolV2xMessage<EmergencyVehicleAlert> message) {
        EmergencyVehicleAlert eva = message.getTcrosProtocol();
    }

    public boolean needSendRsa() {
        return !rsaTimeQueueManager.getTimeQueue(RSA_QUEUE).isEmpty();
    }

    public RoadSideAlert createRsa(long simOffsetTimeMs) {
        RsaBuilder rsaBuilder = new RsaBuilder(simOffsetTimeMs);
        rsaBuilder.setMsgCnt(1);

        return rsaBuilder.create();
    }

    public void addRsaRecord(RoadSideAlert rsa) { rsaSentRecords.add(rsa); }

    public void exportSentMessage() throws IOException {
        File outputFile = logPath.resolve("SsmRecords.json").toFile();
        ObjectExportUtil.exportTcrosBaseMessage(outputFile,ssmSentRecords);
        outputFile = logPath.resolve("SpatRecords.json").toFile();
        ObjectExportUtil.exportTcrosBaseMessage(outputFile,spatSentRecords);
        outputFile = logPath.resolve("MapDataRecords.json").toFile();
        ObjectExportUtil.exportTcrosBaseMessage(outputFile,mapDataSentRecords);
        outputFile = logPath.resolve("RsaRecords.json").toFile();
        ObjectExportUtil.exportTcrosBaseMessage(outputFile, rsaSentRecords);
    }

    public void exportTrafficLightInfoToJson() throws IOException {
        File outputFile = logPath.resolve("trafficLightsInfo.json").toFile();
        ObjectExportUtil.exportTrafficLightInfoToJson(outputFile,trafficLightsStateInfoMap);
    }
}
