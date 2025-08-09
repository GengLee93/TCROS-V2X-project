import os
import json

"""
這檔案可以將蒐集到的各紅綠燈 ID 轉換成 MOSAIC 中 scenario 
下 mapping_config.json 的 "trafficLights" 實體。
輸入: 包含每行 ID 的文字檔
輸出: JSON
"""

# 取得目前腳本的目錄
script_dir = os.path.dirname(os.path.abspath(__file__))
file_path = os.path.join(script_dir, "tlGroupId.txt")

# 讀取檔案並轉成 JSON 格式
traffic_lights = []
with open(file_path, "r") as file:
    for line in file:
        tl_id = line.strip()
        if tl_id:  # 忽略空行
            traffic_lights.append({
                "name": "TrafficLight",
                "tlGroupId": tl_id
            })

# 輸出成 JSON 檔案
output_path = os.path.join(script_dir, "trafficLights.json")
with open(output_path, "w", encoding="utf-8") as out_file:
    json.dump({"trafficLights": traffic_lights},
              out_file, indent=2, ensure_ascii=False)

print(f"✅ JSON 檔案已輸出至: {output_path}")
