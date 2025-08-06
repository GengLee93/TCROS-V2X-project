import json

def convert_wayids_to_connections(connections):
    resultConnections = []
    for connection in connections:
        print(connection)
        for way in connection["wayIds"]:
            parts = way.split("_")
            if len(parts) == 3:  # Ensure wayId has three parts
                resultConnections.append({
                    "laneId": parts[0],
                    "startNode": parts[1],
                    "endNode": parts[2],
                    "openProgram": ""
                })
    return resultConnections

# Input data
input_data = [
    {"wayIds": ["286370492_2900290172_3009856570","222611315_2900290157_3009856570","297136419_2900290158_3009856570"]}
]

# Convert to connections
result = convert_wayids_to_connections(input_data)

# Output result as JSON
print(json.dumps(result, indent=4))