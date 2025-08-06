package CommonClass.SsmClass;

import CommonClass.SharedClass.BoundOn;
import CommonEnum.RequestStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jdk.jfr.Description;

import java.io.Serializable;

@Description("A signal status message that can report the signal status for requesters at a specific intersection.")
public record SigStatus(
        @NotNull
        @Valid
        @Description("Requester description.Constraint:Not null.")
        Requester requester,
        @NotNull
        @Valid
        @Description("Request enter lane.Constraint:Not null.")
        BoundOn inboundOn,
        @NotNull
        @Valid
        @Description("Request out lane.Constraint:Not null.")
        BoundOn outboundOn,
        @Min(0)
        @NotNull
        @Description("Total accumulated minutes for the year.Constraint:Large than zero.")
        Integer minute,
        @Min(0)
        @NotNull
        @Description("Total accumulated micro-second in this minute.Constraint:Large than zero.")
        Integer second,
        @Min(0)
        @NotNull
        @Description("Default is 0.Constraint:Equal or large than zero.")
        Integer duration,
        @NotNull
        @Description("Reply status of this request. Constraint: Not null.")
        RequestStatus status
)implements Serializable {}