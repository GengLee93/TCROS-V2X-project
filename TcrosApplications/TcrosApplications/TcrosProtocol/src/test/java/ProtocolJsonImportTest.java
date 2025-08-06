import JsonMapper.JsonMapper;
import TcrosProtocols.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.fail;

class ProtocolJsonImportTest {
    @Test
    void importSRMTest() {
        try {
            SignalRequestMessage srm = JsonMapper.importObjectByJsonFile("src/test/ExampleJSON/ExampleSRMJson.json", SignalRequestMessage.class,false);
            System.out.println(srm);
        }catch (IOException e){
            fail();
        }
    }
    @Test
    void importSSMTest(){
        try {
            SignalStatusMessage ssm = JsonMapper.importObjectByJsonFile("src/test/ExampleJSON/ExampleSSMJson.json", SignalStatusMessage.class,false);
            System.out.println(ssm);
        }catch (IOException e){
            e.printStackTrace();
            fail();
        }
    }
    @Test
    void importSSMContainRootTest(){
        try {
            SignalStatusMessage ssm = JsonMapper.importObjectByJsonFile("src/test/ExampleJSON/ExampleSSMJsonContainRoot.json", SignalStatusMessage.class,true);
            System.out.println(ssm);
        }catch (IOException e){
            e.printStackTrace();
            fail();
        }
    }
    @Test
    void importSPaTDataTest() {
        try {
            SPaTData spat = JsonMapper.importObjectByJsonFile("src/test/ExampleJSON/ExampleSPaTDataJson.json", SPaTData.class,false);
            System.out.println(spat);
        }catch (IOException e){
            fail();
        }
    }
    @Test
    void importMapDataTest() {
        try{
            V2XMapData mapData = JsonMapper.importObjectByJsonFile("src/test/ExampleJSON/ExampleMapDataJson.json",V2XMapData.class,false);
            System.out.println(mapData);
        }catch (IOException e){
            fail();
        }
    }
}
