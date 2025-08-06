package Tcros2MosaicProtocol;

import org.eclipse.mosaic.lib.objects.v2x.EncodedPayload;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public class TcrosProtocolV2xMessage<T extends Serializable> extends V2xMessage {
    private final T tcrosProtocol;
    private final String className;
    private final EncodedPayload payload;
    private static final long MIN_LEN = 16L;
    private String senderId;

    public TcrosProtocolV2xMessage(MessageRouting routing, T protocolBody,Class<T> type) {
        super(routing);
        this.tcrosProtocol = protocolBody;
        className = type.getName();
        payload = new EncodedPayload(tcrosProtocol.toString().length(),MIN_LEN);
    }
    @NotNull
    @Override
    public EncodedPayload getPayload() {
        return this.payload;
    }

    @Override
    public String toString(){
        return this.tcrosProtocol.toString();
    }
    public T getTcrosProtocol(){
        return this.tcrosProtocol;
    }
    public String getProtocolClassName(){
        return this.className;
    }
    public String getSenderId(){return this.senderId;}
    public void setSenderId(String s){this.senderId = s;}


}
