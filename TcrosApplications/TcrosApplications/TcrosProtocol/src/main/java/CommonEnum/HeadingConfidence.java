package CommonEnum;

import com.fasterxml.jackson.annotation.JsonCreator;
import jdk.jfr.Description;

@Description("Heading direction confidence level, 0 means unknown or unavailable.")
public enum HeadingConfidence implements IDescriptionEnum<Integer> {
    UNAVAILABLE(0, "未知");

    private final Integer id;
    private final String description;

    HeadingConfidence(Integer id, String description) {
        this.id = id;
        this.description = description;
    }

    @Override
    public Integer getId() { return id; }

    @Override
    public String getDescription() { return description; }

    @JsonCreator
    public static HeadingConfidence fromValue(Integer value) {
        for (HeadingConfidence h : values()) {
            if (h.getId().equals(value)) {
                return h;
            }
        }
        return UNAVAILABLE;
    }
}
