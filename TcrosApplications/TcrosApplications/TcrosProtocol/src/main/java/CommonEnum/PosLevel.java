package CommonEnum;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jdk.jfr.Description;

@Description("Horizontal position confidence level, 0 means unknown or not available.")
public enum PosLevel implements IDescriptionEnum<Integer> {
    UNAVAILABLE(0, "未知");
    // ... 根據實際資料補上更多等級

    private final Integer id;
    private final String description;

    PosLevel(Integer id, String description) {
        this.id = id;
        this.description = description;
    }

    @Override
    public Integer getId() { return id; }

    @Override
    public String getDescription() { return description; }

    @JsonValue
    public Integer toValue() { return id; }

    @JsonCreator
    public static PosLevel fromValue(Integer value) {
        for (PosLevel level : values()) {
            if (level.getId().equals(value)) {
                return level;
            }
        }
        return UNAVAILABLE;
    }
}
