package CommonUtil.TcrosBuilder;

import CommonClass.RsaClass.*;
import CommonEnum.*;
import TcrosProtocols.RoadSideAlert;
import Util.PositionUtil;
import Util.TimeUtil;
import org.eclipse.mosaic.lib.geo.GeoPoint;

import java.util.ArrayList;
import java.util.List;

/*
* timeStamp 對應封包廣播時間點(會更新)
* UtcTime 對應事件發生的時間，建構時就固定了
 */
public class RsaBuilder {
    private Integer msgCnt;
    private final long eventStartTimeMs;
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

    public RsaBuilder(long eventStartTimeMs) {
        this.eventStartTimeMs = eventStartTimeMs;
        description = new ArrayList<>();
        this.position = new PositionInfo(
                new UtcTime(0, 0, 0, 31, 60, 65535),
                1800000001L,
                900000001L,
                -4096,
                2880,
                new SpeedInfo(TransmissionState.UNAVAILABLE, 8191),
                new PosAccuracy(255, 255, 65525),
                TimeConfidence.Unavailable,
                new PosConfidence(PosLevel.UNAVAILABLE, ElevationLevel.UNAVAILABLE),
                new SpeedConfidence(HeadingConfidence.UNAVAILABLE, SpeedLevel.UNAVAILABLE, ThrottleConfidence.UNAVAILABLE)
        );
    }

    public RoadSideAlert create() {
        return new RoadSideAlert(
                msgCnt,
                TimeUtil.minuteOfYears(eventStartTimeMs),
                typeEvent,
                description,
                priority,
                headingBitString,
                extent,
                position
                );
    }

    public RsaBuilder setMsgCnt(int cnt) {
        this.msgCnt = cnt;
        return this;
    }

    public RsaBuilder setTypeEvent(ITISCode typeEvent) {
        this.typeEvent = typeEvent;
        return this;
    }

    public RsaBuilder addDescription(ITISCode code) {
        if (code != null
                && !code.equals(typeEvent)          // 不該跟主要事件重複
                && !this.description.contains(code) // 不重複加入相同事件
                && description.size() < 8)          // 最多8則 ITIS code 事件描述
        {
            this.description.add(code);
        }
        return this;
    }

    public RsaBuilder setPriority(RsaPriority priority) {
        this.priority = priority;
        return this;
    }

    public RsaBuilder setHeadingBitString(String bitString){
        if(bitString != null && bitString.length() == 16){
            this.headingBitString = bitString;
        }
        return this;
    }

    public RsaBuilder setExtent(Extent extent) {
        this.extent = extent;
        return this;
    }

    public RsaBuilder SetPosition(GeoPoint geoPoint){
        int elevation = (int) Math.round(geoPoint.getAltitude() * 10);
        elevation = Math.max(-4096, Math.min(elevation, 61439));

        position =  new PositionInfo(
                TimeUtil.toUtcTime(eventStartTimeMs),
                PositionUtil.toOneOfTenMicroDegrees(geoPoint.getLongitude()),
                PositionUtil.toOneOfTenMicroDegrees(geoPoint.getLatitude()),
                elevation,
                headingDegrees,                  // 行駛方向（default = 0）
                speedInfo,
                posAccuracy, // 橢圓精度（default = 0）
                timeConfidence,
                posConfidence,
                speedConfidence
        );
        return this;
    }

    public RsaBuilder setHeadingByDegree(double degree) {
        int sector = (int) (degree / 22.5) % 16;
        StringBuilder sb = new StringBuilder("0000000000000000");
        sb.setCharAt(sector, '1');
        this.headingDegrees = (int) (degree / 0.0125); // ⇦ 給 position 用
        return this;
    }

    public RsaBuilder setSpeed(double speedMs, TransmissionState transmissionState) {
        Integer speed = (int) (speedMs / 0.02);
        SpeedInfo speedInfo = new SpeedInfo(
                transmissionState,
                speed
        );
        return this;
    }

    public RsaBuilder setAccuracy(Integer semiMajor, Integer semiMinor, Integer orientation) {
        PosAccuracy posAccuracy = new PosAccuracy(semiMajor, semiMinor, orientation);
        return this;
    }

    public RsaBuilder setConfidence(
            TimeConfidence timeC,
            PosLevel posC,
            ElevationLevel elevC,
            HeadingConfidence headingC,
            SpeedLevel speedC,
            ThrottleConfidence throttleC
    ) {
        this.timeConfidence = timeC;
        this.posConfidence = new PosConfidence(posC, elevC);
        this.speedConfidence = new SpeedConfidence(headingC, speedC, throttleC);
        return this;
    }
}