package CommonClass.SrmClass;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jdk.jfr.Description;

import java.io.Serializable;

public record Requests(
        @Description("Single intersection priority requests information")
        @NotNull
        @Valid
        Request request,
        @Description("Estimated arrival time, Total accumulated minutes for the year")
        @Min(0)
        Integer minute,
        @Description("Estimated arrival time, Total accumulated micro-second in this minute.")
        @Min(0)
        Integer second,
        @Description("Default is 0.")
        @Min(0)
        Integer duration
) implements Serializable {
    @JsonIgnore
    public Long getRequestIntersectionId(){
        try {
            return request.id().id();
        }catch (Exception e){
            return -1L;
        }
    }
}