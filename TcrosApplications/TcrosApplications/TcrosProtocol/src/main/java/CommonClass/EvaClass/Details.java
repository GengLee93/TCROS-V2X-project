package CommonClass.EvaClass;

import CommonEnum.LightUse;
import CommonEnum.Multi;
import CommonEnum.ResponseType;
import CommonEnum.SirenUse;

import jdk.jfr.Description;
import java.io.Serializable;

public record Details(
        @Description("Sound alarm type")
        SirenUse sirenUse,

        @Description("Light alarm type")
        LightUse lightUse,

        @Description("Fleet quantity")
        Multi multi,

        @Description("Emergency event type")
        Events events,

        @Description("Dispatch type")
        ResponseType responseType
) implements Serializable {}
