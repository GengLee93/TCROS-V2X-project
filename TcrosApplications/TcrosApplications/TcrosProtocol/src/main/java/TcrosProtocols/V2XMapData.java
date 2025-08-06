package TcrosProtocols;

import CommonClass.MapClass.Intersection;
import com.fasterxml.jackson.annotation.JsonRootName;
import jdk.jfr.Description;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@JsonRootName(value = "MapData")
@Description("Map Data")
public record V2XMapData(
        @Description("Message serial number, confirming the message update status.")
        Integer msgIssueRevision,
        @Description("Static intersection message set controlled by the RSU")
        List<Intersection> intersections
) implements Serializable, ITcrosProtocol {
    public boolean containNode(String nodeId){
        Long nodeLongId = Long.parseLong(nodeId);
        for(Intersection intersection : intersections){
            if(Objects.equals(intersection.id().id(), nodeLongId)){
                return true;
            }
        }
        return false;
    }
}

