package CommonEnum.LaneType;

import CommonEnum.IDescriptionEnum;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;

public enum BikeLane implements IDescriptionEnum<String> {
    laneAllocation("0000000000000000", "調撥車道"),
    laneAllocationReverse("0000000000000001", "調撥車道（反向）"),
    pedestrianBikeLane("0000000000000010", "行人共用"),
    threeDimensionalBikeLane("0000000000000100", "立體車道"),
    defaultPedestrianBikeLane("0000000000001000", "預設時制"),
    unknownBikeLane("0000000000010000", "未知"),
    entitySeparationBikeLane("0000000000100000", "實體分隔車道");

    private final String id;
    private final String description;
    BikeLane(String id, String description) {
        this.id = id;
        this.description = description;
    }
    @Override
    public String getId() {
        return id;
    }
    @Override
    public String getDescription() {
        return description;
    }

    @JsonCreator
    static BikeLane fromID(String id){
        for (BikeLane e : values()){
            if (Objects.equals(id, e.getId())){
                return e;
            }
        }
        return unknownBikeLane;
    }
}
