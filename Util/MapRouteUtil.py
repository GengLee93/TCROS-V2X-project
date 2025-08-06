import xml.etree.ElementTree as ET
import networkx as nx
import xml.etree.ElementTree as ET
import networkx as nx

# 解析 XML 文件
def parse_xml(file_path):
    tree = ET.parse(file_path)
    root = tree.getroot()
    
    edges = []
    for edge in root.findall('edge'):
        edge_id = edge.get('id')
        from_node = edge.get('from')
        to_node = edge.get('to')
        edges.append((from_node, to_node, edge_id))  # 加入 edge_id 作為邊的標籤
    
    return edges


# 檢查是否存在經過指定節點的路徑，並列出路徑
def check_path_and_list_edges(G, source, target,waypoints):
    try:
        path = []
        startPoint = source
        for waypoint in waypoints:
            way = nx.shortest_path(G, source=startPoint, target=waypoint)
            path.extend(way[:-1])
            startPoint = waypoint
        
        path.extend(nx.shortest_path(G, source=startPoint, target=target))
    
        
        # 根據路徑獲取邊資訊
        edges_in_path = []
        for i in range(len(path) - 1):
            from_node = path[i]
            to_node = path[i + 1]
            edge_id = G.edges[from_node, to_node]['id']
            edges_in_path.append(edge_id)
        
        return edges_in_path
    
    except nx.NetworkXNoPath:
        print(f"從 {source} 到 {target} 不存在路徑。")

# 主程式
if __name__ == "__main__":
    # 假設 XML 文件名為 edges.xml
    xml_file = "F:\Project\Thesis\Thesis\Eclispe MOSAIC Application\D_City_map_Base\sumo\D_City_map.edg.xml"
    
    # 解析 XML 並獲取邊列表
    edges = parse_xml(xml_file)
    
    # 建立圖形
    G = nx.DiGraph()  # 有向圖
    for from_node, to_node, edge_id in edges:
        G.add_edge(from_node, to_node, id=edge_id)
    
    # 測試節點之間的路徑
    source = "1144509770"   # 起點
    target = "2900290155"   # 終點
    waypoints = ["1144510002"]  # 指定的中間節點
    edges_in_path = check_path_and_list_edges(G, source, target, waypoints)
    # 格式化輸出
    edges_str = " ".join(edges_in_path)
    print(f"從 {source} 到 {target} 的路徑存在，邊如下：")
    print(f"edges=\"{edges_str}\"")
