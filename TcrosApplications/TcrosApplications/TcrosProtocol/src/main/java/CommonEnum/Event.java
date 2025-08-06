package CommonEnum;

import com.fasterxml.jackson.annotation.JsonCreator;
import jdk.jfr.Description;

@Description("緊急車輛狀態")
public enum Event implements IDescriptionEnum<Integer> {
    peUnavailable(0, "無法使用或無設備"),
    peEmergencyResponse(1, "緊急應變"),
    peEmergencyLightsActive(2, "緊急燈光告警"),
    peEmergencySoundActive(3, "緊急聲響告警"),
    peNonEmergencyLightsActive(4, "非緊急燈光告警"),
    peNonEmergencySoundActive(5, "非緊急聲響告警");

    private final Integer id;
    private final String description;

    Event(Integer id, String description) {
        this.id = id;
        this.description = description;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @JsonCreator
    public static Event fromValue(Integer id) {
        for (Event e : Event.values()) {
            if (e.getId().equals(id)) {
                return e;
            }
        }
        return peUnavailable;
    }
}
