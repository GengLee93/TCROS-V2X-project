package Configurations;
public class LLMConfiguration {
    public String modelName;
    public String url;
    public double temperature;
    public String ragFilesPath;
    public int[] targetIds;
    public int modifyLimit;
    public boolean jsonSchema;
    public String basicPrompt;
    public CustomPromptTemplate customPromptTemplate;
    public String queryTemplate;
}
