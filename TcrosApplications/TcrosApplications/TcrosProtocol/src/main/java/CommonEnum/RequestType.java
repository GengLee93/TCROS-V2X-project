package CommonEnum;

import com.fasterxml.jackson.annotation.JsonCreator;
import jdk.jfr.Description;

import java.util.Objects;

public enum RequestType implements IDescriptionEnum<Integer> {

    @Description("Priority request type reserved")
    priorityRequestTypeReserved(0,"保留值"),   //保留值
    @Description("Priority request")
    priorityRequest(1,"優先請求"),              //在取得 SSM status requested 前(隱含 RSU及OBU尚未正式溝通)，將持續發送priorityReques
    @Description("Priority request updated")
    priorityRequestUpdate(2,"優先請求更新"),      //取得SSM status requested後，SRM將持續發送priorityRequestUpdate (無論SRM 內容是否更新)
    @Description("Priority request cancel")
    priorityCancellation(3,"請求取消") ;         //當優先請求車輛通過路口後，必須發送priorityCancellation
    private final Integer id;
    private final String description;

    RequestType(Integer i,String d){
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
    static RequestType fromID(Integer id){
        for (RequestType e : values()){
            if (Objects.equals(id, e.getId())){
                return e;
            }
        }
        return priorityRequest;
    }
}
