package CommonClass.RsaClass;

import CommonEnum.ElevationLevel;
import CommonEnum.PosLevel;
import jdk.jfr.Description;

import java.io.Serializable;

// PosConfidence class
public record PosConfidence(
        @Description("Horizontal position confidence, 0 means unknown.")
        PosLevel pos,

        @Description("Elevation confidence, 0 means unknown.")
        ElevationLevel elevation
) implements Serializable { }
