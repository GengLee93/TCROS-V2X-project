package CommonUtil.TcrosBuilder;

import CommonClass.RsaClass.*;
import CommonEnum.*;
import TcrosProtocols.RoadSideAlert;
import Util.TimeUtil;

import java.util.ArrayList;
import java.util.List;

/*
* timeStamp 對應封包廣播時間點(會更新)
* UtcTime 對應事件發生的時間，建構時就固定了
 */
public class RsaBuilder {
    private int timeStamp;
    private Integer msgCnt;
    private final long now;
    private ITISCode typeEvent;
    private final List<ITISCode> description;
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

    private String senderConnectionId;
    private Integer senderLaneIndex;

    public RsaBuilder(long now) {
        this.now = now;
        description = new ArrayList<>();
        this.position = new PositionInfo(
                new UtcTime(0, 0, 0, 31, 60, 65535),
                1800000001L,
                900000001L,
                -4096L,
                28880,
                new SpeedInfo(TransmissionState.UNAVAILABLE, 8191),
                new PosAccuracy(255, 255, 65535),
                TimeConfidence.Unavailable,
                new PosConfidence(PosLevel.UNAVAILABLE, ElevationLevel.UNAVAILABLE),
                new SpeedConfidence(HeadingConfidence.UNAVAILABLE, SpeedLevel.UNAVAILABLE, ThrottleConfidence.UNAVAILABLE)
        );
    }

    public RoadSideAlert create() {
        return new RoadSideAlert(
                msgCnt,
                TimeUtil.minuteOfYears(now),
                typeEvent,
                description,
                priority,
                headingBitString,
                extent,
                position
                );
    }

    public RsaBuilder setMsgCnt(int msgCnt) {
        this.msgCnt = msgCnt;
        return this;
    }

    public RsaBuilder setTypeEvent(ITISCode typeEvent) {
        this.typeEvent = typeEvent != null ? typeEvent : ITISCode.UNKNOWN;
        return this;
    }

    public RsaBuilder addDescription(ITISCode code) {
        if (code != null
                && !code.equals(typeEvent)          // 不該跟主要事件重複
                && !this.description.contains(code) // 不重複加入相同事件
                && description.size() < 8)          // 最多8則 ITIS code 事件描述
        {
            this.description.add(code);
            System.out.println("Description added, current event: " + code.toString());
        }
        else {
            System.out.println("Description NOT added: "
                    + (code == null ? "null" : code.equals(typeEvent)
                    ? "equals typeEvent" : this.description.contains(code)
                    ? "already contains" : "description size limit"));
        }
        return this;
    }

    public RsaBuilder setPriority(RsaPriority priority) {
        this.priority = priority != null ? priority : RsaPriority.PRIORITY_0;
        return this;
    }

    public RsaBuilder setHeadingBitString(String bitString) {
        if(bitString != null && bitString.length() == 16){
            this.headingBitString = bitString;
        } else {
            this.headingBitString = "0000000000000000";
        }
        return this;
    }

    /**
     * 接受 heading 角度（以度为单位的浮点数）并转换为16位二进制字符串
     * @param headingDegree 航向角度（0-360度）
     * @return RsaBuilder 实例
     */
    public RsaBuilder setHeadingByDegreeString(Double headingDegree) {
        if (headingDegree != null) {
            // 将角度标准化到 0-360 范围内
            double normalizedDegree = headingDegree % 360;
            if (normalizedDegree < 0) normalizedDegree += 360;

            // 将角度映射到 16 个方向之一（每个方向覆盖 22.5 度）
            int sector = (int) (normalizedDegree / 22.5) % 16;

            // 创建一个 16 位的字符串，在相应位置设置为 1
            StringBuilder bitString = new StringBuilder("0000000000000000");
            bitString.setCharAt(sector, '1');

            this.headingBitString = bitString.toString();
            this.headingDegrees = (int) normalizedDegree; // 更新 headingDegrees 字段
        } else {
            this.headingBitString = "0000000000000000";
            this.headingDegrees = 0;
        }
        return this;
    }

    public RsaBuilder setExtent(Extent extent) {
        this.extent = extent != null ? extent : Extent.useInstantlyOnly;
        return this;
    }

    public RsaBuilder setPosition(UtcTime utcTime, Long longitude, Long latitude, Long elev) {
        Long elevation =  Math.max(-4096, Math.min(elev, 61439));
        this.position = new PositionInfo(
                utcTime != null ? utcTime : new UtcTime(0, 0, 0, 31, 60, 65535),
                longitude != null ? longitude : 0L,
                latitude != null ? latitude : 0L,
                elevation,
                headingDegrees != null ? headingDegrees : 0,
                speedInfo != null ? speedInfo : new SpeedInfo(TransmissionState.UNAVAILABLE, 8191),
                posAccuracy != null ? posAccuracy : new PosAccuracy(255, 255, 65525),
                timeConfidence != null ? timeConfidence : TimeConfidence.Unavailable,
                posConfidence != null ? posConfidence : new PosConfidence(PosLevel.UNAVAILABLE, ElevationLevel.UNAVAILABLE),
                speedConfidence != null ? speedConfidence : new SpeedConfidence(HeadingConfidence.UNAVAILABLE, SpeedLevel.UNAVAILABLE, ThrottleConfidence.UNAVAILABLE)
        );

        return this;
    }

    public RsaBuilder setHeadingByDegree(Integer degree) {
        if (degree != null && degree >= 0 && degree <= 28800) {
            int sector = (int) ((degree * 0.0125) / 22.5) % 16; // 將 heading 轉換為 16 向量之一
            StringBuilder sb = new StringBuilder("0000000000000000");
            sb.setCharAt(sector, '1');
            this.headingDegrees = degree;
        } else {
            this.headingDegrees = 0;
        }
        return this;
    }

    public RsaBuilder setSpeed(TransmissionState transmissionState, Double speedMs) {
        TransmissionState ts = transmissionState != null ? transmissionState : TransmissionState.UNAVAILABLE;
        int speed = speedMs != null ? (int) (speedMs / 0.02) : 8191;
        this.speedInfo = new SpeedInfo(ts, speed);
        return this;
    }

    public RsaBuilder setAccuracy(Integer semiMajor, Integer semiMinor, Integer orientation) {
        this.posAccuracy = new PosAccuracy(
                semiMajor != null ? semiMajor : 255,
                semiMinor != null ? semiMinor : 255,
                orientation != null ? orientation : 65525
        );
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
        this.timeConfidence = timeC != null ? timeC : TimeConfidence.Unavailable;
        this.posConfidence = new PosConfidence(
                posC != null ? posC : PosLevel.UNAVAILABLE,
                elevC != null ? elevC : ElevationLevel.UNAVAILABLE
        );
        this.speedConfidence = new SpeedConfidence(
                headingC != null ? headingC : HeadingConfidence.UNAVAILABLE,
                speedC != null ? speedC : SpeedLevel.UNAVAILABLE,
                throttleC != null ? throttleC : ThrottleConfidence.UNAVAILABLE
        );
        return this;
    }


    public RsaBuilder setSenderConnectionId(String connId) {
        this.senderConnectionId = connId;
        return this;
    }
    public RsaBuilder setSenderLaneIndex(Integer laneIdx) {
        this.senderLaneIndex = laneIdx;
        return this;
    }
}