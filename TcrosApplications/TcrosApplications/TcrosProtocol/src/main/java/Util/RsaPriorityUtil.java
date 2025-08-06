package Util;

import CommonEnum.RsaPriority;


/**
 * RsaPriorityUtil 是 RSA 專案中用來解析優先權欄位的工具類。
 * 將 8 位元二進位字串轉換為 RSAPriority 枚舉。
 */
public class RsaPriorityUtil {

    /**
     * 將 8 位元二進位字串轉換為 RSAPriority 枚舉。
     * 若格式錯誤或無法解析，則回傳預設 PRIORITY_0。
     *
     * @param binaryString 8 位元二進位字串（例如 "00001000"）
     * @return 對應的 RSAPriority 枚舉
     */
    public static RsaPriority fromBinaryString(String binaryString) {
        if (binaryString == null || binaryString.length() != 8) {
            return RsaPriority.PRIORITY_0;
        }
        try {
            int bits = Integer.parseInt(binaryString, 2); // 二進位字串轉 int
            byte bitPattern = (byte) bits;
            return RsaPriority.fromBitPattern(bitPattern);
        } catch (NumberFormatException e) {
            return RsaPriority.PRIORITY_0;
        }
    }
}