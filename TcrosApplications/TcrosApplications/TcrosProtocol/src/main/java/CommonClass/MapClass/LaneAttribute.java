package CommonClass.MapClass;

import CommonEnum.LaneType.LaneType;

import java.io.Serializable;

public record LaneAttribute (
        String directionalUse,
        String sharedWith,
        LaneType laneType
)implements Serializable {}
