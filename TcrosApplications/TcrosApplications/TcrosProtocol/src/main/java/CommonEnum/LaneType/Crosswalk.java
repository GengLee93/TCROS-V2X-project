package CommonEnum.LaneType;

import CommonEnum.IDescriptionEnum;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;

public enum Crosswalk implements IDescriptionEnum<String> {
    laneAllocationCrossing("0000000000000000", "調撥車道"),
    laneAllocationReverseCrossing("0000000000000001", "調撥車道（反向）"),
    pedestrianBikeCrossing("0000000000000010", "允許自行車通行的行人穿越道"),
    elevatedPedestrianCrossing("0000000000000100", "立體行人穿越道"),
    defaultPedestrianCrossing("0000000000001000", "預設行人時相"),
    unknownCrossing("0000000000010000", "未知"),
    pedestrianButtonCrossing("0000000000100000", "行人按鈕"),
    audiblePedestrianSignalCrossing("0000000001000000", "聲響行人號誌");

    private final String id;
    private final String description;
    Crosswalk(String id, String description) {
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
    static Crosswalk fromID(String id){
        for (Crosswalk e : values()){
            if (Objects.equals(id, e.getId())){
                return e;
            }
        }
        return unknownCrossing;
    }

}
