# MCP Server with FAWE Integration - Design Spec

## Overview
Embed an MCP (Model Context Protocol) server inside a Spigot plugin to expose FAWE (FastAsyncWorldEdit) block manipulation operations and basic server management as MCP tools.

## Architecture

```
spigot-mcp (Kotlin/Gradle Plugin)
├── MCP Server (embedded, official Kotlin SDK)
│   ├── stdio Transport (local LLM integration)
│   ├── HTTP/SSE Transport (Netty, port 8080 default)
│   ├── Auth Middleware (Bearer token)
│   └── Tool Registry + Dispatcher
├── FAWE Adapter (programmatic API)
│   ├── Block Operations: set, replace, walls, sphere, cylinder
│   ├── Clipboard: copy, paste, clear
│   ├── Selection: get/set pos1/pos2
│   └── (Phase 2) Schematics: load, save, paste
├── Server Management
│   ├── restart_server()
│   ├── stop_server()
│   └── server_status()
└── Configuration (config.yml)
```

## MCP Protocol Support
- **Transports**: stdio + HTTP/SSE (both enabled by default)
- **Auth**: Bearer token via `Authorization` header (HTTP) or env var (stdio)
- **Protocol Version**: 2024-11-05 (current MCP spec)

## Tool Definitions (Phase 1)

### Block Operations
| Tool | Parameters | Description |
|------|------------|-------------|
| `set_blocks` | `material: string`, `pos1: Pos`, `pos2: Pos` | Fill region with material |
| `replace_blocks` | `from: string`, `to: string`, `pos1: Pos`, `pos2: Pos` | Replace material in region |
| `walls` | `material: string`, `pos1: Pos`, `pos2: Pos` | Build walls around region |
| `sphere` | `material: string`, `center: Pos`, `radius: int` | Create sphere |
| `cylinder` | `material: string`, `center: Pos`, `radius: int`, `height: int` | Create cylinder |

### Clipboard
| Tool | Parameters | Description |
|------|------------|-------------|
| `copy` | `pos1: Pos`, `pos2: Pos` | Copy region to clipboard |
| `paste` | `origin: Pos`, `rotate?: int` | Paste clipboard at origin |
| `clear_clipboard` | (none) | Clear clipboard |

### Selection
| Tool | Parameters | Description |
|------|------------|-------------|
| `get_selection` | (none) | Get current pos1/pos2 |
| `set_selection` | `pos1: Pos`, `pos2: Pos` | Set selection points |

### Server Management
| Tool | Parameters | Description |
|------|------------|-------------|
| `restart_server` | `delay?: int` | Restart Spigot (Bukkit restart) |
| `stop_server` | `delay?: int` | Stop Spigot gracefully |
| `server_status` | (none) | Get online players, TPS, memory |

**Pos type**: `{ x: number, y: number, z: number, world: string }`

## Authentication
- Configurable token in `config.yml` (plain text, env var override supported)
- Required for all tool calls
- HTTP: `Authorization: Bearer <token>`
- stdio: `MCP_AUTH_TOKEN` env var

## Configuration (config.yml)
```yaml
mcp:
  port: 8080
  stdio-enabled: true
  http-enabled: true
  auth:
    enabled: true
    token: "changeme-in-production"
    env-var: "MCP_AUTH_TOKEN"  # overrides config token
fawe:
  max-blocks-per-op: 50000
  require-selection: true
  async: true
server:
  restart-delay: 5
  stop-delay: 3
```

## Safety & Limits
- **Max blocks per operation**: 50,000 (configurable)
- **Require selection**: Most ops need valid pos1/pos2
- **Async execution**: FAWE ops run async via `FaweAPI`
- **Permission check**: Optional Bukkit permission `spigotmcp.mcp.use`

## Error Handling
- MCP error codes: `-32600` (invalid request), `-32601` (method not found), `-32602` (invalid params), `-32603` (internal error)
- FAWE errors → MCP internal error with message
- Auth failures → `-32001` (unauthorized)

## Dependencies (Add to build.gradle.kts)
```kotlin
implementation("io.modelcontextprotocol:mcp:0.6.0")  // Kotlin MCP SDK
implementation("io.netty:netty-transport-native-epoll:4.1.100.Final")  // Linux epoll
implementation("io.netty:netty-codec-http:4.1.100.Final")
```

## File Structure (New)
```
src/main/kotlin/xyz/joseg/spigotmcp/
├── SpigotMCPPlugin.kt           # Main plugin class
├── mcp/
│   ├── McpServer.kt             # Embedded MCP server lifecycle
│   ├── transport/
│   │   ├── StdioTransport.kt
│   │   └── HttpSseTransport.kt
│   ├── auth/
│   │   └── AuthMiddleware.kt
│   └── tools/
│       ├── ToolRegistry.kt
│       ├── block/
│       │   ├── SetBlocksTool.kt
│       │   ├── ReplaceBlocksTool.kt
│       │   ├── WallsTool.kt
│       │   ├── SphereTool.kt
│       │   └── CylinderTool.kt
│       ├── clipboard/
│       │   ├── CopyTool.kt
│       │   ├── PasteTool.kt
│       │   └── ClearClipboardTool.kt
│       ├── selection/
│       │   ├── GetSelectionTool.kt
│       │   └── SetSelectionTool.kt
│       └── server/
│           ├── RestartServerTool.kt
│           ├── StopServerTool.kt
│           └── ServerStatusTool.kt
├── fawe/
│   └── FaweAdapter.kt           # FAWE programmatic wrapper
├── config/
│   └── Config.kt                # Config.yml binding
└── util/
    └── Position.kt              # Pos data class
```

## Testing Strategy
- Unit tests for `FaweAdapter` (mock FAWE API)
- Integration test: start plugin, call MCP tools via stdio
- Manual test: HTTP/SSE with curl/Postman

## Out of Scope (Phase 2+)
- Schematic load/save/paste
- Biome operations
- Chunk tools
- Player-specific operations
- Web UI for tool testing

## Acceptance Criteria
1. Plugin loads on Spigot 1.20.2 with FAWE installed
2. MCP server starts on both stdio and HTTP (port 8080)
3. Auth rejects unauthorized requests
4. `set_blocks` + `replace_blocks` work via MCP
5. `restart_server` / `stop_server` work via MCP
6. Configurable via `config.yml` with hot-reload (optional)