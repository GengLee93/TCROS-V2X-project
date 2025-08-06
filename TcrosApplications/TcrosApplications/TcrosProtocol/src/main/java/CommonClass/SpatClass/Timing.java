package CommonClass.SpatClass;

import jdk.jfr.Description;

import java.io.Serializable;

public record Timing (
        @Description("Signal start time points.")
        Integer startTime,
        @Description("Signal end time points.")
        Integer minEndTime
)implements Serializable {}