package Configurations;

import java.util.List;

public class RsuConfiguration {
    public String label;
    public Integer priorityStartTime;
    public Integer priorityEndTime;
    public List<TrafficLightInfo> trafficLightInfoList;
    @Override
    public String toString(){
        return String.format("RsuConfiguration[label:%s,\n trafficLightInfo:%s]",label,trafficLightInfoList);
    }
}
