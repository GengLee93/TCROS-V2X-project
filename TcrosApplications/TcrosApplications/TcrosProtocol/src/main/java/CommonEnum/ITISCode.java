package CommonEnum;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * ITIS (Intelligent Transportation Systems Information Type) 代碼
 * 根據 SAE J2540 標準定義的交通信息類型代碼，此 enum 僅用來模擬用途。
 */
public enum ITISCode implements IDescriptionEnum<Integer> {
    // 天氣相關
    RAIN(7169, "雨"),
    HEAVY_RAIN(7170, "大雨"),
    SNOW(7171, "雪"),
    FOG(7173, "霧"),

    // 道路狀況
    ACCIDENT(513, "事故"),
    ROADWORK(514, "道路施工"),
    CONGESTION(1025, "交通擁堵"),
    LANE_CLOSED(1281, "車道關閉"),

    // 特殊事件
    EMERGENCY_VEHICLE(1793, "緊急車輛"),
    POLICE_ACTIVITY(1795, "警察活動"),

    // 其他代碼...
    UNKNOWN(0, "未知");

    private final Integer code;
    private final String description;

    ITISCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public Integer getId() {
        return code;
    }
    @Override
    public String getDescription() {
        return description;
    }

    @JsonCreator
    public static ITISCode fromCode(int code) {
        for (ITISCode itis : ITISCode.values()) {
            // 防範空值及物件參考判斷是否相同
            if (itis != null && itis.code.equals(code)) {
                return itis;
            }
        }
        return UNKNOWN;
    }
}
