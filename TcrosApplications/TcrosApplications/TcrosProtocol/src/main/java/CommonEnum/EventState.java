package CommonEnum;

import com.fasterxml.jackson.annotation.JsonCreator;
import jdk.jfr.Description;

import java.util.Objects;

public enum EventState implements IDescriptionEnum<Integer>{
    @Description("Unavailable")
    unavailable(0,"故障"),
    @Description("Dark")
    dark(1,"無啟用"),
    @Description("Stop then proceed")
    stopThenProceed(2,"紅燈停車再開"),
    @Description("Stop then remain")
    stopAndRemain(3,"行人紅燈停等"),
    @Description("Pre-movement")
    preMovement(4,"綠燈預告"),
    @Description("Permissive movement allowed")
    permissiveMovementAllowed(5,"行人允許綠燈(圓燈)"),
    @Description("Protected movement allowed")
    protectedMovementAllowed(6,"行人保護綠燈(箭頭)"),
    @Description("Permissive clearance")
    permissiveClearance(7,"允許黃燈(行人閃綠)"),
    @Description("Protected clearance")
    protectedClearance(8,"保護黃燈"),
    @Description("Caution conflicting traffic")
    cautionConflictingTraffic(9,"閃光號誌 ");

    private final Integer id;
    private final String description;

    EventState(Integer i,String d){
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
    static EventState fromID(Integer id){
        for (EventState e : values()){
            if (Objects.equals(id, e.getId())){
                return e;
            }
        }
        return unavailable;
    }
}
