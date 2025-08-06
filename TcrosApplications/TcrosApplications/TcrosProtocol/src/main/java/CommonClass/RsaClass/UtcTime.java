package CommonClass.RsaClass;

import jdk.jfr.Description;
import java.io.Serializable;

public record UtcTime(
        @Description("Year of the event; 0 means information is unknown.")
        Integer year,

        @Description("Month of the event; 0 means information is unknown.")
        Integer month,

        @Description("Day of the event; 0 means information is unknown.")
        Integer day,

        @Description("Hour of the event; 31 means information is unknown.")
        Integer hour,

        @Description("Minute of the event; 60 means information is unknown.")
        Integer minute,

        @Description("second of the event; 65535 means information is unknown.")
        Integer second
) implements Serializable { }
