package CommonClass.EvaClass;

import CommonEnum.Event;
import jdk.jfr.Description;

import java.io.Serializable;

public record Events(
        @Description("""
            Emergency event state.
            Bit 0: Unavailable
            Bit 1: Emergency response active
            Bit 2: Emergency lights active
            Bit 3: Emergency sound active
            Bit 4: Non-emergency lights active
            Bit 5: Non-emergency sound active
            """)
        Event event
) implements Serializable {}
