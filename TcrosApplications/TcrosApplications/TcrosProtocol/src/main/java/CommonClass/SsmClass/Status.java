package CommonClass.SsmClass;

import CommonClass.SharedClass.IntersectionID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jdk.jfr.Description;

import java.io.Serializable;
import java.util.List;

@Description("A signal status message that can describe the signal status of up to 32 intersections.")
public record Status(
        @Min(0)
        @NotNull
        @Description("Message serial number.Constraint:Large than zero, Not null.")
        Integer sequenceNumber,
        @NotNull
        @Valid
        @Description("Unique intersection node number.Associated with other messages through this message set. Constraint: Not null.")
        IntersectionID id,
        @NotNull
        @Description("Signal status message set, reply request to current node. Constraint: Not null.")
        @Valid
        List<SigStatus> sigStatus
) implements Serializable {}
