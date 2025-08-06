package Configurations;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Objects;

public class ConnectionInfo {
    public int id;
    public String incomingLane;
    public String outgoingLane;

    public boolean isInOutPair(String inLane,String outLane){
        return Objects.equals(getIncomingEdge(),inLane) &&
               Objects.equals(getOutgoingEdge(),outLane);
    }

    @JsonIgnore
    public String getIncomingEdge(){
        return incomingLane.split("_")[0];
    }

    @JsonIgnore
    public String getOutgoingEdge(){
        return outgoingLane.split("_")[0];
    }
    @Override
    public String toString(){
        return String.format("id: %d, incomingLane: %s, outgoingLane: %s",id,incomingLane,outgoingLane);
    }
}
