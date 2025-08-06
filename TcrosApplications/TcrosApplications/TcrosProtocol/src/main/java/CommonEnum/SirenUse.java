package CommonEnum;

import com.fasterxml.jackson.annotation.JsonCreator;
import jdk.jfr.Description;

@Description("聲音警報類型")
public enum SirenUse implements IDescriptionEnum<Integer> {
    unavailable(0, "無法使用或無設備"),
    notInUse(1, "未開啟"),
    inUse(2, "開啟"),
    reserved(3, "保留值");

    private final Integer value;
    private final String description;

    SirenUse(Integer value, String description) {
        this.value = value;
        this.description = description;
    }

    @Override
    public Integer getId() { return value; }

    @Override
    public String getDescription() { return description; }

    @JsonCreator
    public static SirenUse fromValue(Integer value) {
        for (SirenUse s : SirenUse.values()) {
            if (s.value.equals(value)) {
                return s;
            }
        }
        return unavailable;
    }
}
