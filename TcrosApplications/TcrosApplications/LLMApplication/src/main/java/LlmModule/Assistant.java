package LlmModule;

import dev.langchain4j.service.UserMessage;

public interface Assistant<T> {
    public T modifyToObject(@UserMessage String userMessage);
    public String modifyToString(@UserMessage String userMessage);
    public String chat(@UserMessage String userMessage);
}
