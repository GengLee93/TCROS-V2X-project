package Tcros2MosaicProtocol.TrafficLightMessage;

public class TrafficLightControlInfo {
    private final String nodeId;
    private final int inLane;
    private final int outLane;

    public TrafficLightControlInfo(String nId,int iLane,int oLane){
        nodeId = nId;
        inLane = iLane;
        outLane = oLane;
    }

    public String getNodeId(){
        return nodeId;
    }

    public int getInLane() {
        return inLane;
    }

    public int getOutLane() {
        return outLane;
    }
}
