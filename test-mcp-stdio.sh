#!/bin/bash
# Test MCP tools via stdio

echo "Testing MCP server via stdio..."
echo '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}' | java -jar build/libs/spigot-mcp-1.0-SNAPSHOT.jar