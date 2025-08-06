package CommonEnum.LaneType;

import CommonEnum.IDescriptionEnum;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;

public enum Sidewalk implements IDescriptionEnum<String> {
    laneAllocationSidewalk("0000000000000000", "調撥車道"),
    laneAllocationReverseSidewalk("0000000000000001", "調撥車道（反向）"),
    pedestrianBikeSidewalk("0000000000000010", "與自行車共用人行道"),
    sidewalk("0000000000000100", "人行道"),
    bikeLeadingSidewalk("0000000000001000", "自行車須牽行人行道"),
    unknownCrossing("0000000000010000", "未知");

    private final String id;
    private final String description;
    Sidewalk(String id, String description) {
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
    static Sidewalk fromID(String id){
        for (Sidewalk e : values()){
            if (Objects.equals(id, e.getId())){
                return e;
            }
        }
        return unknownCrossing;
    }
}
