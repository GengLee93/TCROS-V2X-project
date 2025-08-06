import CommonUtil.TcrosValidator.TcrosValidator;
import Configurations.LLMConfiguration;
import JsonMapper.JsonMapper;
import LlmModule.tools.TextClearer;
import TcrosProtocols.SignalStatusMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

class LlmServiceCoreTest {
    private LlmServiceCore<SignalStatusMessage> llmServiceCore;
    @BeforeEach
    public void setup() throws IOException {
        Locale.setDefault(Locale.ENGLISH);
        ObjectMapper objectMapper = new ObjectMapper();

        LLMConfiguration llmConfiguration = objectMapper.readValue(
                new File("src\\test\\TestJson\\llmConfiguration.json"),
                LLMConfiguration.class
        );
        llmServiceCore = new LlmServiceCore<>(llmConfiguration);
        llmServiceCore.loadFolderFileToRag();
    }
//    @Test
    void ragReviseJsonTest(){
        String filePath = "src/test/TestJson/sentSsm.json";
        String prompt =
"""
If you want modifying the Original Signal Status Message to reject Entity 20 , how do you modify it? The `sigStatus.status` in Signal Status Message is enumeration, change it to reject correctly.
Original Signal Status Message:
 '''json
{
  "SignalStatusMessage" : {
    "timeStamp" : 127752,
    "second" : 1901,
    "sequenceNumber" : 0,
    "status" : [ {
      "sequenceNumber" : 1,
      "id" : {
        "region" : 1,
        "id" : 2900290158
      },
      "sigStatus" : [ {
        "requester" : {
          "id" : {
            "entityID" : 20
          },
          "request" : 0,
          "sequenceNumber" : 77,
          "role" : 0
        },
        "inboundOn" : {
          "lane" : 0
        },
        "outboundOn" : {
          "lane" : 5
        },
        "minute" : 127752,
        "second" : 901,
        "duration" : 0,
        "status" : 1
      } ]
    } ]
  }
}
'''
Previous Error Type:None
Previous Error Content :
None
""";
        try {
            String jsonString = new String(Files.readAllBytes(Paths.get(filePath)));
            System.out.println(jsonString + "\n" + prompt );
            System.out.println("====================");
            System.out.println(llmServiceCore.modifyToString( prompt));
        }catch (IOException e){
            Assertions.fail();
        }
    }

//    @Test
    void noRagReviseJsonTest(){
        String filePath = "src/test/TestJson/sentSsm.json";
        try {
            String prompt =
"""
Above is a SignalStatusMessage. If you want to perform a black hole attack on Entity 0 in this packet, how would you modify it?
""";
            String jsonString = new String(Files.readAllBytes(Paths.get(filePath)));
            System.out.println( jsonString + "\n" + prompt);
            System.out.println("====================");
            System.out.println(llmServiceCore.generateText(prompt + "\n" + jsonString));
        }catch (IOException e){
            Assertions.fail();
        }
    }

    @Test
    void testSsmValidator() throws JsonProcessingException {
        String filePath = "src/test/TestJson/resultJson.json";
        String jsonString;
        try {
            jsonString = new String(Files.readAllBytes(Paths.get(filePath)));
        }catch (IOException e){
            System.out.println(e.getMessage());
            Assertions.fail();
            return;
        }
        SignalStatusMessage ssm = JsonMapper.importObjectByJsonString(jsonString,SignalStatusMessage.class,false);
        System.out.println(ssm.toString());
        List<String> errorList = TcrosValidator.validate(ssm);
        System.out.println("Error:");
        for(String error : errorList){
            System.out.println(error);
        }
    }

    @Test
    void testJsonClearer()throws Exception{
        String filePath = "src/test/TestJson/clearerTestText.txt";
        String jsonString;
        jsonString = new String(Files.readAllBytes(Paths.get(filePath)));
        jsonString = TextClearer.extractMarkdownJsonBlock(jsonString);
        System.out.println(jsonString);
        jsonString = TextClearer.repairJsonByPython(jsonString);
        System.out.println(jsonString);

        SignalStatusMessage ssm = JsonMapper.importObjectByJsonString(jsonString,SignalStatusMessage.class,true);
        System.out.println(ssm);
        List<String> stringList = TcrosValidator.validate(ssm);
        System.out.println(stringList);

    }
}