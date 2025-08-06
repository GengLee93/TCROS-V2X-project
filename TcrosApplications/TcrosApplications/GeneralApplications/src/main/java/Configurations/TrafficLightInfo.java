package Configurations;

public class TrafficLightInfo {
    public String id;
    public String tlGroup;
    public String nodeId;
    @Override
    public String toString(){
        return String.format("Id:%s, tlGroup:%s, nodeId:%s",id,tlGroup,nodeId);
    }

}
