package CommonClass.SharedClass;

import jdk.jfr.Description;

import java.io.Serializable;

public record Position (
        @Description("Latitude, unit is 10 micro degrees ,900000001 means no information.")
        Long lat,
        @Description("Longitude, unit is 10 micro degrees ,1800000001 means no information.")
        Long lon,
        @Description("Default is 0.")
        Long elevation
)implements Serializable {}
