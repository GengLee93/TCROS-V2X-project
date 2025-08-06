package CommonEnum;

import com.fasterxml.jackson.annotation.JsonCreator;
import jdk.jfr.Description;

@Description("車輛類型 ")
public enum BasicType implements IDescriptionEnum<Integer> {
    none (0, "車型未知"),
    unknown (1, "無匹配車型"),
    special (2, "特殊車型"),
    moto (3, "摩托車"),
    car (4, "小客車"),
    carOther (5, "其他類型四輪車輛"),
    bus (6, "公車");

    private final Integer id;
    private final String description;

    BasicType(Integer id, String description) {
        this.id = id;
        this.description = description;
    }

    @Override
    public Integer getId() { return id; }

    @Override
    public String getDescription() { return description; }

    @JsonCreator
    public static BasicType forValue(Integer value) {
        for (BasicType v : BasicType.values()) {
            if (v.getId().equals(value)) {
                return v;
            }
        }
        return none;
    }
}
