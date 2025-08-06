package RecordObject;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"packageNumber", "successCount","originalJson", "resultJson"})
public record SuccessRecord(
        Integer packageNumber,
        Integer successCount,
        String resultJson,
        String originalJson
) {
}
