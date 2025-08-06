package CommonClass.MapClass;

import CommonClass.SharedClass.IntersectionID;
import CommonClass.SharedClass.Position;
import jdk.jfr.Description;

import java.io.Serializable;
import java.util.List;

public record Intersection(
        @Description("Unique intersection node number.Associated with other messages through this message set.")
        IntersectionID id,
        @Description("Message serial number, confirm the latest message.")
        Integer revision,
        @Description("Intersection reference latitude and longitude.")
        Position refPoint,
        @Description("Lane set, including all lane information connected to this node.")
        List<Lane> laneSet
)implements Serializable {}
