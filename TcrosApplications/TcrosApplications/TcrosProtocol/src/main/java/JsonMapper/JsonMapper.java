package JsonMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;

public class JsonMapper {
    private JsonMapper(){}
    public static <T> T importObjectByJsonFile(String path,Class<T> targetClazz,boolean containRoot) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        if(containRoot)
            mapper.enable(DeserializationFeature.UNWRAP_ROOT_VALUE);
        return mapper.readValue(new File(path), targetClazz);
    }

    public static <T> String exportJsonFileByObject(T object,boolean containRoot) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        if(containRoot)
            mapper.enable(SerializationFeature.WRAP_ROOT_VALUE);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
    }
    public static <T> T importObjectByJsonString(String json, Class<T> targetClazz,boolean containRoot) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        if(containRoot)
            mapper.enable(DeserializationFeature.UNWRAP_ROOT_VALUE);
        return mapper.readValue(json, targetClazz);
    }
}
