package CommonClass.MapClass;

import CommonClass.SharedClass.BoundOn;
import CommonClass.SharedClass.IntersectionID;

import java.io.Serializable;

public record ConnectingTo(
        BoundOn connectingLane,
        IntersectionID remoteIntersection,
        Integer signalGroup
)implements Serializable {}
