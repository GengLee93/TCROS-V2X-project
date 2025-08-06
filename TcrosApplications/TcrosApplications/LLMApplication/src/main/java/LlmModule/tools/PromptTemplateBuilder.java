package LlmModule.tools;

import Configurations.CustomPromptTemplate;

import java.util.Map;

public class PromptTemplateBuilder {
    private PromptTemplateBuilder(){}

    public static String buildTemplate(CustomPromptTemplate customPromptTemplate, Map<String,String> promptValueMap){
        String prompt = customPromptTemplate.template;
        for(Map.Entry<String,String> replace : customPromptTemplate.replaceMap.entrySet()){
            if(replace.getValue() != null){
                prompt = prompt.replaceAll("<"+replace.getKey()+">",replace.getValue());
            }
        }
        for(Map.Entry<String,String> promptValue : promptValueMap.entrySet()){
            prompt = prompt.replaceAll("<"+promptValue.getKey()+">",promptValue.getValue());
        }
        return prompt;
    }
}
