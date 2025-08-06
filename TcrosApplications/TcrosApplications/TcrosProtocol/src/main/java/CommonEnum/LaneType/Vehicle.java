package CommonEnum.LaneType;

import CommonEnum.IDescriptionEnum;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;

public enum Vehicle implements IDescriptionEnum<String> {
    laneAllocation("00000000", "調撥車道"),
    laneAllocationReverse("00000001", "調撥車道（反向）"),
    elevatedLane("00000010", "高架車道"),
    highOccupancyVehicleLane("00000100", "高乘載車道"),
    busOnlyLane("00001000", "公車專用道"),
    taxiOnlyLane("00010000", "計程車專用道"),
    governmentVehicleLane("00100000", "公務使用車道"),
    unknownLane("01000000", "未知"),
    specialVehicleLane("10000000", "特殊車種許可通行車道");
    private final String id;
    private final String description;
    Vehicle(String id, String description) {
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
    static Vehicle fromID(String id){
        for (Vehicle e : values()){
            if (Objects.equals(id, e.getId())){
                return e;
            }
        }
        return unknownLane;
    }
}
