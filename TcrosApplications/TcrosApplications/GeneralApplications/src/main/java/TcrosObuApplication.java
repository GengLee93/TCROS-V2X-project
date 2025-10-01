import Configurations.ObuConfiguration;
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
import org.eclipse.mosaic.lib.enums.VehicleClass;
import org.eclipse.mosaic.lib.geo.GeoCircle;
import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.geo.UtmPoint;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleData;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.eclipse.mosaic.rti.TIME;

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
    //避讓
    private Integer lastLaneIdx = null;      // 上一個 tick 的 laneIndex（做 diff 用）
    private Integer lastChangeTarget = null; // 這次要求的目標 laneIndex
    private Long    lastChangeWhen = null;   // 這次換道預計執行的模擬時間（ns）
    private String  lastChangeConnId = null; // 下指令當下所屬的 connection，驗證時需一致
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
                .distance(GEO_BOARD_CAST_RADIUS)
                .create());
        getLog().infoSimTime(this,"Vehicle ID:{}",getOs().getId());
    }
    @Override
    public void processEvent(Event event){
        /*No need to implement currently*/
    }

    @Override
    public void onVehicleUpdated(@Nullable VehicleData vehicleData, @NotNull VehicleData vehicleData1) {
        obuControlCore.updateVehicleData(vehicleData1, getOs().getSimulationTime());
        updateMessageSend();

        // 換道
        if (lastChangeTarget == null && obuControlCore.hasPendingLaneChange()) {
            int delta = obuControlCore.consumeLaneChangeDelta();
            tryLaneChange(delta);
        }

        // 讀取當前 lane跟connection
        var rp = getOs().getVehicleData().getRoadPosition();
        int curLane   = rp.getLaneIndex();
        String curConn = rp.getConnectionId();

        if (lastLaneIdx != null && curLane != lastLaneIdx) {
            getLog().infoSimTime(this, "Avoidance[DIFF ]: prev={} -> now={} (conn={})", lastLaneIdx, curLane, curConn);
        }
        lastLaneIdx = curLane;

        if (lastChangeTarget != null && lastChangeWhen != null) {
            long now     = getOs().getSimulationTime();
            long elapsed = now - lastChangeWhen;

            if (now < lastChangeWhen) {
                getLog().infoSimTime(this, "Avoidance: pending (waiting for scheduled when)");
            } else {
                String elapStr = String.format(java.util.Locale.ROOT, "%.3f", elapsed / (double) TIME.SECOND);
                getLog().infoSimTime(this,
                        "Avoidance[VERIFY]: cur={}, target={}, conn={}, storedConn={}, elapsed={}s",
                        curLane, lastChangeTarget, curConn, lastChangeConnId, elapStr);

                // connection 改變
                if (lastChangeConnId != null && !lastChangeConnId.equals(curConn)) {
                    getLog().infoSimTime(this,
                            "Avoidance: ABORT verification (connection changed {} -> {}).",
                            lastChangeConnId, curConn);
                    lastChangeTarget = null;
                    lastChangeWhen   = null;
                    lastChangeConnId = null;

                    // SUCCESS
                } else if (curLane == lastChangeTarget) {
                    getLog().infoSimTime(this, "Avoidance: SUCCESS, lane==target ({})", curLane);
                    lastChangeTarget = null;
                    lastChangeWhen   = null;
                    lastChangeConnId = null;

                    // TIMEOUT
                } else if (elapsed >= (long)(5.0 * TIME.SECOND)) {
                    getLog().infoSimTime(this,
                            "Avoidance: TIMEOUT, still not at target (cur={}, expected={}, conn={})",
                            curLane, lastChangeTarget, curConn);
                    lastChangeTarget = null;
                    lastChangeWhen   = null;
                    lastChangeConnId = null;

                    // 等待中
                } else {
                    getLog().infoSimTime(this,
                            "Avoidance: waiting (cur={}, expected={}, elapsed={}s, conn={})",
                            curLane, lastChangeTarget, elapStr, curConn);
                }
            }
        }
        updateLog(vehicleData1);
    }

    //避讓
    private void tryLaneChange(int delta) {
        if (lastChangeTarget != null){
            getLog().infoSimTime(this, "Avoidance: skip try (verification in progress, target={})", lastChangeTarget);
            return;
        }
        var rp = getOs().getVehicleData().getRoadPosition();
        int lanes = rp.getConnection().getLanes();
        int cur   = rp.getLaneIndex();

        int chosenDelta = delta;
        int target = cur + chosenDelta;

        if (target < 0) target = 0;
        if (target >= lanes) target = lanes - 1;

        // 若與當前相同換反向試
        if (target == cur) {
            int altDelta = (delta > 0) ? -1 : +1;
            int altTarget = cur + altDelta;
            if (altTarget < 0) altTarget = 0;
            if (altTarget >= lanes) altTarget = lanes - 1;

            if (altTarget != cur) {
                getLog().infoSimTime(this,
                        "Avoidance: primary dir saturated (cur={}), try opposite dir -> altTarget={}",
                        cur, altTarget);
                chosenDelta = altDelta;
                target = altTarget;
            } else {
                getLog().infoSimTime(this,
                        "Avoidance: no lane available to change (cur={}, lanes={}), skip.",
                        cur, lanes);
                lastChangeTarget = null;
                lastChangeWhen   = null;
                lastChangeConnId = null;
                return;
            }
        }

        long now = getOs().getSimulationTime();
        long when = now + (long)(0.1 * TIME.SECOND); // 推遲 0.1s，避免同 tick 排序競賽
        String whenStr = String.format(java.util.Locale.ROOT, "%.3f", when / (double) TIME.SECOND);
        getLog().infoSimTime(this,
                "Avoidance: tryLaneChange enter, conn={}, cur={}, delta={}, target={}, lanes={}, when={}s",
                rp.getConnectionId(), cur, chosenDelta, target, lanes, whenStr);

        getOs().changeLane(target, when);

        getLog().infoSimTime(this, "Avoidance: changeLane(targetIndex={}) sent.", target);

        lastChangeTarget = target;
        lastChangeWhen   = when;
        lastChangeConnId = rp.getConnectionId();
        getLog().infoSimTime(this, "Avoidance: target={} on conn={} stored for verification.",
                target, lastChangeConnId);
    }

    private void updateMessageSend(){
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
