package TcrosProtocols;

import CommonClass.RsaClass.PositionInfo;
import CommonEnum.Extent;
import CommonEnum.ITISCode;
import CommonEnum.RsaPriority;
import com.fasterxml.jackson.annotation.JsonRootName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import jdk.jfr.Description;

import java.io.Serializable;
import java.util.List;

@JsonRootName(value = "RoadSideAlert")
@Description("Road Side Alert")
public record RoadSideAlert(
        @Description("Message number, a unique identifier for this alert message")
        @Min(0)
        @Max(127)
        @Valid
        Integer msgCnt,

        @Description("Timestamp of the alert message, indicating when the alert was generated")
        @Min(0)
        @Max(527040)
        @Valid
        Integer timeStamp,

        @Description("Event type, represented using ITISCode to describe specific traffic or warning events")
        @Min(0)
        @Max(65535)
        @Valid
        ITISCode typeEvent,

        @Description("Detailed event description, containing up to 8 ITISCode entries for further clarification")
        @Size(max = 8)
        @Valid
        List<ITISCode> description,

        @Description("Relative priority within RSA events, where a higher value indicates greater importance (7 is highest, 0 is lowest)")
        @Valid
        RsaPriority priority,

        @Description("Alert display direction, represented as a BIT STRING where each bit corresponds to 22.5 degrees, rotating clockwise from north")
        String heading,

        @Description("Alert coverage area, used in conjunction with position data and display direction to define the effective alert zone")
        Extent extent,

        @Description("Geographical position of the event, including coordinates and additional location-related details")
        PositionInfo position
) implements Serializable {
}