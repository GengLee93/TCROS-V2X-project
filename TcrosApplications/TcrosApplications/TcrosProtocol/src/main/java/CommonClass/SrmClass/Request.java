package CommonClass.SrmClass;

import CommonClass.SharedClass.BoundOn;
import CommonClass.SharedClass.IntersectionID;
import CommonEnum.RequestType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jdk.jfr.Description;

import java.io.Serializable;

public record Request (
        @Description("Unique intersection node number.Associated with other messages through this message set.")
        @NotNull
        @Valid
        IntersectionID id,
        @Description("Request number")
        @Min(0)
        Integer requestID,
        @Description("Status of the current priority request.")
        @NotNull
        RequestType requestType,
        @Description("Request enter lane.")
        @NotNull
        @Valid
        BoundOn inBoundLane,
        @Description("Request out lane.")
        @NotNull
        @Valid
        BoundOn outBoundLane
)implements Serializable {}
