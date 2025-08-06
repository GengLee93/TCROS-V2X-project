package CommonClass.SrmClass;

import CommonClass.SharedClass.Position;
import jdk.jfr.Description;

import java.io.Serializable;

public record RequestorPosition(
        @Description("Position of request vehicle")
        Position position
)implements Serializable {}
