package CommonClass.SsmClass;

import CommonClass.SharedClass.Entity;
import CommonEnum.RequestRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jdk.jfr.Description;

import java.io.Serializable;

@Description("Priority requester information, including details such as `entityID` and `request`.")
public record Requester(
        @NotNull
        @Valid
        @Description("Requester Entity Id. Contain an positive integer id. Constraint:Not null.")
        Entity id,
        @Min(0)
        @NotNull
        @Description("Request id. Constraint:Large than zero.")
        Integer request,
        @Min(0)
        @NotNull
        @Description("Request sequence number.Constraint:Large than zero.")
        Integer sequenceNumber,
        @NotNull
        @Description("Requester role, default is 0. Constraint:Not null.")
        RequestRole role
)implements Serializable {}
