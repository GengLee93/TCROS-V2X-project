package CommonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ObjectCloneUtil {
    private ObjectCloneUtil(){}
    private static final ObjectMapper objectMapper = new ObjectMapper();
    public static <T> T deepCopy(T object, Class<T> clazz) {
        try {
            String json = objectMapper.writeValueAsString(object);
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
