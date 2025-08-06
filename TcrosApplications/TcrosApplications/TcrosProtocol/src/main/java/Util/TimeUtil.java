package Util;

import CommonClass.RsaClass.UtcTime;

import java.time.*;

/**
 * TimeUtil 是一個通用的時間處理工具類，
 * 提供將毫秒時間戳（Unix timestamp）轉換為特定時間單位的實用方法。
 * 適用於需要以「分鐘」、「毫秒」、「百毫秒」為單位進行時間計算的應用場景，
 */
public class TimeUtil {
    private TimeUtil() {
    }

    /**
     * 計算指定時間戳在當年中是第幾分鐘。
     * 例如：2025-01-02 00:01 將回傳 1441。
     *
     * @param milliTimeStamp 毫秒時間戳（Unix 時間）
     * @return 當年中的第幾分鐘（0~525599）
     */
    public static Integer minuteOfYears(Long milliTimeStamp) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(milliTimeStamp), ZoneId.systemDefault());
        LocalDateTime startOfYear = LocalDateTime.of(dateTime.getYear(), 1, 1, 0, 0);
        return Math.toIntExact(Duration.between(startOfYear, dateTime).toMinutes());
    }

    /**
     * 計算指定時間戳在該分鐘中是第幾毫秒。
     * 例如：00:01:23.456 → 回傳 23456。
     *
     * @param milliTimeStamp 毫秒時間戳
     * @return 該分鐘中的毫秒數（0~59999）
     */
    public static Integer msInMinute(Long milliTimeStamp) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(milliTimeStamp), ZoneId.systemDefault());
        int secondInMinute = dateTime.getSecond();
        int nanoInSecond = dateTime.getNano();
        return secondInMinute * 1000 + nanoInSecond / 1_000_000;
    }

    /**
     * 計算指定時間戳在該小時中是第幾個百毫秒(0.1秒)。
     * 例如：00:15:30.500 → 回傳 9300（15*60+30 秒 = 930 秒 → 9300 百毫秒）。
     *
     * @param milliTimeStamp 毫秒時間戳
     * @return 該小時中的百毫秒數（0~35999）
     */
    public static Integer hundredMsInHour(Long milliTimeStamp) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(milliTimeStamp), ZoneId.systemDefault());
        int secondsInHour = dateTime.getMinute() * 60 + dateTime.getSecond();
        int nanosInSecond = dateTime.getNano();
        int msInHour = secondsInHour * 1000 + nanosInSecond / 1_000_000;
        return msInHour / 100;
    }

    /**
     * 將毫秒時間戳轉換為UTC時間對象。
     * 此方法將Unix時間戳（毫秒）轉換為包含年、月、日、時、分、秒（毫秒）的UtcTime物件。
     *
     * @param milliTimeStamp 毫秒時間戳（Unix 時間）
     * @return UtcTime 包含完整UTC時間信息物件
     */
    public static UtcTime toUtcTime(Long milliTimeStamp) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(milliTimeStamp), ZoneOffset.UTC);
        return new UtcTime(
                dateTime.getYear(),
                dateTime.getMonthValue(),
                dateTime.getDayOfMonth(),
                dateTime.getHour(),
                dateTime.getMinute(),
                dateTime.getSecond() * 1000 + dateTime.getNano() / 1_000_000
        ) ;
    }
}