package xyz.joseg.spigotmcp.mcp.tools.server

import xyz.joseg.spigotmcp.mcp.protocol.CallToolResult
import xyz.joseg.spigotmcp.mcp.protocol.TextContent

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
        
        CallToolResult(
            listOf(TextContent("Server restart scheduled in $delay seconds")),
            false
        )
    }
}