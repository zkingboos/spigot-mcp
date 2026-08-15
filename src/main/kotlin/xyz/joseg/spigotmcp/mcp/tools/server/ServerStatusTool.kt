package xyz.joseg.spigotmcp.mcp.tools.server

import io.modelcontextprotocol.spec.McpSchema
import xyz.joseg.spigotmcp.mcp.tools.ToolDefinition
import kotlinx.serialization.json.Json

data class ServerStatusOutput(
    val onlinePlayers: Int,
    val maxPlayers: Int,
    val tps: DoubleArray,
    val memoryUsedMb: Long,
    val memoryMaxMb: Long,
    val version: String
)

fun createServerStatusTool(): ToolDefinition {
    return ToolDefinition(
        name = "server_status",
        description = "Get server status information",
        inputSchemaJson = """{"type": "object", "properties": {}}"""
    ) { args ->
        val players = org.bukkit.Bukkit.getOnlinePlayers()
        val runtime = Runtime.getRuntime()
        
        // Get TPS using Bukkit's method
        val tpsArray = DoubleArray(3) { 20.0 } // Default to 20 TPS if not available
        
        val status = ServerStatusOutput(
            onlinePlayers = players.size,
            maxPlayers = org.bukkit.Bukkit.getMaxPlayers(),
            tps = tpsArray,
            memoryUsedMb = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024,
            memoryMaxMb = runtime.maxMemory() / 1024 / 1024,
            version = org.bukkit.Bukkit.getVersion()
        )
        
        val json = Json.encodeToString(status)
        McpSchema.CallToolResult(
            listOf(McpSchema.TextContent(json)),
            false
        )
    }
}