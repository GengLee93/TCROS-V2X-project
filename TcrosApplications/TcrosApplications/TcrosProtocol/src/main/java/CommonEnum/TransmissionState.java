package CommonEnum;

import com.fasterxml.jackson.annotation.JsonCreator;

// 變速器狀態
public enum TransmissionState implements IDescriptionEnum<Integer> {
    NEUTRAL(0, "空檔"),
    PARK(1, "停車擋"),
    FORWARDGEARS(2, "前進檔"),
    REVERSEGEARS(3, "倒車檔"),
    UNAVAILABLE(7, "未知、無法取得值"); // 其他狀態

    private final Integer value;
    private final String description;

    TransmissionState(Integer value, String d) {
        this.value = value;
        this.description = d;
    }

    @Override
    public Integer getId() { return value; }

    @Override
    public String getDescription() { return description; }

    @JsonCreator
    public static TransmissionState fromValue(Integer value) {
        for (TransmissionState state : TransmissionState.values()) {
            if (state.getId().equals(value)) {
                return state;
            }
        }
        return UNAVAILABLE;
    }
}
