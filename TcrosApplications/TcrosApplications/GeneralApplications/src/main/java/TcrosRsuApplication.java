import CoreModule.RsuControlCore;
import Singleton.RealTimeReferencePoint;
import Configurations.RsuConfiguration;
import Tcros2MosaicProtocol.TcrosProtocolV2xMessage;
import Tcros2MosaicProtocol.TrafficLightMessage.RsuTrafficLightMessage;
import Tcros2MosaicProtocol.TrafficLightMessage.TrafficLightControlInfo;
import TcrosProtocols.*;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.ConfigurableApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.os.RoadSideUnitOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.geo.*;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class TcrosRsuApplication extends ConfigurableApplication<RsuConfiguration, RoadSideUnitOperatingSystem>
        implements CommunicationApplication{
    private static final long UPDATE_INTERVAL = TIME.SECOND;
    private static final long EVENT_START_TIME = 5 * TIME.SECOND;
    private static final int GEO_BOARD_CAST_RADIUS = 200;
    private RsuControlCore controlCore;
    private RealTimeReferencePoint timeReferencePoint;
    private GeoArea geoBoardCastArea;
    public TcrosRsuApplication(){
        super(RsuConfiguration.class,"TcrosRsuApplication");
    }
    @Override
    public void onStartup() {
        getLog().infoSimTime(this,"==================");
        getLog().infoSimTime(this,"RSU start initial.");
        initBasicAttribute();
        getLog().infoSimTime(this,"RSU has been initial.");
        getLog().infoSimTime(this,"RealTime:{}" , LocalDateTime.ofInstant(Instant.ofEpochMilli(getRealMilliTimeInSimOffset()), ZoneId.systemDefault()));
        getLog().infoSimTime(this,"Position lat:{},lon:{},alt:{}", getOs().getPosition().getLatitude(), getOs().getPosition().getLongitude(), getOs().getPosition().getAltitude());
        getLog().infoSimTime(this,"Id:{}", getOs().getId());
        getLog().infoSimTime(this,"Address:{}", getOs().getAdHocModule().getSourceAddress());
        getLog().infoSimTime(this,"Rsu Config:{}",controlCore.getRsuConfiguration().toString());
        getLog().infoSimTime(this,"==================");

        Event newEvent = new Event(getOs().getSimulationTime() + EVENT_START_TIME, this);
        getOs().getEventManager().addEvent(newEvent);
    }
    private void initBasicAttribute(){
        controlCore = new RsuControlCore(
                getOs().getPosition(),
                this.getConfiguration(),
                getLog().getUnitLogDirectory()
        );
        getOs().getAdHocModule().enable(new AdHocModuleConfiguration()
                .addRadio()
                .channel(AdHocChannel.CCH)
                .power(50)
                .distance(150)
                .create());
        timeReferencePoint = RealTimeReferencePoint.getInstance();
        geoBoardCastArea = new GeoCircle(getOs().getPosition(), GEO_BOARD_CAST_RADIUS);
    }
    @Override
    public void processEvent(Event event) {
        getLog().infoSimTime(this,"RSU has catch event.");
        eventTarget();
    }
    @Override
    public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
        controlCore.handleMessage(receivedV2xMessage.getMessage());
    }
    @Override
    public void onAcknowledgementReceived(ReceivedAcknowledgement receivedAcknowledgement) {
        /*No need to implement currently*/
    }
    @Override
    public void onCamBuilding(CamBuilder camBuilder) {
        /*No need to implement currently*/
    }
    @Override
    public void onMessageTransmitted(V2xMessageTransmission v2xMessageTransmission) {
        /*No need to implement currently*/
    }
    private void eventTarget(){
        Event newEvent = new Event(getOs().getSimulationTime() + UPDATE_INTERVAL, this);
        getOs().getEventManager().addEvent(newEvent);
        updateControlCore();
        updateMessageSend();
        updateLog();
    }
    private void updateControlCore() {
        controlCore.updateAllState(getOs().getSimulationTime());
    }
    private void updateLog(){
        getLog().infoSimTime(this,"============================");
        getLog().infoSimTime(this,"RSU has Updated.");
        getLog().infoSimTime(this,"Requested Queue:{}",controlCore.getRequestedVehicleIds());
        getLog().infoSimTime(this,"Wait Queue:{}",controlCore.getWaitVehicleIds());
        getLog().infoSimTime(this,"Granted Vehicle:{}",controlCore.getGrantedVehicleId());
        getLog().infoSimTime(this,"Passed Queue:{}", controlCore.getPassedVehicleIds());
        getLog().infoSimTime(this,"Rejected Queue:{}", controlCore.getRejectedVehicleIds());
        getLog().infoSimTime(this,"============================");
    }

    private void updateMessageSend(){
        broadCastSpatAndMapMessage();
        broadCastSsmToAllQueued();
    }

    private void broadCastSpatAndMapMessage(){
        final MessageRouting routing = createGeoBroadCastMessageRouting();
        getOs().getAdHocModule().sendV2xMessage(createMapData(routing));
        getOs().getAdHocModule().sendV2xMessage(creatSPaTData(routing));
    }

    private void broadCastSsmToAllQueued(){
        if(controlCore.needSendSsm()) {
            final MessageRouting routing = createGeoBroadCastMessageRouting();
            SignalStatusMessage ssm = controlCore.createSsm(getRealMilliTimeInSimOffset());
            controlCore.addSsmRecord(ssm);
            TcrosProtocolV2xMessage<SignalStatusMessage> ssmMessage = new TcrosProtocolV2xMessage<>(routing,ssm,SignalStatusMessage.class);
            ssmMessage.setSenderId(getOs().getId());
            getOs().getAdHocModule().sendV2xMessage(ssmMessage);
            getLog().infoSimTime(this, "SSM has sent.");
            RsuTrafficLightMessage tlMessage = controlCore.createTrafficLightMessage(routing, ssmMessage.getTcrosProtocol());
            getOs().getAdHocModule().sendV2xMessage(tlMessage);
            getLog().infoSimTime(this, "RsuTrafficLightMessage has sent.");
            getLog().infoSimTime(this, "Request Node:");

            // 記錄所有交通燈控制信息其對應的交通燈節點 ID 到 log
            for(TrafficLightControlInfo info : tlMessage.getTrafficLightControlInfoList()){
                getLog().info(info.getNodeId());
            }
        }
    }

    /**
     * 建立用於地理區域廣播的訊息路由配置。
     * 此方法設定了基於地理位置的廣播通訊機制，用於在特定區域內傳播車聯網訊息。
     *
     * @return MessageRouting 返回配置好的訊息路由物件，包含通道和地理區域設定
     */
    private MessageRouting createGeoBroadCastMessageRouting(){
        return  getOperatingSystem()
                .getAdHocModule()
                .createMessageRouting()
                .viaChannel(AdHocChannel.CCH)       // 設定使用控制通道（CCH, Control Channel）
                .geoBroadCast(geoBoardCastArea);    // 設定地理廣播區域
    }

    private TcrosProtocolV2xMessage<SPaTData> creatSPaTData(MessageRouting routing){
        SPaTData spatData = controlCore.creatSPaTData(getRealMilliTimeInSimOffset());
        controlCore.addSpatRecord(spatData);
        TcrosProtocolV2xMessage<SPaTData> spatDataMessage = new TcrosProtocolV2xMessage<>(routing,spatData,SPaTData.class);
        spatDataMessage.setSenderId(getOs().getId());
        return spatDataMessage;
    }

    private TcrosProtocolV2xMessage<V2XMapData> createMapData(MessageRouting routing){
        V2XMapData mapData = controlCore.createMapData();
        controlCore.addMapDataRecord(mapData);
        TcrosProtocolV2xMessage<V2XMapData> mapDataMessage = new TcrosProtocolV2xMessage<>(routing,mapData,V2XMapData.class);
        mapDataMessage.setSenderId(getOs().getId());
        return mapDataMessage;
    }

    /**
     * 取得模擬系統當前的時間戳（以毫秒表示）。
     * 此方法將模擬啟動時的系統時間（實際時間基準點）
     * 加上模擬器內部已累積的模擬時間（可調整速率），
     * 得出目前模擬時間軸對應的「虛擬系統時間」。
     *
     * @return 模擬當下對應的系統時間戳（毫秒）
     */
    private long getRealMilliTimeInSimOffset(){
        return timeReferencePoint.getRealTimeReferencePoint() + getOs().getSimulationTimeMs();
    }

    @Override
    public void onShutdown() {
        getLog().infoSimTime(this,"RSU has Shutdown!");
        exportSentMessage();
        exportTrafficLightInfoToJson();
    }

    public void exportSentMessage(){
        try {
            controlCore.exportSentMessage();
        } catch (IOException e) {
            getLog().infoSimTime(this,"Sent records output fail.");
        }
    }

    public void exportTrafficLightInfoToJson() {
        try {
            controlCore.exportTrafficLightInfoToJson();
        } catch (IOException e) {
            getLog().infoSimTime(this,"TrafficLightsInfo records output fail.");
        }
    }
}
