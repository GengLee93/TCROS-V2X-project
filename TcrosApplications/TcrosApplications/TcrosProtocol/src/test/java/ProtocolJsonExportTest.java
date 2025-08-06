import JsonMapper.JsonMapper;
import TcrosProtocols.SPaTData;
import TcrosProtocols.SignalRequestMessage;
import TcrosProtocols.SignalStatusMessage;
import TcrosProtocols.V2XMapData;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.fail;

class ProtocolJsonExportTest {
    @Test
    void exportSRMTest() {
        try {
            SignalRequestMessage srm = JsonMapper.importObjectByJsonFile("src/test/ExampleJSON/ExampleSRMJson.json", SignalRequestMessage.class,false);
            System.out.println(JsonMapper.exportJsonFileByObject(srm,true));
        }catch (IOException e){
            fail();
        }
    }
    @Test
    void exportSSMTest(){
        try {
            SignalStatusMessage ssm = JsonMapper.importObjectByJsonFile("src/test/ExampleJSON/ExampleSSMJson.json", SignalStatusMessage.class,false);
            System.out.println(JsonMapper.exportJsonFileByObject(ssm,true));
        }catch (IOException e){
            fail();
        }
    }
    @Test
    void exportSPaTDataTest() {
        try {
            SPaTData spat = JsonMapper.importObjectByJsonFile("src/test/ExampleJSON/ExampleSPaTDataJson.json", SPaTData.class,false);
            System.out.println(JsonMapper.exportJsonFileByObject(spat,true));
        }catch (IOException e){
            fail();
        }
    }
    @Test
    void exportMapDataTest() {
        try{
            V2XMapData mapData = JsonMapper.importObjectByJsonFile("src/test/ExampleJSON/ExampleMapDataJson.json",V2XMapData.class,false);
            System.out.println(JsonMapper.exportJsonFileByObject(mapData,true));
        }catch (IOException e){
            fail();
        }
    }
}
