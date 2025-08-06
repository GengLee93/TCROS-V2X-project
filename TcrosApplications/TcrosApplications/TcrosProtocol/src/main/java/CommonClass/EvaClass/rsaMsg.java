package CommonClass.EvaClass;

import CommonClass.RsaClass.PositionInfo;
import CommonEnum.Extent;
import CommonEnum.ITISCode;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import jdk.jfr.Description;

public record rsaMsg (
        @Min(0)
        @NotNull
        @Description("Message sequence number")
        Integer msgCnt,

        @Min(0)
        @NotNull
        @Description("Timestamp for message synchronization")
        Integer timeStamp,

        @Min(0)
        @NotNull
        @Description("Event type represented by an ITIS code")
        Integer typeEvent,

        @Description("Detailed event description using up to 8 ITIS codes")
        ITISCode description,

        @Description("Priority level among RSA messages (0 = lowest, 7 = highest)")
        String priority,

        @NotNull
        @Description("Alert direction as a 16-bit string, each bit represents 22.5° clockwise from North")
        String heading,

        @NotNull
        @Description("Extent of the alert area, used with direction and position")
        Extent extent,

        @Description("Position information of the event")
        PositionInfo position
) implements Serializable {}