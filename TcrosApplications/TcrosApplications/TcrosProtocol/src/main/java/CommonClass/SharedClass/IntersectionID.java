package CommonClass.SharedClass;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jdk.jfr.Description;

import java.io.Serializable;

public record IntersectionID(
        @Min(0)
        @NotNull
        @Description("Default is one. Constraint:1. Large than zero. 2. Not null.")
        Long region,
        @Min(0)
        @NotNull
        @Description("Unique key. Defined by OSM node number. Constraint:1. Large than zero. 2. Not null.")
        Long id
)implements Serializable {}
