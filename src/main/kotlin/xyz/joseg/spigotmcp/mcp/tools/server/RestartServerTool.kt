package xyz.joseg.spigotmcp.mcp.tools.server

import io.modelcontextprotocol.spec.McpSchema
import xyz.joseg.spigotmcp.config.ServerConfig
import xyz.joseg.spigotmcp.mcp.tools.ToolDefinition
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable

fun createRestartServerTool(config: ServerConfig): ToolDefinition {
    return ToolDefinition(
        name = "restart_server",
        description = "Restart the Spigot server",
        inputSchemaJson = """
            {
                "type": "object",
                "properties": {
                    "delay": {"type": "integer", "default": 5, "description": "Delay in seconds before restart"}
                }
            }
        """
    ) { args ->
        val delay = (args["delay"] as? Int) ?: config.restartDelay
        val plugin = Bukkit.getPluginManager().getPlugin("spigot-mcp")!!
        val task = object : BukkitRunnable() {
            override fun run() {
                Bukkit.spigot().restart()
            }
        }
        task.runTaskLater(plugin, (delay * 20L).toLong())
        
        McpSchema.CallToolResult(
            listOf(McpSchema.TextContent("Server restart scheduled in $delay seconds")),
            false
        )
    }
}