import Singleton.SharedLaneInfo;
import Tcros2MosaicProtocol.TrafficLightMessage.RsuTrafficLightMessage;
import Tcros2MosaicProtocol.TrafficLightMessage.TrafficLightControlInfo;
import Tcros2MosaicProtocol.TrafficLightMessage.TrafficLightStatusMessage;
import Tcros2MosaicProtocol.TrafficLightMessage.TrafficLightsStateInfo;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.TrafficLightApplication;
import org.eclipse.mosaic.fed.application.app.api.os.TrafficLightOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.geo.GeoCircle;
import org.eclipse.mosaic.lib.objects.trafficlight.TrafficLight;
import org.eclipse.mosaic.lib.objects.trafficlight.TrafficLightGroupInfo;
import org.eclipse.mosaic.lib.objects.trafficlight.TrafficLightProgramPhase;
import org.eclipse.mosaic.lib.objects.trafficlight.TrafficLightState;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TcrosTrafficLightApplication extends AbstractApplication<TrafficLightOperatingSystem>
        implements CommunicationApplication ,TrafficLightApplication{
    private static final String DEFAULT_PROGRAM = "0";
    private static final String LINE = "====================";
    private static final long UPDATE_INTERVAL = TIME.SECOND;
    private static final long CONTROL_LIMIT_TIME_MS = 10000;
    private static final int BOARD_CAST_RADIUS = 20;
    private GeoCircle boardCastArea;
    private Map<Integer,List<TrafficLight>> trafficLightsSubGroup;
    private long leaveControlTime;
    private long enterControlTime;
    private long totalPhaseTime;

    @Override
    public void onStartup() {
        getOs().getAdHocModule().enable(new AdHocModuleConfiguration()
                .addRadio()
                .channel(AdHocChannel.CCH)
                .power(50)
                .distance(150)
                .create());
        resetTrafficLightControlTimer();
        totalPhaseTime = 0;
        for(TrafficLightProgramPhase phase : getOs ().getCurrentProgram().getPhases()){
            totalPhaseTime += phase.getConfiguredDuration();
        }
        totalPhaseTime *= 1_000;

        boardCastArea = new GeoCircle(getOs().getPosition(), BOARD_CAST_RADIUS);
        trafficLightsSubGroup = new HashMap<>();
        getOs().switchToProgram(DEFAULT_PROGRAM);
        getLog().infoSimTime(this,LINE);
        getLog().infoSimTime(this,"Traffic Light has been initial.");
        getLog().infoSimTime(this,"Position lat:{},lon:{},alt:{}", getOs().getPosition().getLatitude(), getOs().getPosition().getLongitude(), getOs().getPosition().getAltitude());
        getLog().infoSimTime(this,"tlGroupId:{}", getOs().getTrafficLightGroup().getGroupId());
        getLog().infoSimTime(this,"Address:{}", getOs().getAdHocModule().getSourceAddress());
        getLog().infoSimTime(this,"CurrentProgram:{}", getOs().getCurrentProgram().getProgramId());
        getLog().infoSimTime(this,"ControlledLanes:{}", getOs().getControlledLanes());
        getLog().infoSimTime(this,"totalPhaseTime:{}", totalPhaseTime);
        getLog().infoSimTime(this,"Traffic Lights:");
        for (TrafficLight tl : getOs().getTrafficLightGroup().getTrafficLights()){
            getLog().info(tl.toString());
        }
        getLog().infoSimTime(this,LINE);

        setTrafficLightsSubGroup(getOs().getTrafficLightGroup().getTrafficLights());
        eventTarget();
    }
    private void resetTrafficLightControlTimer(){
        leaveControlTime = -1;
        enterControlTime = -1;
    }

    private void setTrafficLightsSubGroup(List<TrafficLight> trafficLights){
        List<TrafficLightState> stateList = new ArrayList<>();
        for(TrafficLight trafficLight : trafficLights){
            TrafficLightState currentState = trafficLight.getCurrentState();
            if(currentState != null && !stateList.contains(currentState)){
                stateList.add(currentState);
            }
        }

        trafficLightsSubGroup.clear();
        for (TrafficLight trafficLight : trafficLights) {
            TrafficLightState currentState = trafficLight.getCurrentState();
            int idx = stateList.indexOf(currentState);
            if (!trafficLightsSubGroup.containsKey(idx)) {
                List<TrafficLight> tlList = new ArrayList<>();
                tlList.add(trafficLight);
                trafficLightsSubGroup.put(idx, tlList);
            } else {
                trafficLightsSubGroup.get(idx).add(trafficLight);
            }
        }
    }

    @Override
    public void onShutdown() {
        /*No need to implement currently*/
    }
    @Override
    public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
        if (receivedV2xMessage.getMessage() instanceof RsuTrafficLightMessage message){
            controlSignal(message);
        }
    }

    @Override
    public void processEvent(Event event){
        eventTarget();
    }

    private void eventTarget(){
        Event newEvent = new Event(getOs().getSimulationTime() + UPDATE_INTERVAL, this);
        getOs().getEventManager().addEvent(newEvent);
        updateState();
        sendTrafficLightStatusMessage();
        updateLog();
    }

    private void updateState() {
        setTrafficLightsSubGroup(getOs().getTrafficLightGroup().getTrafficLights());
    }

    private void updateLog(){
        getLog().infoSimTime(this,LINE);
        getLog().infoSimTime(this,"CurrentProgram:{}", getOs().getCurrentProgram().getProgramId());
        getLog().infoSimTime(this,"CurrentPhase:{}", getOs().getCurrentProgram().getCurrentPhase().getIndex());
        getLog().infoSimTime(this,"PhaseConfiguredTime:{}", getOs().getCurrentProgram().getCurrentPhase().getConfiguredDuration());
        getLog().infoSimTime(this,"PhaseRemainTime:{}", getOs().getCurrentProgram().getCurrentPhase().getRemainingDuration());
        getLog().infoSimTime(this,"Traffic Lights:");
        for (TrafficLight tl : getOs().getTrafficLightGroup().getTrafficLights()){
            getLog().info(tl.toString());
        }
        getLog().infoSimTime(this,"Traffic Lights Group:{}",trafficLightsSubGroup);
        getLog().infoSimTime(this,"enterControlTime:{}", enterControlTime);
        getLog().infoSimTime(this,"leaveControlTime:{}", leaveControlTime);
        getLog().infoSimTime(this,LINE);
    }

    private void switchToNormalPhase(){
        long time = (leaveControlTime - enterControlTime) % totalPhaseTime;
        resetTrafficLightControlTimer();
        int phaseIndex = -1;
        List<TrafficLightProgramPhase> phases = getOs().getCurrentProgram().getPhases();
        for(int i = 0 ; i < phases.size() ; i++){
            long tempTime = time - phases.get(i).getConfiguredDuration();
            if(tempTime < 0){
                phaseIndex = i;
                break;
            }else{
                time = tempTime;
            }
        }

        if (phaseIndex != -1) {
            getOs().switchToPhaseIndex(phaseIndex);
            getOs().setRemainingDurationOfCurrentPhase(time);
            getLog().infoSimTime(this,"Switch to normal phase, phase : {},time : {}",phaseIndex,time);
        }
    }

    @Override
    public void onTrafficLightGroupUpdated(TrafficLightGroupInfo trafficLightGroupInfo, TrafficLightGroupInfo trafficLightGroupInfo1) {
        if(trafficLightGroupInfo.getCurrentPhaseIndex() != trafficLightGroupInfo1.getCurrentPhaseIndex() &&
                enterControlTime > 0){
            leaveControlTime = getOs().getSimulationTime();
//            switchToNormalPhase();
        }
    }

    private void controlSignal(RsuTrafficLightMessage message){
        int connectionIdx = getValidConnectionIdx(message);
        int nextPhaseIdx = getConnectionPassPhaseIndex(connectionIdx);
        if (nextPhaseIdx != -1) {
            if(enterControlTime < 0){
                enterControlTime = getOs().getSimulationTime();
            }
            getOs().switchToPhaseIndex(nextPhaseIdx);
//                getOs().setRemainingDurationOfCurrentPhase(CONTROL_LIMIT_TIME_MS);
            getLog().infoSimTime(this, "Switch to phase:{}", nextPhaseIdx);
        }
    }

    private int getValidConnectionIdx(RsuTrafficLightMessage message){
        String currentNodeId = getOs().getTrafficLightGroup().getGroupId();
        List<TrafficLightControlInfo> trafficLightControlInfoList = message.getTrafficLightControlInfoList();
        if(trafficLightControlInfoList.isEmpty())
            return -1;

        TrafficLightControlInfo currentTrafficLightControlInfo = null;
        for (TrafficLightControlInfo info : trafficLightControlInfoList) {
            if (info.getNodeId().equals(currentNodeId)) {
                currentTrafficLightControlInfo = info;
                break;
            }
        }
        if (    currentTrafficLightControlInfo == null ||
                currentTrafficLightControlInfo.getOutLane() < 0 ||
                currentTrafficLightControlInfo.getInLane() < 0)
            return -1;

        List<String> nodeEdgeInfo = SharedLaneInfo.getInstance()
                                                  .getConnectionInfoConfiguration()
                                                  .getTcrosNodeEdgeInfo(currentNodeId);
        String inEdge;
        String outEdge;
        try {
            inEdge = nodeEdgeInfo.get(currentTrafficLightControlInfo.getInLane());
            outEdge = nodeEdgeInfo.get(currentTrafficLightControlInfo.getOutLane());
        }catch (Exception e){
            return -1;
        }
        return SharedLaneInfo.getInstance()
                .getConnectionInfoConfiguration()
                .getInOutConnectionIndex(currentNodeId,inEdge,outEdge);
    }

    private int getConnectionPassPhaseIndex(int connectionIdx) {
        if (connectionIdx == -1)
            return -1;
        List<TrafficLightProgramPhase> tlPhases = getOs ().getCurrentProgram().getPhases();
        for( int i = 0 ; i < tlPhases.size() ; i++){
            TrafficLightProgramPhase phase = tlPhases.get(i);
            if( phase.getStates().get(connectionIdx).equals(TrafficLightState.GREEN))
                return i;
        }
        return -1;
    }

    private void sendTrafficLightStatusMessage(){
        final MessageRouting routing = getOperatingSystem()
                                        .getAdHocModule()
                                        .createMessageRouting()
                                        .geoBroadCast(boardCastArea);
        TrafficLightStatusMessage message = new TrafficLightStatusMessage(
                routing,
                new TrafficLightsStateInfo(
                    getOs().getTrafficLightGroup().getGroupId(),
                    getOs().getPosition(),
                    trafficLightsSubGroup,
                    getOs().getCurrentPhase().getConfiguredDuration() / 1_000,
                    getOs().getCurrentPhase().getRemainingDuration() / 1_000
                )
        );
        getOs().getAdHocModule().sendV2xMessage(message);
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
