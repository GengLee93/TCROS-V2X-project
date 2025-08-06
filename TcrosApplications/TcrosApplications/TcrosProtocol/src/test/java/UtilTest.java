import Util.TimeUtil;
import org.junit.jupiter.api.Test;

public class UtilTest {
    @Test
    void testMinuteOfYears(){
        System.out.println(TimeUtil.msInMinute(-1L));
        System.out.println(TimeUtil.minuteOfYears(-1L));
        System.out.println(TimeUtil.hundredMsInHour(-1L));
    }
}
