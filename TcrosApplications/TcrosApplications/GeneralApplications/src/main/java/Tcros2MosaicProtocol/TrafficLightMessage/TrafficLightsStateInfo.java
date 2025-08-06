package Tcros2MosaicProtocol.TrafficLightMessage;

import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.trafficlight.TrafficLight;

import java.util.List;
import java.util.Map;

public class TrafficLightsStateInfo {
    private final String nodeId;
    private final GeoPoint point;
    private final Map<Integer,List<TrafficLight>> trafficLightGroupMap;
    private final long stateConfiguredTime;
    private final long stateRemainedTime;

    public TrafficLightsStateInfo(String nId,GeoPoint p,Map<Integer,List<TrafficLight>> tlGroupMap,long cTime,long rTime){
        nodeId = nId;
        point = p;
        trafficLightGroupMap = tlGroupMap;
        stateConfiguredTime = cTime;
        stateRemainedTime = rTime;
    }

    public Map<Integer, List<TrafficLight>> getTrafficLightGroupMap() {
        return trafficLightGroupMap;
    }

    public long getStateConfiguredTime() {
        return stateConfiguredTime;
    }

    public long getStateRemainedTime() {
        return stateRemainedTime;
    }

    public String getNodeId() {
        return nodeId;
    }
    public GeoPoint getGeoPoint() {
        return point;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("nodeId: ").append(nodeId).append(", ");
        sb.append("geoPoint: ").append(point).append(", ");
        sb.append("stateConfiguredTime: ").append(stateConfiguredTime).append(", ");
        sb.append("stateRemainedTime: ").append(stateRemainedTime).append(", ");
        sb.append("trafficLightGroupMap: {");

        trafficLightGroupMap.forEach((key, value) -> {
            sb.append("\n  Group ").append(key).append(": [");
            for (int i = 0; i < value.size(); i++) {
                sb.append(value.get(i).toString());
                if (i < value.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append("]");
        });

        sb.append("\n}");
        return sb.toString();
    }

}
