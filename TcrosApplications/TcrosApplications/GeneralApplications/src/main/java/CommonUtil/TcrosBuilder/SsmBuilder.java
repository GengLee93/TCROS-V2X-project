package CommonUtil.TcrosBuilder;

import CommonClass.SharedClass.IntersectionID;
import CommonClass.SrmClass.Requests;
import CommonClass.SsmClass.Requester;
import CommonClass.SsmClass.SigStatus;
import CommonClass.SsmClass.Status;
import CommonEnum.RequestRole;
import CommonEnum.RequestStatus;
import Configurations.TrafficLightInfo;
import TcrosProtocols.SignalRequestMessage;
import TcrosProtocols.SignalStatusMessage;
import Util.TimeUtil;

import java.util.ArrayList;
import java.util.List;

public class SsmBuilder {
    private int requestId ;
    private int sequenceNumber;
    private long nowMs;
    private List<Status> statuses;
    public SignalStatusMessage create(){
        cleanUnusedStatus();
        return new SignalStatusMessage(
            TimeUtil.minuteOfYears(nowMs),
            TimeUtil.msInMinute(nowMs),
            sequenceNumber,
            statuses
        );
    }
    public SsmBuilder(long nowTime,List<TrafficLightInfo> trafficLightInfoList) {
        statuses = new ArrayList<>();
        requestId = 0;
        nowMs = nowTime;
        int sequenceNum = 0;
        for (TrafficLightInfo trafficLightInfo : trafficLightInfoList){
            Status status = new Status(
                    sequenceNum,
                    new IntersectionID(1L,Long.parseLong(trafficLightInfo.nodeId)),
                    new ArrayList<>()
            );
            sequenceNum += 1;
            statuses.add(status);
        }
    }

    public SsmBuilder setGrantedVehicle(SignalRequestMessage grantedMessage) {
        if(grantedMessage != null)
            addRequestStatus(grantedMessage, RequestStatus.granted);
        return this;
    }

    public SsmBuilder setWaitVehicles(List<SignalRequestMessage> waitQueue) {
        for (SignalRequestMessage message : waitQueue) {
            addRequestStatus(message,RequestStatus.processing);
        }
        return this;
    }

    public SsmBuilder setSequenceNumber(int sn){
        sequenceNumber = sn;
        return this;
    }

    public SsmBuilder setRequestedVehicle(List<SignalRequestMessage> requestedQueue) {
        for (SignalRequestMessage message :requestedQueue) {
            addRequestStatus(message, RequestStatus.requested);
        }
        return this;
    }

    public SsmBuilder setRejectVehicle(List<SignalRequestMessage> rejectQueue) {
        for (SignalRequestMessage message :rejectQueue) {
            addRequestStatus(message, RequestStatus.rejected);
        }
        return this;
    }

    private void addRequestStatus(SignalRequestMessage receivedSrm, RequestStatus status){
        if(receivedSrm == null || receivedSrm.requests() == null)
            return;
        for (Requests requests : receivedSrm.requests() ){
            Status status1 =  getRequestNodeStatus(String.valueOf(requests.getRequestIntersectionId()));
            if(status1 != null){
                SigStatus sigStatus = new SigStatus(
                        new Requester(
                                receivedSrm.requestor().id(),
                                requestId,
                                receivedSrm.sequenceNumber(),
                                RequestRole.basicVehicle
                        ),
                        requests.request().inBoundLane(),
                        requests.request().outBoundLane(),
                        receivedSrm.timeStamp(),
                        receivedSrm.second(),
                        0,
                        status
                );
                status1.sigStatus().add(sigStatus);
                requestId += 1;
            }
        }
    }

    private void cleanUnusedStatus(){
        statuses.removeIf(status -> status != null && status.sigStatus().isEmpty());
    }

    private Status getRequestNodeStatus(String nodeId){
        for (Status status : statuses){
            if(nodeId.equals(String.valueOf(status.id().id()))){
                return status;
            }
        }
        return null;
    }
}
