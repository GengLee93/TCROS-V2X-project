import CommonUtil.TcrosBuilder.SrmBuilder;
import CommonUtil.TcrosValidator.TcrosValidator;
import JsonMapper.JsonMapper;
import TcrosProtocols.SignalRequestMessage;
import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.fail;

public class CommonUtilTest {
    @Test
    public void srmBuilderTest(){
        SrmBuilder srmBuilder = new SrmBuilder(-1,-1, GeoPoint.ORIGO);
        SignalRequestMessage srm = srmBuilder.create();
        List<String> errorList = TcrosValidator.validate(srm);
        System.out.println(errorList);
    }

    @Test
    public void testValidSrm(){
        try {
            SignalRequestMessage srm = JsonMapper.importObjectByJsonFile("src/test/resources/TestSrm.json", SignalRequestMessage.class,false);
            System.out.println(srm);
            System.out.println(TcrosValidator.validate(srm));
        }catch (IOException e){
            fail();
        }
    }
}
