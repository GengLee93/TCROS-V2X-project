package CommonEnum;

import com.fasterxml.jackson.annotation.JsonCreator;

// Throttle confidence level
public enum ThrottleConfidence implements IDescriptionEnum<Integer> {
    UNAVAILABLE(0, "未知");

    private final Integer id;
    private final String description;

    ThrottleConfidence(Integer id, String description) {
        this.id = id;
        this.description = description;
    }

    @Override
    public Integer getId() { return id; }

    @Override
    public String getDescription() { return description; }

    @JsonCreator
    public static ThrottleConfidence fromValue(Integer value) {
        for (ThrottleConfidence t : values()) {
            if (t.getId().equals(value)) {
                return t;
            }
        }
        return UNAVAILABLE;
    }
}

