package TcrosProtocols;

import CommonClass.SpatClass.Intersection;
import com.fasterxml.jackson.annotation.JsonRootName;
import jdk.jfr.Description;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@JsonRootName(value = "SPaTData")
@Description("SPaT Data")
public record SPaTData(
        @Description("Dynamic intersection message set controlled by the RSU. Contains real-time information about intersection signs.")
        List<Intersection> intersections
) implements Serializable , ITcrosProtocol {
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
