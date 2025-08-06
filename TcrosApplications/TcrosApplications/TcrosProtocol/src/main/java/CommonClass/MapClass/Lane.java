package CommonClass.MapClass;

import jdk.jfr.Description;

import java.io.Serializable;
import java.util.List;

public record Lane(
        @Description("Lane key in intersection.")
        Long laneID,
        @Description("No use.")
        LaneAttribute laneAttributes,
        @Description("No use.")
        String maneuvers,
        @Description("No use.")
        Nodes nodeList,
        @Description("No use.")
        List<ConnectingTo> connectsTo
)implements Serializable {}
