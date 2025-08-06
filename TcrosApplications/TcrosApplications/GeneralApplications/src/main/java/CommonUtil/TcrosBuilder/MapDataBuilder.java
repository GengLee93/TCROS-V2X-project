package CommonUtil.TcrosBuilder;

import CommonClass.MapClass.Intersection;
import CommonClass.MapClass.Lane;
import CommonClass.SharedClass.IntersectionID;
import CommonClass.SharedClass.Position;
import Configurations.TrafficLightInfo;
import Singleton.SharedLaneInfo;
import TcrosProtocols.V2XMapData;
import Util.PositionUtil;
import org.eclipse.mosaic.lib.geo.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class MapDataBuilder {
    private final List<Intersection> intersections;
    private int msgIssueRevision;
    public MapDataBuilder(){
        msgIssueRevision = 0;
        intersections = new ArrayList<>();
    }
    public V2XMapData create() {
        return new V2XMapData(msgIssueRevision,intersections);
    }
    public MapDataBuilder setMsgIssueRevision(int i){
        msgIssueRevision = i;
        return this;
    }

    public MapDataBuilder setIntersection(GeoPoint geoPoint, List< TrafficLightInfo > trafficLightInfoList){
        addMapIntersections(geoPoint,trafficLightInfoList);
        return this;
    }

    private void addMapIntersections(GeoPoint geoPoint, List<TrafficLightInfo> trafficLightInfoList){
        int revision = 0;
        for (TrafficLightInfo trafficLightInfo : trafficLightInfoList) {
            revision += 1;
            CommonClass.MapClass.Intersection intersection = new CommonClass.MapClass.Intersection(
                    new IntersectionID(1L, Long.parseLong(trafficLightInfo.nodeId)),
                    revision,
                    new Position(
                            PositionUtil.toOneOfTenMicroDegrees(geoPoint.getLatitude()),
                            PositionUtil.toOneOfTenMicroDegrees(geoPoint.getLongitude()),
                            0L
                    ),
                    new ArrayList<>()
            );

            List<String> nodeEdgeInfos = SharedLaneInfo.getInstance()
                    .getConnectionInfoConfiguration()
                    .getTcrosNodeEdgeInfo(trafficLightInfo.nodeId);
            if(nodeEdgeInfos != null) {
                for (long i = 0; i < nodeEdgeInfos.size(); i++) {
                    Lane lane = new Lane(i,null,null,null,null);
                    intersection.laneSet().add(lane);
                }
            }
            intersections.add(intersection);
        }
    }
}
