package CommonEnum;

import com.fasterxml.jackson.annotation.JsonCreator;
import jdk.jfr.Description;

@Description("燈光警報類型")
public enum LightUse implements IDescriptionEnum<Integer> {
    unavailable(0, "無法使用或無設備"),
    notInUse(1, "未開啟"),
    inUse(2, "開啟"),
    yellowCautionLights (3, "黃色警告燈"),
    schooldBusLights (4, "校車巴士燈"),
    arrowSignsActive (5, "箭頭標誌燈"),
    slowMovingVehicle (6, "慢速移動車輛");

    private final Integer id;
    private final String description;

    LightUse(Integer id, String description) {
        this.id = id;
        this.description = description;
    }

    @Override
    public Integer getId() { return id; }

    @Override
    public String getDescription() { return description; }

    @JsonCreator
    public static LightUse fromValue(Integer id) {
        for (LightUse e : values()) {
            if (e.getId().equals(id)) {
                return e;
            }
        }
        return unavailable;
    }
}
