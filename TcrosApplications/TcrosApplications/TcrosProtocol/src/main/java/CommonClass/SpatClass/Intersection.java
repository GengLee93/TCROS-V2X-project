package CommonClass.SpatClass;

import CommonClass.SharedClass.IntersectionID;
import CommonEnum.SignalStatus;
import jdk.jfr.Description;

import java.io.Serializable;
import java.util.List;
public record Intersection(
        @Description("Unique intersection node number.Associated with other messages through this message set.")
        IntersectionID id,
        @Description("Message serial number, confirm the latest message.")
        Integer revision,
        @Description("Signal operation status enum.")
        SignalStatus status,
        @Description("Total accumulated minutes for the year")
        Integer moy,
        @Description("Total accumulated micro-second in this minute.")
        Integer timeStamp,
        @Description("Including all information of turn signal status.")
        List<State> states
)implements Serializable {}
