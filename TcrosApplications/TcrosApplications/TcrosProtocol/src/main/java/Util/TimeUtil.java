package Util;

import java.time.*;

public class TimeUtil {
    private TimeUtil(){}
    public static Integer minuteOfYears(Long milliTimeStamp) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(milliTimeStamp), ZoneId.systemDefault());
        LocalDateTime startOfYear = LocalDateTime.of(dateTime.getYear(), 1, 1, 0, 0);
        return Math.toIntExact(Duration.between(startOfYear, dateTime).toMinutes());
    }
    public static Integer msInMinute(Long milliTimeStamp){
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(milliTimeStamp), ZoneId.systemDefault());
        int secondInMinute = dateTime.getSecond();
        int nanoInSecond = dateTime.getNano();
        return secondInMinute * 1000 + nanoInSecond / 1_000_000;
    }

    public static Integer hundredMsInHour(Long milliTimeStamp){
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(milliTimeStamp), ZoneId.systemDefault());
        int secondsInHour = dateTime.getMinute() * 60 + dateTime.getSecond();
        int nanosInSecond = dateTime.getNano();
        int msInHour = secondsInHour * 1000 + nanosInSecond / 1_000_000;
        return msInHour / 100;
    }
}
