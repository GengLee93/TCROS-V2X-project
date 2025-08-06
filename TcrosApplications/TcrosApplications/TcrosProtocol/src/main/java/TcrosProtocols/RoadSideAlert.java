package TcrosProtocols;

import CommonClass.RsaClass.PositionInfo;
import CommonEnum.Extent;
import CommonEnum.ITISCode;
import CommonEnum.RsaPriority;
import com.fasterxml.jackson.annotation.JsonRootName;
import jdk.jfr.Description;

import java.io.Serializable;
import java.util.List;

@JsonRootName(value = "RoadSideAlert")
@Description("Road Side Alert")
public record RoadSideAlert(
        @Description("Message number, a unique identifier for this alert message")
        Integer msgCnt,

        @Description("Timestamp of the alert message, indicating when the alert was generated")
        Integer timeStamp,

        @Description("Event type, represented using ITISCode to describe specific traffic or warning events")
        ITISCode typeEvent,

        @Description("Detailed event description, containing up to 8 ITISCode entries for further clarification")
        List<ITISCode> description,

        @Description("Relative priority within RSA events, where a higher value indicates greater importance (7 is highest, 0 is lowest)")
        RsaPriority priority,

        @Description("Alert display direction, represented as a BIT STRING where each bit corresponds to 22.5 degrees, rotating clockwise from north")
        String heading,

        @Description("Alert coverage area, used in conjunction with position data and display direction to define the effective alert zone")
        Extent extent,

        @Description("Geographical position of the event, including coordinates and additional location-related details")
        PositionInfo position
) implements Serializable {
}