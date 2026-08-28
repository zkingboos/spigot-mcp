#!/bin/bash
# Practical bug demonstration script for spigot-mcp PR #3
# Uses REST batch API at /api/tools/batch
# Correct argument format per schema: region.pos1, region.pos2

set -e

echo "=========================================="
echo "Spigot MCP Bug Demonstration (REST API)"
echo "=========================================="

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Build plugin
echo -e "${YELLOW}Building plugin...${NC}"
./gradlew shadowJar --quiet

# Start modern server
echo -e "${YELLOW}Starting modern server (1.21.4 + FAWE)...${NC}"
docker compose up -d

# Wait for server to start
echo -e "${YELLOW}Waiting for server to fully start (60s)...${NC}"
sleep 60

# Check server logs for backend detection
echo -e "${YELLOW}Checking backend detection...${NC}"
docker compose logs spigot | grep -i "worldedit backend" || echo "Backend log not found yet"

# Function to make batch tool calls via REST API
batch_call() {
    local calls_json=$1
    
    curl -s -X POST http://localhost:8080/api/tools/batch \
        -H "Content-Type: application/json" \
        -d "{\"calls\": $calls_json}"
}

# Test 1: List tools via REST
echo -e "\n${GREEN}Test 1: List available tools (REST)${NC}"
curl -s http://localhost:8080/api/tools | jq '.[] | {name, description}'

# Test 2: setBlocks - small region (CORRECT FORMAT)
echo -e "\n${GREEN}Test 2: setBlocks - small region (6x1x6 = 36 blocks)${NC}"
batch_call '[{"id":2,"name":"set_blocks","arguments":{"region":{"pos1":{"x":0,"y":64,"z":0,"world":"world"},"pos2":{"x":5,"y":64,"z":5,"world":"world"}},"material":"stone"}}]' | jq .

# Test 3: setBlocks - large region (should hit limit)
echo -e "\n${GREEN}Test 3: setBlocks - large region 101x1x101 = 10201 blocks${NC}"
batch_call '[{"id":3,"name":"set_blocks","arguments":{"region":{"pos1":{"x":100,"y":64,"z":100,"world":"world"},"pos2":{"x":200,"y":64,"z":200,"world":"world"}},"material":"stone"}}]' | jq .

# Test 4: setBlocks - very large region (should hit limit)
echo -e "\n${GREEN}Test 4: setBlocks - very large region 300x300x1 = 90000 blocks (exceeds 50k limit)${NC}"
batch_call '[{"id":4,"name":"set_blocks","arguments":{"region":{"pos1":{"x":300,"y":64,"z":300,"world":"world"},"pos2":{"x":599,"y":64,"z":599,"world":"world"}},"material":"stone"}}]' | jq .

# Test 5: copy region
echo -e "\n${GREEN}Test 5: copy region (10x10x10 = 1000 blocks)${NC}"
batch_call '[{"id":5,"name":"copy","arguments":{"pos1":{"x":10,"y":64,"z":10,"world":"world"},"pos2":{"x":20,"y":74,"z":20,"world":"world"}}}]' | jq .

# Test 6: paste copied region
echo -e "\n${GREEN}Test 6: paste copied region${NC}"
batch_call '[{"id":6,"name":"paste","arguments":{"origin":{"x":30,"y":64,"z":30,"world":"world"},"rotation":0}}]' | jq .

# Test 7: Block with properties
echo -e "\n${GREEN}Test 7: Block with properties (oak_stairs[facing=north,half=top])${NC}"
batch_call '[{"id":7,"name":"set_blocks","arguments":{"region":{"pos1":{"x":40,"y":64,"z":40,"world":"world"},"pos2":{"x":40,"y":64,"z":40,"world":"world"}},"material":"oak_stairs[facing=north,half=top]"}}]' | jq .

# Test 8: replaceBlocks
echo -e "\n${GREEN}Test 8: replaceBlocks (stone -> dirt)${NC}"
batch_call '[{"id":8,"name":"replace_blocks","arguments":{"region":{"pos1":{"x":50,"y":64,"z":50,"world":"world"},"pos2":{"x":55,"y":64,"z":55,"world":"world"}},"from":"stone","to":"dirt"}}]' | jq .

# Test 9: sphere
echo -e "\n${GREEN}Test 9: sphere (radius 5, ~523 blocks)${NC}"
batch_call '[{"id":9,"name":"sphere","arguments":{"center":{"x":60,"y":70,"z":60,"world":"world"},"radius":5,"material":"glass"}}]' | jq .

# Test 10: cylinder
echo -e "\n${GREEN}Test 10: cylinder (radius 5, height 10, ~785 blocks)${NC}"
batch_call '[{"id":10,"name":"cylinder","arguments":{"center":{"x":70,"y":64,"z":70,"world":"world"},"radius":5,"height":10,"material":"oak_log"}}]' | jq .

# Test 11: batch blocks with door
echo -e "\n${GREEN}Test 11: batch_blocks with door (should place 2 blocks)${NC}"
batch_call '[{"id":11,"name":"batch_blocks","arguments":{"blocks":[{"pos":{"x":80,"y":64,"z":80,"world":"world"},"material":"oak_door"}]}}]' | jq .

# Test 12: copy large region (exceeds clipboard limit)
echo -e "\n${GREEN}Test 12: copy large region (50x50x50 = 125000 blocks, exceeds 50k limit)${NC}"
batch_call '[{"id":12,"name":"copy","arguments":{"pos1":{"x":100,"y":64,"z":100,"world":"world"},"pos2":{"x":150,"y":114,"z":150,"world":"world"}}}]' | jq .

# Test 13: paste with 90 degree rotation
echo -e "\n${GREEN}Test 13: paste with 90 degree rotation${NC}"
batch_call '[{"id":13,"name":"paste","arguments":{"origin":{"x":90,"y":64,"z":90,"world":"world"},"rotation":90}}]' | jq .

# Test 14: Test wall tool
echo -e "\n${GREEN}Test 14: walls (hollow box)${NC}"
batch_call '[{"id":14,"name":"walls","arguments":{"region":{"pos1":{"x":100,"y":64,"z":100,"world":"world"},"pos2":{"x":105,"y":68,"z":105,"world":"world"}},"material":"stone_bricks"}}]' | jq .

# Test 15: Test batch_blocks with multiple blocks
echo -e "\n${GREEN}Test 15: batch_blocks with multiple blocks${NC}"
batch_call '[{"id":15,"name":"batch_blocks","arguments":{"blocks":[{"pos":{"x":200,"y":64,"z":200,"world":"world"},"material":"diamond_block"},{"pos":{"x":201,"y":64,"z":200,"world":"world"},"material":"gold_block"},{"pos":{"x":202,"y":64,"z":200,"world":"world"},"material":"iron_block"}]}}]' | jq .

echo -e "\n${YELLOW}=========================================="
echo "Server logs (last 100 lines):"
echo "==========================================${NC}"
docker compose logs --tail=100 spigot

echo -e "\n${GREEN}Tests complete! Share this output for analysis.${NC}"