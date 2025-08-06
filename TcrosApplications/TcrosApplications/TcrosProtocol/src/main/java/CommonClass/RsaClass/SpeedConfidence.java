package CommonClass.RsaClass;

import CommonEnum.HeadingConfidence;
import CommonEnum.SpeedLevel;
import CommonEnum.ThrottleConfidence;
import jdk.jfr.Description;

import java.io.Serializable;

/// Speed Confidence Level Information Set.
public record SpeedConfidence(
        @Description("Heading direction confidence level, 0 means unknown or unavailable.")
        HeadingConfidence heading,

        @Description("Speed confidence level, 0 means unknown or unavailable.")
        SpeedLevel speed,

        @Description("Throttle confidence level, 0 means unknown or unavailable.")
        ThrottleConfidence throttle
) implements Serializable {}
