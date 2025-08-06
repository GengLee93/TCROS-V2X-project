package CommonClass.SharedClass;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jdk.jfr.Description;

import java.io.Serializable;

public record BoundOn (
        @Min(0)
        @NotNull
        @Description("Lane Id in node")
        Integer lane
) implements Serializable {}
