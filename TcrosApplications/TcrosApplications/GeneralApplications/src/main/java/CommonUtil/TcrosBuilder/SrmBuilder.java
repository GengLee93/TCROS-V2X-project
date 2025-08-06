package CommonUtil.TcrosBuilder;

import CommonClass.SharedClass.BoundOn;
import CommonClass.SharedClass.Entity;
import CommonClass.SharedClass.IntersectionID;
import CommonClass.SharedClass.Position;
import CommonClass.SrmClass.*;
import CommonEnum.*;
import Singleton.SharedLaneInfo;
import TcrosProtocols.SignalRequestMessage;
import Util.PositionUtil;
import Util.TimeUtil;
import org.eclipse.mosaic.lib.geo.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class SrmBuilder{
    private int requestId ;
    private int sequenceNumber;
    private final long nowMs;
    private final List<Requests> requestSet;
    private final Requestor requestor;
    public SignalRequestMessage create(){
        return new SignalRequestMessage(
                TimeUtil.minuteOfYears(nowMs),
                TimeUtil.msInMinute(nowMs),
                sequenceNumber,
                requestSet,
                requestor
        );
    }
    public SrmBuilder(long now, int vehicleId, GeoPoint geoPoint){
        requestId = 0;
        nowMs = now;
        requestor = new Requestor(
                new Entity(vehicleId),
                new RequestorType(
                        RequestRole.basicVehicle,
                        HpmsType.car
                ),
                new RequestorPosition(
                        new Position(
                                PositionUtil.toOneOfTenMicroDegrees(geoPoint.getLatitude()),
                                PositionUtil.toOneOfTenMicroDegrees(geoPoint.getLongitude()),
                                0L
                        )
                ),
                TransitStatus.defaultVehicle,
                TransitOccupancy.occupancyUnknown,
                -122
        );
        requestSet = new ArrayList<>();
    }

    public SrmBuilder addUpcomingNodeRequest(String nodeId,double upcomingETC, String currentLane, String nextLane, RequestType nextType){
        long upcomingTime = nowMs + (long)upcomingETC;

        Requests requests = new Requests(
            new Request(
                new IntersectionID(1L, Long.parseLong(nodeId)),
                requestId,
                nextType,
                new BoundOn(
                    SharedLaneInfo.getInstance().getConnectionInfoConfiguration().getTcrosLaneId(
                        nodeId,
                        currentLane,
                        true
                    )
                ),
                new BoundOn(
                    SharedLaneInfo.getInstance().getConnectionInfoConfiguration().getTcrosLaneId(
                        nodeId,
                        nextLane,
                        false
                    )
                )
            ),
            TimeUtil.minuteOfYears(upcomingTime),
            TimeUtil.msInMinute(upcomingTime),
            0
        );
        requestSet.add(requests);
        requestId += 1;
        return this;
    }

    public SrmBuilder addPreviousNodeRequest(Requests previousRequest){
        requestSet.add(
            new Requests(
                new Request(
                    previousRequest.request().id(),
                    requestId,
                    RequestType.priorityCancellation,
                    previousRequest.request().inBoundLane(),
                    previousRequest.request().outBoundLane()
                ),
                previousRequest.minute(),
                previousRequest.second(),
                previousRequest.duration()
            )
        );
        requestId += 1;
        return this;
    }

    public SrmBuilder setSequenceNumber(int sn){
        sequenceNumber = sn;
        return this;
    }

}
