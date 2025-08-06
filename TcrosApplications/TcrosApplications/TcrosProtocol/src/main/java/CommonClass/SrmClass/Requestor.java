package CommonClass.SrmClass;

import CommonClass.SharedClass.Entity;
import CommonEnum.TransitOccupancy;
import CommonEnum.TransitStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jdk.jfr.Description;

import java.io.Serializable;

public record Requestor (
        @Description("Unique key of request vehicle.")
        @NotNull
        @Valid
        Entity id,
        @Description("Type of request vehicle.")
        @NotNull
        RequestorType type,
        @Description("Position of request vehicle")
        @NotNull
        RequestorPosition position,
        @Description("Default is 11111111")
        @NotNull
        TransitStatus transitStatus,
        @Description("Default is 0")
        @NotNull
        TransitOccupancy transitOccupancy,
        @Description("Default is -122")
        @NotNull
        Integer transitSchedule
)implements Serializable {}
