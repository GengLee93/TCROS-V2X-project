package CommonEnum;

import com.fasterxml.jackson.annotation.JsonCreator;
import jdk.jfr.Description;

import java.util.Objects;

public enum SignalStatus implements IDescriptionEnum<String> {
    @Description("Manual control is enabled")
    manualControlIsEnabled("0000000000000001", "手動控制"),
    @Description("Stop time is activated")
    stopTimeIsActivated("0000000000000010", "運作時間鎖定"),
    @Description("Failure flash")
    failureFlash("0000000000000100", "故障閃燈"),
    @Description("Preempt is active")
    preemptIsActive("0000000000001000", "絕對優先"),
    @Description("Signal priority is active")
    signalPriorityIsActive("0000000000010000", "條件優先"),
    @Description("Fixed time operation")
    fixedTimeOperation("0000000000100000", "定時控制"),
    @Description("Failure mode")
    failureMode("0000000100000000", "異常"),
    @Description("Off")
    off("0000001000000000", "關閉執行"),
    @Description("Unknown")
    unknown("0000010000000000","未知");
    private final String id;
    private final String description;

    SignalStatus(String id, String description) {
        this.id = id;
        this.description = description;
    }
    @Override
    public String getId() {
        return id;
    }
    @Override
    public String getDescription() {
        return description;
    }

    @JsonCreator
    static SignalStatus fromID(String id){
        for (SignalStatus e : values()){
            if (Objects.equals(id, e.getId())){
                return e;
            }
        }
        return unknown;
    }

}

