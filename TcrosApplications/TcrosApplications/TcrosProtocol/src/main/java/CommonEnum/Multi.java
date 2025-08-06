package CommonEnum;

import com.fasterxml.jackson.annotation.JsonCreator;
import jdk.jfr.Description;

@Description("車隊數量")
public enum Multi implements IDescriptionEnum<Integer>{
    unavailable (0, "無法使用或無設備"),
    singleVehicle (1, "一車"),
    multiVehicle (2, "車隊"),
    reserved (3, "保留值");

    private  final Integer id;
    private final String  description;

    Multi(Integer id, String description){
        this.id = id;
        this.description = description;
    }

    @Override
    public String getDescription(){ return description; }

    @Override
    public Integer getId(){ return id; }

    @JsonCreator
    public static Multi getById(Integer id){
        for (Multi m : Multi.values()){
            if (m.getId().equals(id)){
                return m;
            }
        }
        return unavailable;
    }
}
