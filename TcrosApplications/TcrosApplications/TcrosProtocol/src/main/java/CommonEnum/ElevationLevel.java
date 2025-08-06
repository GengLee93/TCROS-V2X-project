package CommonEnum;

import com.fasterxml.jackson.annotation.JsonCreator;
import jdk.jfr.Description;

@Description("Elevation confidence level, 0 means unknown or not available.")
public enum ElevationLevel implements IDescriptionEnum<Integer> {
    UNAVAILABLE(0, "未知");
    // ... 根據實際需求擴充等級

    private final Integer id;
    private final String description;

    ElevationLevel(Integer id, String description) {
        this.id = id;
        this.description = description;
    }

    @Override
    public Integer getId() { return id; }

    @Override
    public String getDescription() { return description; }

    @JsonCreator
    public static ElevationLevel fromValue(Integer value) {
        for (ElevationLevel level : values()) {
            if (level.getId().equals(value)) {
                return level;
            }
        }
        return UNAVAILABLE;
    }
}
