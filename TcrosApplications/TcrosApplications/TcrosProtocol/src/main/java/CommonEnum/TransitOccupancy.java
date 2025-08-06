package CommonEnum;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;

public enum TransitOccupancy implements IDescriptionEnum<Integer> {
    occupancyUnknown(0, "狀態未知"),
    occupancyEmpty(1, "空車"),
    occupancyVeryLow(2, "乘載量極低"),
    occupancyLow(3, "乘載量低"),
    occupancyMed(4, "乘載量中等"),
    occupancyHigh(5, "乘載量高"),
    occupancyNearlyFull(6, "幾乎滿載"),
    occupancyFull(7, "滿載");

    private final Integer id;
    private final String description;
    TransitOccupancy(Integer id, String description) {
        this.id = id;
        this.description = description;
    }
    @Override
    public Integer getId() {
        return id;
    }
    @Override
    public String getDescription() {
        return description;
    }

    @JsonCreator
    static TransitOccupancy fromID(Integer id){
        for (TransitOccupancy e : values()){
            if (Objects.equals(id, e.getId())){
                return e;
            }
        }
        return occupancyUnknown;
    }

}


