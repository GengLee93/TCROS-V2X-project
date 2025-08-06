package CommonEnum;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum SpeedLevel implements IDescriptionEnum<Integer>{
    UNAVAILABLE(0, "未知");

    private final int id;
    private final String description;

    SpeedLevel(int id, String description) {
        this.id = id;
        this.description = description;
    }

    @Override
    public Integer getId() { return id; }

    @Override
    public String getDescription() { return description; }

    @JsonCreator
    public static SpeedLevel fromValue(Integer value) {
        for (SpeedLevel s : values()) {
            if (s.getId().equals(value)) {
                return s;
            }
        }
        return UNAVAILABLE;
    }
}
