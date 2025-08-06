package CommonEnum;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum RsaPriority implements IDescriptionEnum<Integer> {
    PRIORITY_7(7, (byte) 0b11100000),
    PRIORITY_6(6, (byte) 0b01100000),
    PRIORITY_5(5, (byte) 0b10100000),
    PRIORITY_4(4, (byte) 0b00100000),
    PRIORITY_3(3, (byte) 0b11000000),
    PRIORITY_2(2, (byte) 0b01000000),
    PRIORITY_1(1, (byte) 0b10000000),
    PRIORITY_0(0, (byte) 0b00000000);

    private final Integer level;
    private final byte bitPattern;

    RsaPriority(Integer level, byte bitPattern) {
        this.level = level;
        this.bitPattern = bitPattern;
    }

    @Override
    public Integer getId() {
        return level;
    }

    @Override
    public String getDescription() {
        // 使用 Byte.toUnsignedInt 以確保正確轉換
        return String.format("%8s", Integer.toBinaryString(Byte.toUnsignedInt(bitPattern)))
                .replace(' ', '0');
    }

    public byte getBitPattern() {
        return bitPattern;
    }

    @JsonCreator
    public static RsaPriority fromLevel(int level) {
        for (RsaPriority p : values()) {
            if (p.level.equals(level)) return p;
        }
        return PRIORITY_0; // fallback
    }

    public static RsaPriority fromBitPattern(byte bits) {
        for (RsaPriority p : values()) {
            if (p.bitPattern == bits) return p;
        }
        return PRIORITY_0;
    }
}