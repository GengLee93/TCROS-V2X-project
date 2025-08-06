package Tcros2MosaicProtocol.TrafficLightMessage;

import org.eclipse.mosaic.lib.objects.v2x.EncodedPayload;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.jetbrains.annotations.NotNull;

public class TrafficLightStatusMessage extends V2xMessage {
    private final TrafficLightsStateInfo trafficLightStateInfo;
    private final EncodedPayload payload;
    private static final long MIN_LEN = 16L;
    public TrafficLightStatusMessage(MessageRouting routing,TrafficLightsStateInfo tlStateInfo) {
        super(routing);
        payload = new EncodedPayload(MIN_LEN);
        trafficLightStateInfo = tlStateInfo;
    }
    @NotNull
    @Override
    public EncodedPayload getPayload() {
        return payload;
    }
    @Override
    public String toString(){
        return trafficLightStateInfo.toString();
    }
    public TrafficLightsStateInfo getTrafficLightStateInfo(){return trafficLightStateInfo;}
}
