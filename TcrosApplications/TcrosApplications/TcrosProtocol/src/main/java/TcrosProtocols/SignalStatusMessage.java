package TcrosProtocols;

import CommonClass.SsmClass.SigStatus;
import CommonClass.SsmClass.Status;
import CommonEnum.RequestStatus;
import com.fasterxml.jackson.annotation.JsonRootName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jdk.jfr.Description;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@JsonRootName(value = "SignalStatusMessage")
@Description("Signal Status Message. Contain time information and reply status of request.")
public record SignalStatusMessage(
        @Min(0)
        @NotNull
        @Description("Total accumulated minutes for the year. Constraint: Large than zero, Not null.")
        Integer timeStamp,
        @Min(0)
        @NotNull
        @Description("Total accumulated micro-second in this minute. Constraint:Large than zero, Not null.")
        Integer second,
        @Min(0)
        @NotNull
        @Description("Message serial number. Constraint: Large than zero, Not null.")
        Integer sequenceNumber,
        @NotNull
        @Valid
        @Description("Signal Status Message Set. Constraint: Not null.")
        List<Status> status
)implements Serializable, ITcrosProtocol {
    public RequestStatus getRequestStatus(String nodeId,Integer requesterId){
        try {
            Status status = getRequestNodeStatus(nodeId);
            if(status==null)
                return null;
            for (SigStatus sigStatus : status.sigStatus()){
                if (Objects.equals(sigStatus.requester().id().entityID(), requesterId)){
                    return sigStatus.status();
                }
            }
            return null;
        }catch (Exception e){
            return null;
        }
    }

    public Status getRequestNodeStatus(String nodeId){
        if(status()==null)
            return null;
        for (Status status : status()){
            if(nodeId.equals(String.valueOf(status.id().id()))){
                return status;
            }
        }
        return null;
    }

    public SigStatus getEntityRequestSigStatus(Integer entityId){
        return Optional.ofNullable(status())
                .stream()
                .flatMap(List::stream)
                .flatMap(s -> Optional.ofNullable(s.sigStatus()).stream().flatMap(List::stream))
                .filter(sigStatus -> entityId.equals(sigStatus.requester().id().entityID()))
                .findFirst()
                .orElse(null);
    }

}

