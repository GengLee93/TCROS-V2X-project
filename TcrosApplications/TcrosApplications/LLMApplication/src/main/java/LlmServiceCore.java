
import CommonUtil.ObjectCloneUtil;
import CommonUtil.TcrosValidator.TcrosValidator;
import Configurations.CustomPromptTemplate;
import Configurations.LLMConfiguration;
import JsonMapper.JsonMapper;
import LlmModule.Assistant;
import LlmModule.tools.PromptTemplateBuilder;
import LlmModule.tools.TextClearer;
import TcrosProtocols.SignalStatusMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.message.SystemMessage;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import RecordObject.FailRecord;
import RecordObject.LlmModifyRecord;
import RecordObject.SuccessRecord;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class LlmServiceCore<T> {
    private final ChatLanguageModel chatLanguageModel;
    private final ChatMemory chatMemory;
    private final InMemoryEmbeddingStore<TextSegment> embeddingStore;
    private final Assistant<T> assistant;
    private final String ragFolderPath;
    private final CustomPromptTemplate promptTemplate;
    private final List<Integer> targetIds;
    private final Integer modifyLimit;
    private final List<LlmModifyRecord> modifyRecords;
    private Path logPath;
    private String logPrefix;
    private final LLMConfiguration llmConfiguration;
    private static final String JSON_FORMAT_ERROR = "Json Format Error";
    private static final String ILLEGAL_VALUE_ERROR = "Illegal Value Error";
    private static final String NONE = "None";
    public LlmServiceCore(LLMConfiguration configuration){
        llmConfiguration = configuration;
        if (llmConfiguration.targetIds != null) {
            targetIds = Arrays.stream(llmConfiguration.targetIds)
                    .boxed()
                    .toList();
        }else {
            targetIds = new ArrayList<>();
        }
        modifyLimit = llmConfiguration.modifyLimit;
        modifyRecords = new ArrayList<>();

        OllamaChatModel.OllamaChatModelBuilder builder = OllamaChatModel.builder();
        builder.baseUrl(llmConfiguration.url)
               .modelName(llmConfiguration.modelName)
               .temperature(llmConfiguration.temperature)
               .logRequests(true)
               .logResponses(true);

        if(llmConfiguration.jsonSchema)
            builder.responseFormat(ResponseFormat.JSON);

        chatLanguageModel = builder.build();

        chatMemory =  MessageWindowChatMemory.builder()
                .id(1)
                .maxMessages(10)
                .build();
        chatMemory.clear();
        chatMemory.add(SystemMessage.systemMessage(llmConfiguration.basicPrompt));
        promptTemplate = llmConfiguration.customPromptTemplate;
        ragFolderPath = llmConfiguration.ragFilesPath;
        embeddingStore = new InMemoryEmbeddingStore<>();

        ContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .minScore(0.75)
                .maxResults(5)
                .build();

//        QueryTransformer queryTransformer = ExpandingQueryTransformer.builder()
//                .chatLanguageModel(chatLanguageModel)
//                .build();

        ContentInjector contentInjector = DefaultContentInjector.builder()
                .promptTemplate(PromptTemplate.from(llmConfiguration.queryTemplate))
                .build();

        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
//                .queryTransformer(queryTransformer)
                .contentRetriever(retriever)
                .contentInjector(contentInjector)
                .build();

        AiServices<Assistant> assistantBuilder = AiServices.builder(Assistant.class);
        assistantBuilder.chatLanguageModel(chatLanguageModel)
                .chatMemory(chatMemory)
                .retrievalAugmentor(retrievalAugmentor);

        assistant = assistantBuilder.build();
    }

    public void setLogPath(Path lPath){
        logPath = Path.of(lPath.toString());
        logPrefix = logPath.getParent()
                .getParent()
                .getFileName()
                .toString()
                .replace("log-", "");
    }
    public String generateText(String prompt) {
        return chatLanguageModel.generate(prompt);
    }

    public String chat(String userMessage){
       return assistant.chat(userMessage);
    }

    public void clearChatMemory(){
        chatMemory.clear();
    }
    public void loadFolderFileToRag(){
        List<Document> documents = FileSystemDocumentLoader.loadDocuments(ragFolderPath,new TextDocumentParser());
//        DocumentSplitter documentSplitter = new DocumentByParagraphSplitter(1000,100);
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
//                .documentSplitter(documentSplitter)
            .textSegmentTransformer(textSegment -> TextSegment.from(
                    textSegment.metadata().getString("file_name") + "\n" + textSegment.text(),
                    textSegment.metadata()
            ))
            .embeddingStore(embeddingStore)
            .build();
        ingestor.ingest(documents);
    }

    public T modifyToObject(String objectJsonText){
        return assistant.modifyToObject(objectJsonText);
    }

    public String modifyToString(String objectJsonText){
        return assistant.modifyToString(objectJsonText);
    }

    public boolean containTarget(SignalStatusMessage ssm){
        if(targetIds == null || targetIds.isEmpty())
            return true;
        for(Integer targetId : targetIds) {
            if(ssm.getEntityRequestSigStatus(targetId) != null)
                return true;
        }
        return false;
    }
    public T repeatModifyByLlm(T message,Class<T> clazz,Integer modifiedId) throws JsonProcessingException {
        T modifiedMessage = ObjectCloneUtil.deepCopy(message, clazz);
        String originalJsonString = JsonMapper.exportJsonFileByObject(message,true);
        boolean hasJsonError = false;
        boolean hasValidateError = false;
        int modifyCount = 0 ;
        List<String> errorList = new ArrayList<>();
        Map<String,String> promptInjectMap = new HashMap<>();

        do{
            promptInjectMap.put("originalJson",originalJsonString);
            promptInjectMap.put("errorType", errorTypeText(hasJsonError, hasValidateError));
            promptInjectMap.put("errorContent",errorContent(errorList));
            String prompt = PromptTemplateBuilder.buildTemplate(promptTemplate,promptInjectMap);

            hasJsonError = false;
            hasValidateError = false;
            errorList.clear();

            /* 對LLM發出請求 */
            String replyString = modifyToString(prompt);
            System.out.println(replyString);

            try {
                modifiedMessage = JsonMapper.importObjectByJsonString(
                    TextClearer.repairJsonByPython(
                        TextClearer.extractMarkdownJsonBlock(replyString)
                    ),
                    clazz,
                    true
                );
            }catch (JsonProcessingException e){
                String errorMsg = e.getMessage();
                int lineSeparatorIdx = errorMsg.indexOf("\n");
                errorMsg = lineSeparatorIdx > 0 ? errorMsg.substring(0,lineSeparatorIdx) : errorMsg;
                errorList.add(errorMsg);
                hasJsonError = true;
            }
            if(!hasJsonError) {
                List<String> validateErrorList = TcrosValidator.validate(modifiedMessage);
                if (!validateErrorList.isEmpty()) {
                    hasValidateError = true;
                    errorList.addAll(validateErrorList);
                }
            }
            String modifiedSsmString =  JsonMapper.exportJsonFileByObject(modifiedMessage,true);
            modifyRecords.add(
                new LlmModifyRecord(
                    modifiedId,
                    prompt,
                    replyString,
                    originalJsonString,
                    hasJsonError ? JSON_FORMAT_ERROR : modifiedSsmString,
                    modifyCount,
                    errorContent(errorList),
                    errorTypeText(hasJsonError, hasValidateError)
                )
            );
            modifyCount++;
        }while (modifyCount < modifyLimit && (hasJsonError || hasValidateError));

        return modifiedMessage;
    }
    private String errorTypeText(boolean jsonFormatError, boolean validateError){
        if(jsonFormatError){
            return JSON_FORMAT_ERROR;
        }else if(validateError){
            return ILLEGAL_VALUE_ERROR;
        }
        return NONE;
    }

    private String errorContent(List<String> errorList){
        return errorList.isEmpty() ? NONE : String.join(System.lineSeparator(),errorList);
    }

    public void exportLlmModifyRecord() throws IOException {
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = mapper
                .schemaFor(LlmModifyRecord.class)
                .withHeader()
                .withColumnSeparator(',');

        File outputFile = logPath.resolve(logPrefix+"_llmModifyRecords.csv").toFile();
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), StandardCharsets.UTF_8)) {
            mapper.writer(schema).writeValues(writer).writeAll(modifyRecords);
        }
    }

    public void exportSuccessRecord() throws IOException{
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = mapper
                .schemaFor(SuccessRecord.class)
                .withHeader()
                .withColumnSeparator(',');
        List<SuccessRecord> successRecords = createSuccessRecords();
        File outputFile = logPath.resolve(logPrefix+"_successRecord.csv").toFile();
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), StandardCharsets.UTF_8)) {
            mapper.writer(schema).writeValues(writer).writeAll(successRecords);
        }
    }

    private List<SuccessRecord> createSuccessRecords(){
        List<SuccessRecord> successRecords = new ArrayList<>();
        for(LlmModifyRecord llmModifyRecord : modifyRecords){
            if(Objects.equals(llmModifyRecord.currentErrorType(), NONE)){
                successRecords.add(
                    new SuccessRecord(
                        llmModifyRecord.modifiedId(),
                        llmModifyRecord.modifyCount(),
                        llmModifyRecord.modifiedSsm(),
                        llmModifyRecord.originSsm()
                    )
                );
            }
        }
        return successRecords;
    }

    public void exportFailRecord() throws IOException {
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = mapper
                .schemaFor(FailRecord.class)
                .withHeader()
                .withColumnSeparator(',');
        List<FailRecord> failRecords = createFailRecords();

        File outputFile = logPath.resolve(logPrefix+"_failRecord.csv").toFile();
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), StandardCharsets.UTF_8)) {
            mapper.writer(schema).writeValues(writer).writeAll(failRecords);
        }
    }

    private List<FailRecord> createFailRecords(){
        List<FailRecord> failRecords = new ArrayList<>();
        for(LlmModifyRecord llmModifyRecord : modifyRecords){
            if(!Objects.equals(llmModifyRecord.currentErrorType(), NONE)){
                failRecords.add(
                    new FailRecord(
                        llmModifyRecord.modifiedId(),
                        llmModifyRecord.modifyCount(),
                        llmModifyRecord.originSsm(),
                        llmModifyRecord.modifiedSsm(),
                        llmModifyRecord.currentErrorType()
                    )
                );
            }
        }
        return failRecords;
    }

    public void exportLlmConfiguration() throws IOException{
        File outputFile = logPath.resolve(logPrefix + "_llmConfiguration.json").toFile();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.WRAP_ROOT_VALUE);
            writer.write(
                    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(llmConfiguration)
            );
        }
    }
}
