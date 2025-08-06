package Tcros2MosaicProtocol.TrafficLightMessage;

import org.eclipse.mosaic.lib.objects.v2x.EncodedPayload;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RsuTrafficLightMessage extends V2xMessage {
    private final List<TrafficLightControlInfo> trafficLightControlInfoList;
    private final EncodedPayload payload;
    private static final long MIN_LEN = 16L;
    public RsuTrafficLightMessage(MessageRouting routing,List<TrafficLightControlInfo> tInfo){
        super(routing);
        payload = new EncodedPayload(MIN_LEN);
        trafficLightControlInfoList = tInfo;
    }

    @NotNull
    @Override
    public EncodedPayload getPayload() {
        return this.payload;
    }

    public List<TrafficLightControlInfo> getTrafficLightControlInfoList() {
        return trafficLightControlInfoList;
    }
}
