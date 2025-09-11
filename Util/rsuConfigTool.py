import json
import os

# === 路徑設定 ===
input_file = r"C:\Users\lizhe\Desktop\eclipse-mosaic-24.1\scenarios\ntut\mapping\mapping_config.json"
output_dir = r"C:\Users\lizhe\Desktop\eclipse-mosaic-24.1\scenarios\ntut\application"
os.makedirs(output_dir, exist_ok=True)

# === 讀取 mapping_config.json ===
with open(input_file, "r", encoding="utf-8") as f:
    data = json.load(f)

# === 建立 tlGroupId → trafficLight 映射 ===
tl_map = {tl["tlGroupId"]: tl for tl in data["trafficLights"]}

# === 過濾掉 A00，並排序剩下的 RSU ===


def extract_rsu_index(label: str) -> int:
    """從 label 中取出 A01 → 1、A17 → 17"""
    try:
        base = label.split("/")[0]  # 取 A01
        return int(base[2:])        # 取 01 → 1
    except Exception as e:
        print(f"❌ 無法解析 RSU label: {label}，錯誤：{e}")
        return 9999  # 排到最後


filtered_rsus = [rsu for rsu in data["rsus"]
                 if not rsu["label"].startswith("A00")]
filtered_rsus.sort(key=lambda x: extract_rsu_index(x["label"]))

# === 處理每個 RSU ===
for idx, rsu in enumerate(filtered_rsus):
    label_parts = rsu["label"].split("/")
    tl_ids = label_parts[1:]  # e.g., ["5372642442", "631668976", ...]

    traffic_light_info_list = []
    for tl_index, tl_id in enumerate(tl_ids):
        if tl_id in tl_map:
            traffic_light_info_list.append({
                "id": f"tl_{tl_index + 1}",
                "tlGroup": tl_id,
                "nodeId": tl_id
            })
        else:
            print(f"⚠️ tlGroupId {tl_id} 不在 trafficLights 清單中。")

    rsu_json = {
        "label": rsu["label"].split("/")[0],  # 只保留 A01、A02...
        "priorityStartTime": 0,
        "priorityEndTime": 999,
        "trafficLightInfoList": traffic_light_info_list
    }

    # === 輸出檔名格式：TcrosRsuApplication_rsu_0.json ~ rsu_16.json ===
    output_filename = f"TcrosRsuApplication_rsu_{idx}.json"
    output_path = os.path.join(output_dir, output_filename)

    with open(output_path, "w", encoding="utf-8") as out_f:
        json.dump(rsu_json, out_f, indent=2, ensure_ascii=False)

print(f"✅ 共輸出 {len(filtered_rsus)} 個 RSU 設定檔至：{output_dir}")
