# Spigot MCP

MCP (Model Context Protocol) server for Spigot Minecraft servers with FastAsyncWorldEdit (FAWE) integration. Exposes block editing, clipboard, selection, server management, and player tools to MCP clients (LLM agents).

## Features

- **Block tools**: fill regions, replace materials, build walls, spheres, cylinders
- **Batch placement**: place multiple blocks with different materials in one call (supports facing, doors, glass panes, double chests)
- **Clipboard**: copy, paste, clear
- **Selection**: get/set WorldEdit selection points
- **Server management**: status, stop, restart
- **Player tools**: get player positions
- **Transports**: stdio and HTTP (streamable + SSE), plus a REST API for tool listing

## Requirements

- Java 21+
- Spigot/Paper 1.20+
- FastAsyncWorldEdit (soft dependency)

## Build

```bash
./gradlew build
```

Plugin JAR: `build/libs/spigot-mcp-1.0-SNAPSHOT.jar`

## Installation

1. Drop the JAR into your server's `plugins/` folder
2. Install FastAsyncWorldEdit (optional but recommended)
3. Restart the server

## Configuration

Create `plugins/spigot-mcp/config.yml` to override defaults:

```yaml
mcp:
  port: 8080
  bind-address: "0.0.0.0"
  stdio-enabled: true
  http-enabled: true
  auth:
    enabled: true
    token: "changeme-in-production"
    env-var: "MCP_AUTH_TOKEN"

fawe:
  max-blocks-per-op: 50000
  require-selection: true
  async: true

server:
  restart-delay: 5
  stop-delay: 3
```

## HTTP Endpoints

| Endpoint | Description |
|----------|-------------|
| `POST /` | MCP JSON-RPC (streamable) |
| `GET /sse` | MCP SSE notifications |
| `GET /api/health` | Health check |
| `GET /api/tools` | List available tools |

## Docker

```bash
docker-compose up -d
```

Runs a Spigot 1.21.4 server with FAWE and the plugin pre-loaded (data in `./spigot-data`).

## Connecting AI Clients

The plugin runs inside the Spigot server, so AI clients connect over HTTP to `http://<server-ip>:8080/` (or via SSE at `/sse`). If authentication is enabled, pass the token as a Bearer header.

### Claude Code

```bash
claude mcp add spigot-mcp --transport http http://localhost:8080/ \
  --header "Authorization: Bearer MCP_AUTH_TOKEN"
```

Or in `~/.claude.json`:

```json
{
  "mcpServers": {
    "spigot-mcp": {
      "type": "http",
      "url": "http://localhost:8080/",
      "headers": { "Authorization": "Bearer MCP_AUTH_TOKEN" }
    }
  }
}
```

### Codex

```bash
codex mcp add spigot-mcp --url http://localhost:8080/ \
  --header "Authorization: Bearer MCP_AUTH_TOKEN"
```

### OpenCode

Add to `opencode.json`:

```json
{
  "mcp": {
    "spigot-mcp": {
      "type": "http",
      "url": "http://localhost:8080/",
      "headers": { "Authorization": "Bearer MCP_AUTH_TOKEN" }
    }
  }
}
```

Once connected, the agent can call tools like `set_blocks`, `batch_blocks`, `sphere`, or `get_player_position` directly.

## Testing

```bash
./gradlew test    # Run tests
./run-test.sh     # Build + start server via docker-compose
./test-mcp-stdio.sh  # Smoke-test the MCP server over stdio
```