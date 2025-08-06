package CommonEnum;

import com.fasterxml.jackson.annotation.JsonCreator;
import jdk.jfr.Description;

@Description("告警距離，表示告警訊息的有效範圍，與點位與方向共同使用")
public enum Extent implements IDescriptionEnum<Integer> {
    useInstantlyOnly(0, "收到訊息僅持續極短時間"),
    useFor3meters(1, "3公尺"),
    useFor10meters(2, "10公尺"),
    useFor50meters(3, "50公尺"),
    useFor100meters(4, "100公尺"),
    useFor500meters(5, "500公尺"),
    useFor1000meters(6, "1000公尺"),
    useFor5000meters(7, "5000公尺"),
    useFor10000meters(8, "10000公尺"),
    useFor50000meters(9, "50000公尺"),
    useFor100000meters(10, "100000公尺"),
    useFor500000meters(11, "500000公尺"),
    useFor1000000meters(12, "1000000公尺"),
    useFor5000000meters(13, "5000000公尺"),
    useFor10000000meters(14, "10000000公尺"),
    forever(15, "收到訊息永久顯示");

    private final Integer id;
    private final String description;

    Extent(Integer id, String description) {
        this.id = id;
        this.description = description;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @JsonCreator
    public static Extent fromId(Integer id) {
        for (Extent e : values()) {
            if (e.getId().equals(id)) {
                return e;
            }
        }
        return useInstantlyOnly; // default fallback
    }
}