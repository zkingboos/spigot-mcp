package xyz.joseg.spigotmcp

import org.bukkit.plugin.java.JavaPlugin
import xyz.joseg.spigotmcp.config.ConfigLoader
import xyz.joseg.spigotmcp.config.PluginConfig
import xyz.joseg.spigotmcp.mcp.McpServerHost
import xyz.joseg.spigotmcp.worldedit.WorldEditBackends
import xyz.joseg.spigotmcp.worldedit.WorldEditService

class SpigotMCPPlugin : JavaPlugin() {

    companion object {
        lateinit var instance: SpigotMCPPlugin
            private set
    }

    private lateinit var pluginConfig: PluginConfig
    private var worldEdit: WorldEditService? = null
    private lateinit var mcpServer: McpServerHost

    override fun onEnable() {
        instance = this
        saveDefaultConfig()
        pluginConfig = ConfigLoader.load(dataFolder)

        worldEdit = WorldEditBackends.detect(pluginConfig.worldEdit, logger)
            ?.let { WorldEditService(it, pluginConfig.worldEdit) }

        mcpServer = McpServerHost(pluginConfig, worldEdit, logger)
        mcpServer.start()

        logger.info(
            "SpigotMCP enabled - MCP server started on " +
                "stdio:${pluginConfig.mcp.stdioEnabled} http:${pluginConfig.mcp.httpEnabled}:${pluginConfig.mcp.port}"
        )
    }

    override fun onDisable() {
        if (::mcpServer.isInitialized) {
            mcpServer.stop()
        }
        logger.info("SpigotMCP disabled")
    }
}
