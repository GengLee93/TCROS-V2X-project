package CommonEnum;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;

public enum TransitStatus implements IDescriptionEnum<String>{
    loading("00000000", "停車狀態"),
    anADAuse("00000001", "身心障礙者服務"),
    aBikeLoad("00000010", "裝載自行車"),
    doorOpen("00000100", "開門載客中"),
    charging("00001000", "充電中"),
    atStopLine("00010000", "停止線上"),
    defaultVehicle("11111111","非大眾運輸車輛");
    private final String id;
    private final String description;

    TransitStatus(String id, String description) {
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
    static TransitStatus fromID(String id){
        for (TransitStatus e : values()){
            if (Objects.equals(id, e.getId())){
                return e;
            }
        }
        return defaultVehicle;
    }
}
