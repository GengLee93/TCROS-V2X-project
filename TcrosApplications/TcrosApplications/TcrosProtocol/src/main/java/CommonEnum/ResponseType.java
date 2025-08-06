package CommonEnum;

import com.fasterxml.jackson.annotation.JsonCreator;
import jdk.jfr.Description;

@Description("出勤型態")
public enum ResponseType implements IDescriptionEnum<Integer>{
    notInUseOrNotEquipped (0, "無設備或未啟用"),
    emergency (1, "緊急狀態"),
    nonEmergency (2, "非緊急狀態"),
    pursuit (3, "追趕"),
    stationary (4, "停止於路側"),
    slowMoving (5, "慢速移動"),
    stopAndGoMovement (6, "走走停停");

    private final Integer id;
    private final String description;

    ResponseType(Integer id, String description) {
        this.id = id;
        this.description = description;
    }

    @Override
    public Integer getId() { return id; }

    @Override
    public String getDescription() { return description; }

    @JsonCreator
    public static ResponseType fromDescription(String description) {
        for (ResponseType e : ResponseType.values()) {
            if (e.description.equals(description)) {
                return e;
            }
        }
        return notInUseOrNotEquipped;
    }
}
