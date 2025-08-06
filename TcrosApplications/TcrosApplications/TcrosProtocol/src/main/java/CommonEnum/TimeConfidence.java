package CommonEnum;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TimeConfidence implements IDescriptionEnum<Integer> {
    Unavailable(0, "未知");

    private final Integer value;
    private final String description;

    TimeConfidence(Integer value, String d) {
        this.value = value;
        this.description = d;
    }

    @Override
    public Integer getId() { return value; }

    @Override
    public String getDescription() { return description; }

    @JsonCreator
    public static TimeConfidence fromValue(Integer value) {
        for (TimeConfidence c : values()) {
            if (c.getId().equals(value)) {
                return c;
            }
        }
        return Unavailable;
    }

}
