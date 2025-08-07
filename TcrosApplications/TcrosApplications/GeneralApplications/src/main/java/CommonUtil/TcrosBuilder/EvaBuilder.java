package CommonUtil.TcrosBuilder;

import CommonClass.EvaClass.Details;
import CommonClass.EvaClass.Events;
import CommonClass.EvaClass.rsaMsg;
import CommonClass.RsaClass.*;
import CommonEnum.*;
import TcrosProtocols.EmergencyVehicleAlert;
import TcrosProtocols.RoadSideAlert;
import TcrosProtocols.RoadSideAlert;
import Util.TimeUtil;
import jakarta.validation.constraints.Null;
import org.eclipse.mosaic.lib.geo.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class EvaBuilder {
    private int timeStamp;
    private String id;
    private Integer mass;
    private ResponseType responseType;
    private TcrosProtocols.RoadSideAlert rsaMsgs;
    private Details details;
    private BasicType basicTypes;


    private Integer msgCnt;
    private long nowMs;
    private ITISCode typeEvent;
    private List<ITISCode> description;
    private RsaPriority priority;
    private String headingBitString;
    private Extent extent;
    private PositionInfo position;

    private Integer headingDegrees;
    private SpeedInfo speedInfo;
    private PosAccuracy posAccuracy;
    private TimeConfidence timeConfidence;
    private PosConfidence posConfidence;
    private SpeedConfidence speedConfidence;

    public RsaBuilder rsaBuilder;
    private RoadSideAlert rsa;



    public EmergencyVehicleAlert create(){
        rsaMsgs = rsaBuilder.create();
        return new EmergencyVehicleAlert(
                timeStamp,
                id,
                rsaMsgs,
                responseType,
                details,
                mass,
                basicTypes
        );
    }
    public EvaBuilder(long nowMs) {
        this.timeStamp = (int)nowMs;
        this.mass = 255;
        this.rsaBuilder = new RsaBuilder(nowMs);
        this.responseType = ResponseType.notInUseOrNotEquipped;
        this.details = new Details(
                SirenUse.unavailable,
                LightUse.unavailable,
                Multi.unavailable,
                new Events(Event.peUnavailable),
                ResponseType.notInUseOrNotEquipped);
        this.basicTypes = BasicType.none;
    }
    public EvaBuilder setId(String id){
        this.id = id;
        return this;
    }

    public EvaBuilder setMass(int mass) {
        this.mass = mass;
        return this;
    }

    public EvaBuilder setResponseType(ResponseType responseType) {
        this.responseType = responseType;
        return this;
    }


    public EvaBuilder setDetails(Details details) {
        this.details = details;
        return this;
    }

    public EvaBuilder setBasicType(BasicType basicType) {
        this.basicTypes = basicType;
        return this;
    }
}
