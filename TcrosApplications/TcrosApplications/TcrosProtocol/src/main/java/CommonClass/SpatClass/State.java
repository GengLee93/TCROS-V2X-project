package CommonClass.SpatClass;

import jdk.jfr.Description;

import java.io.Serializable;
import java.util.List;
public record State (
        @Description("Movement direction signal number")
        Integer signalGroup,
        @Description("Signal event message set, including all information of signal events.")
        List<StateTimeSpeed> stateTimeSpeed
)implements Serializable {}
