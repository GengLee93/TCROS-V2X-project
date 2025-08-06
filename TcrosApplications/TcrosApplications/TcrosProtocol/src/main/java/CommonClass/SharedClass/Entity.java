package CommonClass.SharedClass;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jdk.jfr.Description;

import java.io.Serializable;

@Description("Entity information, contain a unique id for this entity.")
public record Entity (
        @Min(0)
        @NotNull
        @Description("Id for this entity. Constraint:Equal or large than zero.")
        Integer entityID
)implements Serializable {}
