package RecordObject;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"modifiedId", "prompt","replyText", "originSsm","modifiedSsm","modifyCount","previousModifyErrors","currentErrorType"})
public record LlmModifyRecord (
    Integer modifiedId,
    String prompt,
    String replyText,
    String originSsm,
    String modifiedSsm,
    Integer modifyCount,
    String previousModifyErrors,
    String currentErrorType
){}
