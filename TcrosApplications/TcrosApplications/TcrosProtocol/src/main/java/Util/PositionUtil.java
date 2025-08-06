package Util;

public class PositionUtil {
    private PositionUtil(){}

    /**
     * 將經緯度（度）轉換為「一千萬分度」格式。
     * 例如：121.123456 轉成 1211234560。
     *
     * @param oct 經緯度（double）
     * @return 一千萬分度格式（long）
     */
    public static Long toOneOfTenMicroDegrees(double oct){
        return (long)(oct * 1e7);
    }

    /**
     * 將「一千萬分度」格式還原為經緯度（度）。
     *
     * @param degrees 一千萬分度格式（long）
     * @return 經緯度（double）
     */
    public static double toFullDegrees(Long degrees){
        return degrees / 1e7;
    }
}
