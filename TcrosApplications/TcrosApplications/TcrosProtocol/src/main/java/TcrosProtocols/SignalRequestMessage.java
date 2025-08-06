package TcrosProtocols;

import CommonClass.SrmClass.Requestor;
import CommonClass.SrmClass.Requests;
import CommonEnum.*;
import com.fasterxml.jackson.annotation.JsonRootName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jdk.jfr.Description;

import java.io.Serializable;
import java.util.List;

@JsonRootName(value = "SignalRequestMessage")
@Description("Signal Request Message")
public record SignalRequestMessage (
        @Min(0)
        @Description("Total accumulated minutes for the year")
        Integer timeStamp,
        @Min(0)
        @Description("Total accumulated micro-second in this minute.")
        Integer second,
        @Min(0)
        @Description("Message serial number.")
        Integer sequenceNumber,
        @NotEmpty
        @Valid
        @Description("Priority Request Message Set")
        List<Requests> requests,
        @NotNull
        @Valid
        @Description("Requestor description.")
        Requestor requestor
)implements Serializable, ITcrosProtocol {
    public Requests getRequest(String nodeId){
        try {
            for (Requests requests : requests){
                if (requests.request().id().id() == Long.parseLong(nodeId)){
                    return requests;
                }
            }
            return null;
        }catch (Exception e){
            return null;
        }
    }
    public RequestType getRequestType(String nodeId){
        Requests requests = getRequest(nodeId);
        return requests == null ? null : requests.request().requestType();
    }
}

