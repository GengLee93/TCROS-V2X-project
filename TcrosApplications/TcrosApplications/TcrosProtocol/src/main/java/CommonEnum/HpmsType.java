package CommonEnum;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;

public enum HpmsType implements IDescriptionEnum<Integer> {
    none(0, "資訊無法取得"),
    car(4, "小客車"),
    bus(6, "公車");
    private final Integer id;
    private final String description;
    HpmsType(Integer code, String description) {
        this.id = code;
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
    static HpmsType fromID(Integer id){
        for (HpmsType e : values()){
            if (Objects.equals(id, e.getId())){
                return e;
            }
        }
        return none;
    }

}

