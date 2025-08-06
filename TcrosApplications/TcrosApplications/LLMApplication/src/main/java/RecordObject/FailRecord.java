package RecordObject;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"packageNumber","modifyCount", "originalJson","resultJson", "errorType"})
public record FailRecord(
        Integer packageNumber,
        Integer modifyCount,
        String originalJson,
        String resultJson,
        String errorType
) {
}
