package CommonClass.RsaClass;

import CommonEnum.TransmissionState;
import jdk.jfr.Description;
import java.io.Serializable;

// 速率及檔位訊息集
public record SpeedInfo(
        @Description("Transmission state, enumerated integer (0–7), representing different gear positions.")
        TransmissionState transmission,

        @Description("Speed value, measured in units of 0.02 m/s, ranging from 0 to 8191.")
        Integer speed
) implements Serializable { }
