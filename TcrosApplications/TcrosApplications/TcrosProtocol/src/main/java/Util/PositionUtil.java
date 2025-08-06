package Util;

public class PositionUtil {
    private PositionUtil(){}
    public static Long toOneOfTenMicroDegrees(double oct){
        return (long)(oct * 1e7);
    }
    public static double toFullDegrees(Long degrees){
        return degrees / 1e7;
    }
}
