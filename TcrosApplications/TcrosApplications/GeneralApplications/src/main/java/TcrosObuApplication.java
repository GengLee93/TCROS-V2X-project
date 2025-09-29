import Configurations.ObuConfiguration;
import Configurations.VehicleConfiguration;
import CoreModule.ObuControlCore;
import Singleton.RealTimeReferencePoint;
import Tcros2MosaicProtocol.TcrosProtocolV2xMessage;
import TcrosProtocols.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.ConfigurableApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.VehicleApplication;
import org.eclipse.mosaic.fed.application.app.api.os.VehicleOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.interactions.vehicle.VehicleLaneChange;
import org.eclipse.mosaic.interactions.vehicle.VehicleSightDistanceConfiguration;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.enums.LaneChangeMode;
import org.eclipse.mosaic.lib.enums.VehicleClass;
import org.eclipse.mosaic.lib.enums.VehicleStopMode;
import org.eclipse.mosaic.lib.geo.GeoCircle;
import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.geo.UtmPoint;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleData;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.OptionalLong;

public class TcrosObuApplication extends ConfigurableApplication<ObuConfiguration, VehicleOperatingSystem>
        implements VehicleApplication, CommunicationApplication {
    private static final int GEO_BOARD_CAST_RADIUS = 200;
    private static final Log log = LogFactory.getLog(TcrosObuApplication.class);
    private ObuControlCore obuControlCore;
    private RealTimeReferencePoint timeReferencePoint;
    public TcrosObuApplication(){
        super(ObuConfiguration.class,"TcrosObuApplication");
    }
    @Override
    public void onStartup() {
        getLog().infoSimTime(this,"Vehicle has start!");

        obuControlCore = new ObuControlCore(getOs().getId(),getConfiguration(), getLog().getUnitLogDirectory());
        timeReferencePoint = RealTimeReferencePoint.getInstance();
        getOs().getAdHocModule().enable(new AdHocModuleConfiguration()
                .addRadio()
                .channel(AdHocChannel.CCH)
                .power(50)
                .distance(150)
                .create());
        getLog().infoSimTime(this,"Vehicle ID:{}",getOs().getId());

        VehicleSightDistanceConfiguration sightConfig = new VehicleSightDistanceConfiguration(
                getOs().getSimulationTime(),
                getOs().getId(),
                20,
                180);
    }
    @Override
    public void processEvent(Event event){
        /*No need to implement currently*/
    }
    @Override
    public void onVehicleUpdated(@Nullable VehicleData vehicleData, @NotNull VehicleData vehicleData1) {
        obuControlCore.updateVehicleData(vehicleData1,getOs().getSimulationTime());
        updateMessageSend();
        updateLog(vehicleData1);
    }

    private void updateMessageSend(){
        // TODO: 判斷是否為救護車邏輯重寫
        if (getOs().getVehicleParameters().getInitialVehicleType().getVehicleClass()
                == VehicleClass.EmergencyVehicle ) {
            if (obuControlCore.needSendSrm()) { sendSrm(); }
            sendEva();
        }
    }
    private void updateLog(VehicleData newVehicleData){
        getLog().infoSimTime(this,"==================");
        getLog().infoSimTime(this,"Vehicle has been update");
        getLog().infoSimTime(this,"Driving {} m/s.", newVehicleData.getSpeed());
        getLog().infoSimTime(this,"Position lat:{},lon:{},alt:{}.", obuControlCore.getCurrentPoint().getLatitude(), obuControlCore.getCurrentPoint().getLongitude(), obuControlCore.getCurrentPoint().getAltitude());
        getLog().infoSimTime(this,"ConnectionId:{}" , newVehicleData.getRoadPosition().getConnectionId());
        getLog().infoSimTime(this,"Average Speed:{} m/s" ,obuControlCore.getAverageSpeed());
        getLog().infoSimTime(this,"Upcoming Node:{},ETC: {}ms",obuControlCore.getUpcomingNode() == null ? "null" : obuControlCore.getUpcomingNode().getId(), obuControlCore.getUpcomingNodeETCms());
        getLog().infoSimTime(this,"SPaT Count:{},Map Count:{}",
                obuControlCore.getSpatTimer().isEmpty() ? "null" : obuControlCore.getSpatTimer().getTimer(),
                obuControlCore.getMapTimer().isEmpty() ? "null" : obuControlCore.getMapTimer().getTimer()
        );
        getLog().infoSimTime(this,"Vehicle name:{}", newVehicleData.getName());
        getLog().infoSimTime(this,"Previous Node:{}",obuControlCore.getPreviousNode() == null ? "null" : obuControlCore.getPreviousNode() .getId());
        getLog().infoSimTime(this,"RouteId:{}" , newVehicleData.getRouteId());
        getLog().infoSimTime(this, "Total driving time: {} s", obuControlCore.getTotalDrivingDurationSeconds().orElse(0L));
        getLog().infoSimTime(this,"==================");
    }

    private void sendSrm(){
        final MessageRouting routing = getOperatingSystem()
                .getAdHocModule()
                .createMessageRouting()
                .geoBroadCast(new GeoCircle(getOs().getPosition(), GEO_BOARD_CAST_RADIUS));
        SignalRequestMessage srm = obuControlCore.createSRM(getRealMilliTimeInSimOffset());
        obuControlCore.addSrmRecord(srm);
        TcrosProtocolV2xMessage<SignalRequestMessage> sendMessage =  new TcrosProtocolV2xMessage<>(routing,srm,SignalRequestMessage.class);
        sendMessage.setSenderId(getOs().getId());
        getOs().getAdHocModule().sendV2xMessage(sendMessage);
        getLog().infoSimTime(this, "Send SRM,Request junction.{}",obuControlCore.getUpcomingNode().getId());
    }

    private void sendEva(){
        final MessageRouting routing = getOperatingSystem()
                .getAdHocModule()
                .createMessageRouting()
                .geoBroadCast(new GeoCircle(getOs().getPosition(), GEO_BOARD_CAST_RADIUS));
        EmergencyVehicleAlert eva = obuControlCore.createEva(getRealMilliTimeInSimOffset());
        obuControlCore.addEvaRecord(eva);
        TcrosProtocolV2xMessage<EmergencyVehicleAlert> sendMessage =  new TcrosProtocolV2xMessage<>(routing,eva,EmergencyVehicleAlert.class);
        sendMessage.setSenderId(getOs().getId());
        getOs().getAdHocModule().sendV2xMessage(sendMessage);
        getLog().infoSimTime(this, "Send EVA,info junction.{}",
                obuControlCore.getUpcomingNode() != null ?
                        obuControlCore.getUpcomingNode().getId() : "null");
    }

    private long getRealMilliTimeInSimOffset() {
        return timeReferencePoint.getRealTimeReferencePoint() + getOs().getSimulationTimeMs();
    }

    @Override
    public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
        if (receivedV2xMessage.getMessage() instanceof TcrosProtocolV2xMessage<?> message) {
            obuControlCore.handleMessage(message);
            writeReceivedMessageLog(message);

            // 分緊急車輛必須執行對應變道邏輯
            if (getOs().getVehicleParameters().getInitialVehicleType().getVehicleClass()
                    != VehicleClass.EmergencyVehicle) {
                if (message.getTcrosProtocol() instanceof RoadSideAlert rsa ||
                    message.getTcrosProtocol() instanceof EmergencyVehicleAlert eva) {
                    executeYieldAction(obuControlCore.getLastYieldAction());
                }
            }
        }
    }

    private void executeYieldAction(ObuControlCore.YieldAction action) {
        switch (action) {
            case CHANGE_LANE_LEFT -> {
                getOs().changeLane(VehicleLaneChange.VehicleLaneChangeMode.TO_LEFT, 1_000_000_000L);
                log.info("Left lane change successful");
            }
            case CHANGE_LANE_RIGHT -> {
                getOs().changeLane(VehicleLaneChange.VehicleLaneChangeMode.TO_RIGHT, 5_000_000_000L);
                log.info("Right lane change successful");
            }
            case STOP -> {
                getOs().stopNow(VehicleStopMode.PARK_ON_ROADSIDE, 5_000_000_000L);
                log.info("Acceleration failed, pulling over to stop");
            }
            case NONE -> log.info("No Yield Action");
        }
    }

    private void writeReceivedMessageLog(TcrosProtocolV2xMessage<?> message){
        getLog().infoSimTime(this,
                "Message received, sender:{},type:{}"
                ,message.getSenderId()
                ,message.getProtocolClassName());
    }
    @Override
    public void onShutdown() {
        getLog().infoSimTime(this,"Vehicle has Shutdown!");
        Path logPath = getLog().getUnitLogDirectory();
        if(logPath == null) {
            getLog().infoSimTime(this,"Sent records output fail");
            return;
        }
        try {
            obuControlCore.exportSentMessage();
        } catch (IOException e) {
            getLog().infoSimTime(this,"Sent records output fail");
        }

        try {
            obuControlCore.exportDrivingRecords();
        }catch (IOException e){
            getLog().infoSimTime(this,"Driving records output fail");
        }
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
}