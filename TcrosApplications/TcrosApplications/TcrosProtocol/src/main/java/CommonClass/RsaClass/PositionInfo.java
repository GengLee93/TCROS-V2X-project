package CommonClass.RsaClass;

import CommonEnum.TimeConfidence;
import jdk.jfr.Description;
import java.io.Serializable;

// Event position information (used in EVA and RSA packets)
public record PositionInfo(
        @Description("UTC timestamp in seconds.")
        UtcTime utcTime,

        @Description("Longitude, measured in 10 micro degrees (1/10,000,000 degrees), 1800000001 means no information.")
        Long lon,

        @Description("Latitude, measured in 10 micro degrees (1/10,000,000 degrees), 900000001 means no information.")
        Long lat,

        @Description("Elevation in decimeters (0.1 meters), -4096.61439 means no information.")
        Integer elevation,

        @Description("Heading angle in 0.0125-degree units, -360/65535 means no information.")
        Integer heading,

        @Description("Speed, measured in 0.02 meters per second (m/s), -8191 means no information.")
        SpeedInfo speed,

        @Description("Position accuracy, measured in meters.")
        PosAccuracy posAccuracy,

        @Description("Time confidence, measured in seconds.")
        TimeConfidence timeConfidence,

        @Description("Position confidence, measured in meters.")
        PosConfidence posConfidence,

        @Description("Speed confidence, measured in meters per second (m/s).")
        SpeedConfidence speedConfidence
) implements Serializable { }