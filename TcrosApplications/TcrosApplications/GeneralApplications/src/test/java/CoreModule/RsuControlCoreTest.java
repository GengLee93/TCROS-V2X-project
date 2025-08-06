package CoreModule;

import Configurations.RsuConfiguration;
import JsonMapper.JsonMapper;
import TcrosProtocols.SignalRequestMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.rti.TIME;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.Assert.fail;

class RsuControlCoreTest {
    private RsuControlCore rsuControlCore;
    @BeforeEach
    public void setup() throws IOException {
        Locale.setDefault(Locale.ENGLISH);
        ObjectMapper objectMapper = new ObjectMapper();

        RsuConfiguration rsuConfiguration = objectMapper.readValue(
                new File("src\\main\\resources\\ConfugurationJson\\TcrosRsuApplication_rsu_8.json"),
                RsuConfiguration.class
        );
        rsuControlCore = new RsuControlCore(GeoPoint.ORIGO,rsuConfiguration, Path.of(""));
    }
    @Test
    void testAssertRequestTypeBySrm() {
        SignalRequestMessage srm = null;
        try {
            srm = JsonMapper.importObjectByJsonFile("src\\test\\resources\\TestSrm.json", SignalRequestMessage.class,false);
            System.out.println(srm);
        }catch (IOException e){
            fail();
        }
        rsuControlCore.updateAllState(180 * TIME.SECOND);
        System.out.println(rsuControlCore.assertRequestTypeBySrm(srm));

    }
}