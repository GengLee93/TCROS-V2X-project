package Configurations;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.*;
/*
 * 該Class僅供SharedLaneInfo使用(ConnectionInfoServerApplication提供初始化資料)
 * nodeConnectionInfos中包含每個節點(路口)對應的Connection(車道連結方向)與對應的id
 * id與連結資訊來自於每個場景中sumo的net.xml
 * 原因：在TCROS中會對路口的每個車道進行編碼，但實際編碼比較難在MOSAIC中重現，因此自訂車道編碼規則
 * 編碼概念是先取得該路口包含的每條邊，一個邊代表一條道路，一條道路可以有一個或兩個車道(入向或出向)
 * 1. 先取得與路口相連的邊
 * 2. 對所有邊計算包含的車道數量
 * 3. 將邊根據車道數量重複加入List，List中的順序即是車道編碼
 * 在SharedLaneInfo初始化時呼叫encodeTcrosLaneInfo對現有的節點資訊進行車道編碼。儲存在tcrosEncodeLaneInfos中
 */
public class ConnectionInfoConfiguration {
    public Map<String, List<ConnectionInfo>> nodeConnectionInfos;
    private Map<String, List<String>> tcrosEncodeLaneInfos;
    public List<ConnectionInfo> getNodeConnectionInfo(String nodeId){
        return nodeConnectionInfos.get(nodeId);
    }

    @JsonIgnore
    public void encodeTcrosLaneInfo(){
        tcrosEncodeLaneInfos = new HashMap<>();
        for (String nodeId : nodeConnectionInfos.keySet()){
            List<String> intersectionNodeLaneInfo = new ArrayList<>();
            Map<String,Integer> nodeDistinctEdge = getNodeDistinctEdge(nodeId);
            for(Map.Entry<String,Integer> entry : nodeDistinctEdge.entrySet()){
                for(int i = 0 ; i < entry.getValue() ; i++){
                    intersectionNodeLaneInfo.add(entry.getKey());
                }
            }
            tcrosEncodeLaneInfos.put(nodeId,intersectionNodeLaneInfo);
        }
    }

    @JsonIgnore
    public List<String> getTcrosNodeEdgeInfo(String nodeId){
        return tcrosEncodeLaneInfos.get(nodeId);
    }

    @JsonIgnore
    public int getTcrosLaneId(String nodeId, String edgeId, boolean isIncoming){
        List<String> edgeInfo = getTcrosNodeEdgeInfo(nodeId);
        if(edgeInfo == null)
            return -1;
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < edgeInfo.size(); i++) {
            if (edgeInfo.get(i).equals(edgeId)) {
                indexes.add(i);
            }
        }

        if (indexes.isEmpty()) {
            return -1;
        } else if (indexes.size() == 1) {
            return indexes.get(0);
        } else {
            return isIncoming ? indexes.get(0) : indexes.get(1);
        }
    }

    @JsonIgnore
    private Map<String, Integer> getNodeDistinctEdge(String nodeId){
        Map<String, Integer> distinctEdges = new LinkedHashMap<>();
        List<ConnectionInfo> connectionInfos = getNodeConnectionInfo(nodeId);
        for(ConnectionInfo connectionInfo : connectionInfos){
            String incomingEdge = connectionInfo.getIncomingEdge();
            distinctEdges.putIfAbsent(incomingEdge, 1);
        }
        for(ConnectionInfo connectionInfo : connectionInfos){
            String outgoingEdge = connectionInfo.getOutgoingEdge();
            distinctEdges.merge(outgoingEdge, 1, (oldVal, newVal) -> 2);
        }
        return distinctEdges;
    }

    @JsonIgnore
    public int getInOutConnectionIndex(String nodeId,String currentLane,String nextLane){
        List<ConnectionInfo> nodeConnectionInfo = getNodeConnectionInfo(nodeId);
        for(ConnectionInfo connectionInfo : nodeConnectionInfo){
            if(connectionInfo.isInOutPair(currentLane,nextLane)){
                return connectionInfo.id;
            }
        }
        return -1;
    }
    @Override
    public String toString(){
        return nodeConnectionInfos.toString();
    }
}
