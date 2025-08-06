package CommonEnum;

import com.fasterxml.jackson.annotation.JsonCreator;
import jdk.jfr.Description;

import java.util.Objects;

@Description("Role of requester, 0 meaning basic vehicle, 13 meaning Fire truck,  14 meaning ambulance, 16 meaning Public Transportation.")
public enum RequestRole implements IDescriptionEnum<Integer>{
    basicVehicle(0,"小客車"),
    fire(13,"消防車"),
    ambulance(14,"救護車") ,
    transit(16,"大眾運輸車輛") ;
    private final Integer id;
    private final String description;

    RequestRole(Integer i,String d){
        this.id = i;
        this.description = d;
    }

    @Override
    public Integer getId() {
        return id;
    }
    @Override
    public String getDescription(){
        return description;
    }

    @JsonCreator
    static RequestRole fromID(Integer id){
        for (RequestRole e : values()){
            if (Objects.equals(id, e.getId())){
                return e;
            }
        }
        return basicVehicle;
    }
}
