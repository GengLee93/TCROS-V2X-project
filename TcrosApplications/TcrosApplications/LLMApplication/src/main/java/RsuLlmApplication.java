import Configurations.LLMApplicationConfiguration;
import Configurations.LLMConfiguration;
import Configurations.RsuConfiguration;
import CoreModule.RsuControlCore;
import Singleton.RealTimeReferencePoint;
import Tcros2MosaicProtocol.TcrosProtocolV2xMessage;
import Tcros2MosaicProtocol.TrafficLightMessage.RsuTrafficLightMessage;
import Tcros2MosaicProtocol.TrafficLightMessage.TrafficLightControlInfo;
import TcrosProtocols.SPaTData;
import TcrosProtocols.SignalStatusMessage;
import TcrosProtocols.V2XMapData;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.ConfigurableApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.os.RoadSideUnitOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.geo.GeoArea;
import org.eclipse.mosaic.lib.geo.GeoCircle;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;

import java.io.*;
import java.nio.file.Path;

public class RsuLlmApplication extends ConfigurableApplication<LLMApplicationConfiguration,RoadSideUnitOperatingSystem>
    implements CommunicationApplication {
    private static final long EVENT_START_TIME = 5 * TIME.SECOND;
    private static final long UPDATE_INTERVAL =  TIME.SECOND;
    private static final int GEO_BOARD_CAST_RADIUS = 200;
    private RsuControlCore controlCore;
    private LlmServiceCore<SignalStatusMessage> llmServiceCore;
    private RealTimeReferencePoint timeReferencePoint;
    private GeoArea geoBoardCastArea;
    public RsuLlmApplication() {
        super(LLMApplicationConfiguration.class,"RsuLlmApplication");
    }

    @Override
    public void onStartup() {
        getLog().infoSimTime(this, "LLM Rsu application start initial");

        LLMApplicationConfiguration applicationConfiguration = getConfiguration();
        initLlmService(applicationConfiguration.llmConfiguration);
        initBasicAttribute(applicationConfiguration.rsuConfiguration);

        getOs().getAdHocModule().enable(new AdHocModuleConfiguration()
                .addRadio()
                .channel(AdHocChannel.CCH)
                .power(50)
                .distance(150)
                .create());

        Event newEvent = new Event(getOs().getSimulationTime() + EVENT_START_TIME, this);
        getOs().getEventManager().addEvent(newEvent);

        getLog().infoSimTime(this, "LLM Rsu application has been initial");
    }

    private void initBasicAttribute(RsuConfiguration rsuConfiguration){

        controlCore = new RsuControlCore(
                getOs().getPosition(),
                rsuConfiguration,
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

    private void initLlmService(LLMConfiguration llmConfiguration){
        llmServiceCore = new LlmServiceCore<>(llmConfiguration);
        llmServiceCore.setLogPath(getLog().getUnitLogDirectory());
        llmServiceCore.loadFolderFileToRag();
    }
    @Override
    public void processEvent(Event event) {
        getLog().infoSimTime(this,"LLM RSU has catch event.");
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

    private void updateMessageSend() {
        broadCastSpatAndMapMessage();
        broadCastSsmToAllQueued();
    }

    private void broadCastSpatAndMapMessage(){
        final MessageRouting routing = createGeoBroadCastMessageRouting();
        getOs().getAdHocModule().sendV2xMessage(createMapData(routing));
        getOs().getAdHocModule().sendV2xMessage(creatSPaTData(routing));
    }

    private void broadCastSsmToAllQueued() {
        if(controlCore.needSendSsm()) {
            final MessageRouting routing = createGeoBroadCastMessageRouting();
            SignalStatusMessage ssm = controlCore.createSsm(getRealMilliTimeInSimOffset());
            controlCore.addSsmRecord(ssm);
            if(llmServiceCore.containTarget(ssm)){
                System.out.println("Communication to LLM...");
                try {
                    ssm = llmServiceCore.repeatModifyByLlm(ssm, SignalStatusMessage.class,ssm.sequenceNumber());
                    System.out.println("Communication Done!");
                }catch (JsonProcessingException jpe){
                    getLog().error("Modify ssm has an error.");
                    jpe.printStackTrace();
                    System.out.println("Communication Error!");
                }
            }

            TcrosProtocolV2xMessage<SignalStatusMessage> ssmMessage = new TcrosProtocolV2xMessage<>(routing,ssm,SignalStatusMessage.class);
            getOs().getAdHocModule().sendV2xMessage(ssmMessage);
            getLog().infoSimTime(this, "SSM has sent.");

            RsuTrafficLightMessage tlMessage = controlCore.createTrafficLightMessage(routing, ssmMessage.getTcrosProtocol());
            getOs().getAdHocModule().sendV2xMessage(tlMessage);
            getLog().infoSimTime(this, "RsuTrafficLightMessage has sent.");
            getLog().infoSimTime(this, "Request Node:");
            for(TrafficLightControlInfo info : tlMessage.getTrafficLightControlInfoList()){
                getLog().info(info.getNodeId());
            }
        }
    }

    private MessageRouting createGeoBroadCastMessageRouting(){
        return  getOperatingSystem()
                .getAdHocModule()
                .createMessageRouting()
                .viaChannel(AdHocChannel.CCH)
                .geoBroadCast(geoBoardCastArea);
    }

    private TcrosProtocolV2xMessage<SPaTData> creatSPaTData(MessageRouting routing){
        SPaTData spatData = controlCore.creatSPaTData(getRealMilliTimeInSimOffset());
        controlCore.addSpatRecord(spatData);
        return new TcrosProtocolV2xMessage<>(routing,spatData,SPaTData.class);
    }

    private TcrosProtocolV2xMessage<V2XMapData> createMapData(MessageRouting routing){
        V2XMapData mapData = controlCore.createMapData();
        controlCore.addMapDataRecord(mapData);
        return new TcrosProtocolV2xMessage<>(routing,mapData,V2XMapData.class);
    }

    private long getRealMilliTimeInSimOffset(){
        return timeReferencePoint.getRealTimeReferencePoint() + getOs().getSimulationTimeMs();
    }

    @Override
    public void onShutdown() {
        getLog().infoSimTime(this,"RSU has Shutdown!");
        Path logPath = getLog().getUnitLogDirectory();
        if(logPath != null) {
            exportSentMessage();
            exportTrafficLightInfoToJson();
            exportLlmConfiguration();
            exportLlmModifyRecord();
        }else {
            getLog().infoSimTime(this,"Could not get log path.");
        }
    }

    private void exportSentMessage(){
        try {
            controlCore.exportSentMessage();
        } catch (IOException e) {
            getLog().infoSimTime(this,"Sent records output fail.");
        }
    }

    private void exportTrafficLightInfoToJson() {
        try {
            controlCore.exportTrafficLightInfoToJson();
        } catch (IOException e) {
            getLog().infoSimTime(this,"TrafficLightsInfo records output fail.");
        }
    }

    private void exportLlmModifyRecord(){
        try {
           llmServiceCore.exportLlmModifyRecord();
           llmServiceCore.exportSuccessRecord();
           llmServiceCore.exportFailRecord();
        }catch (IOException e){
            getLog().infoSimTime(this,"LlmModify records output fail.");
        }
    }

    private void exportLlmConfiguration(){
        try {
            llmServiceCore.exportLlmConfiguration();
        }catch (IOException e){
            getLog().infoSimTime(this,"Llm configuration output fail.");
        }
    }
}
