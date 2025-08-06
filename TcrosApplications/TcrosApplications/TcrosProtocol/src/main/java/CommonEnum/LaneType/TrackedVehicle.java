package CommonEnum.LaneType;

import CommonEnum.IDescriptionEnum;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;

public enum TrackedVehicle implements IDescriptionEnum<String> {
    laneAllocationTracked("0000000000000000", "調撥車道"),
    laneAllocationReverseTracked("0000000000000001", "調撥車道（反向）"),
    commutingTracked("0000000000000010", "通勤鐵路"),
    lightTracked("0000000000000100", "輕軌"),
    heavyTracked("0000000000001000", "重運量鐵路"),
    otherTracked("0000000000010000", "其他軌道類型"),
    unknownTracked("0000000000100000", "未知");

    private final String id;
    private final String description;
    TrackedVehicle(String id, String description) {
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
    static TrackedVehicle fromID(String id){
        for (TrackedVehicle e : values()){
            if (Objects.equals(id, e.getId())){
                return e;
            }
        }
        return unknownTracked;
    }
}
