package xyz.joseg.spigotmcp.mcp.tools.server

import io.modelcontextprotocol.spec.McpSchema
import xyz.joseg.spigotmcp.config.ServerConfig
import xyz.joseg.spigotmcp.mcp.tools.ToolDefinition
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable

fun createStopServerTool(config: ServerConfig): ToolDefinition {
    return ToolDefinition(
        name = "stop_server",
        description = "Stop the Spigot server gracefully",
        inputSchemaJson = """
            {
                "type": "object",
                "properties": {
                    "delay": {"type": "integer", "default": 3, "description": "Delay in seconds before stop"}
                }
            }
        """
    ) { args ->
        val delay = (args["delay"] as? Int) ?: config.stopDelay
        val plugin = Bukkit.getPluginManager().getPlugin("spigot-mcp")!!
        val task = object : BukkitRunnable() {
            override fun run() {
                Bukkit.shutdown()
            }
        }
        task.runTaskLater(plugin, (delay * 20L).toLong())
        
        McpSchema.CallToolResult(
            listOf(McpSchema.TextContent("Server shutdown scheduled in $delay seconds")),
            false
        )
    }
}