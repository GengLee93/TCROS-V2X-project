package CommonUtil.TcrosBuilder;

import CommonClass.SharedClass.IntersectionID;
import CommonClass.SpatClass.Intersection;
import CommonClass.SpatClass.State;
import CommonClass.SpatClass.StateTimeSpeed;
import CommonClass.SpatClass.Timing;
import CommonEnum.EventState;
import CommonEnum.SignalStatus;
import Configurations.TrafficLightInfo;
import Tcros2MosaicProtocol.TrafficLightMessage.TrafficLightsStateInfo;
import TcrosProtocols.SPaTData;
import Util.TimeUtil;
import org.eclipse.mosaic.lib.objects.trafficlight.TrafficLight;
import org.eclipse.mosaic.lib.objects.trafficlight.TrafficLightState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SpatBuilder {
    List<Intersection> intersections;
    public SpatBuilder(){
        intersections = new ArrayList<>();
    }
    public SPaTData create(){
        return new SPaTData(intersections);
    }
    public SpatBuilder setIntersections(long nowTime, List<TrafficLightInfo> trafficLightInfoList, Map<String, TrafficLightsStateInfo> trafficLightsStateInfoMap){
        int moy = TimeUtil.minuteOfYears(nowTime);
        int timeStamp =  TimeUtil.msInMinute(nowTime);
        int revision = 0;
        for (TrafficLightInfo trafficLightInfo : trafficLightInfoList){
            revision += 1;
            CommonClass.SpatClass.Intersection intersection = new CommonClass.SpatClass.Intersection(
                    new IntersectionID(1L,Long.parseLong(trafficLightInfo.nodeId)),
                    revision,
                    SignalStatus.fixedTimeOperation,
                    moy,
                    timeStamp,
                    createSpatTrafficLightStates(nowTime,trafficLightsStateInfoMap.get(trafficLightInfo.nodeId))
            );
            intersections.add(intersection);
        }
        return this;
    }
    private List<State> createSpatTrafficLightStates(long nowTime, TrafficLightsStateInfo trafficLightsStateInfo){
        List<State> states = new ArrayList<>();
        if(trafficLightsStateInfo != null && trafficLightsStateInfo.getTrafficLightGroupMap() != null) {
            Map<Integer,List<TrafficLight>> tlGroupMap = trafficLightsStateInfo.getTrafficLightGroupMap();
            for(Map.Entry<Integer,List<TrafficLight>> tlEntry : tlGroupMap.entrySet()){
                State state = new State(
                        tlEntry.getKey(),
                        new ArrayList<>()
                );
                for(TrafficLight tl : tlEntry.getValue()){
                    StateTimeSpeed stateTimeSpeed = new StateTimeSpeed(
                        eventStateMapping(tl.getCurrentState()),
                        new Timing(
                            TimeUtil.hundredMsInHour(
                                    nowTime + trafficLightsStateInfo.getStateRemainedTime() - trafficLightsStateInfo.getStateConfiguredTime()
                            ),
                            TimeUtil.hundredMsInHour(
                            nowTime + trafficLightsStateInfo.getStateRemainedTime()
                            )
                        )
                    );
                    state.stateTimeSpeed().add(stateTimeSpeed);
                }
                states.add(state);
            }
        }
        return states;
    }

    private EventState eventStateMapping(TrafficLightState trafficLightState){
        if(trafficLightState.isGreen()){
            return EventState.protectedMovementAllowed;
        }else if(trafficLightState.isRed()){
            return EventState.stopAndRemain;
        }else if(trafficLightState.isYellow()){
            return EventState.protectedClearance;
        }else if(trafficLightState.isOff()){
            return EventState.dark;
        }else if(trafficLightState.isRedYellow()){
            return EventState.cautionConflictingTraffic;
        }else{
            return EventState.unavailable;
        }
    }
}
