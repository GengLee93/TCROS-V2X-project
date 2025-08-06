package CommonClass.RsaClass;

import jdk.jfr.Description;
import java.io.Serializable;

// 橢圓精確度
public record PosAccuracy(
        @Description("Semi-major axis length, measured in 0.05 meters (m), ranging from 0 to 255.")
        Integer semiMajor,

        @Description("Semi-minor axis length, measured in 0.05 meters (m), ranging from 0 to 255.")
        Integer semiMinor,

        @Description("Orientation angle of the semi-major axis, measured in degrees (°), ranging from 0 to 65535, where 360/65535 represents 1 degree.")
        Integer orientation
) implements Serializable { }