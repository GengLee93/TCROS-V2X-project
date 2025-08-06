package CommonEnum.LaneType;

import java.io.Serializable;

public record LaneType (
        Vehicle vehicle,
        Crosswalk crosswalk,
        Sidewalk sidewalk,
        BikeLane bikeLane,
        TrackedVehicle trackedVehicle
)implements Serializable {}
