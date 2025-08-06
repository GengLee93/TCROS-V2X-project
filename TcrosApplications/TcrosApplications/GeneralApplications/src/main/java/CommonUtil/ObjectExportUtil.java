package CommonUtil;

import Tcros2MosaicProtocol.TrafficLightMessage.TrafficLightsStateInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.eclipse.mosaic.lib.objects.trafficlight.TrafficLight;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjectExportUtil {
    private ObjectExportUtil(){}
    public static <T> void  exportTcrosBaseMessage(File exportFileName, List<T> tcrosList)throws IOException{
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(exportFileName))) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.WRAP_ROOT_VALUE);
            writer.write(
                    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(tcrosList)
            );
        }
    }

    public static void exportTrafficLightInfoToJson(File exportFileName, Map<String, TrafficLightsStateInfo> trafficLightsStateInfoMap) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        Map<String,Object> jsonOutput = new HashMap<>();
        for (Map.Entry<String,TrafficLightsStateInfo> tlStateEntry : trafficLightsStateInfoMap.entrySet()) {
            String tlId = tlStateEntry.getKey();
            TrafficLightsStateInfo tlStateInfo = tlStateEntry.getValue();
            if(tlStateInfo != null) {
                Map<Integer,List<TrafficLight>> trafficLightsMap = tlStateInfo.getTrafficLightGroupMap();
                for(List<TrafficLight> trafficLights : trafficLightsMap.values()) {
                    if (trafficLights != null) {
                        List<Object> trafficLightInfo = createTrafficLightInfo(trafficLights);
                        jsonOutput.put(tlId, trafficLightInfo);
                    }
                }
            }
        }
        writer.writeValue(exportFileName, jsonOutput);
    }

    @NotNull
    private static List<Object> createTrafficLightInfo(List<TrafficLight> trafficLights) {
        List<Object> trafficLightInfo = new ArrayList<>();
        for (TrafficLight tl : trafficLights) {
            Map<String, Object> trafficLightDetails = new HashMap<>();
            trafficLightDetails.put("id", tl.getId());
            trafficLightDetails.put("incomingLane", tl.getIncomingLane());
            trafficLightDetails.put("outgoingLane", tl.getOutgoingLane());
            trafficLightInfo.add(trafficLightDetails);
        }
        return trafficLightInfo;
    }
}
