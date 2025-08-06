package CommonClass.SpatClass;

import CommonEnum.EventState;
import jdk.jfr.Description;

import java.io.Serializable;

public record StateTimeSpeed(
        @Description("Signal light status")
        EventState eventState,
        @Description("Signal time points.")
        Timing timing
)implements Serializable {}
