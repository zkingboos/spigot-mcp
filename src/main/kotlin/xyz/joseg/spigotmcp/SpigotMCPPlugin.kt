package xyz.joseg.spigotmcp

import org.bukkit.plugin.java.JavaPlugin
import xyz.joseg.spigotmcp.config.ConfigLoader
import xyz.joseg.spigotmcp.config.PluginConfig
import xyz.joseg.spigotmcp.fawe.FaweAdapter
import xyz.joseg.spigotmcp.mcp.McpServerHost

class SpigotMCPPlugin : JavaPlugin() {
    
    companion object {
        lateinit var instance: SpigotMCPPlugin
            private set
    }
    
    private lateinit var config: PluginConfig
    private var faweAdapter: FaweAdapter? = null
    private lateinit var mcpServer: McpServerHost
    
    override fun onEnable() {
        instance = this
        saveDefaultConfig()
        config = ConfigLoader.load(dataFolder)
        
        // Try to initialize FAWE adapter if FAWE is available
        val fawePlugin = server.pluginManager.getPlugin("FastAsyncWorldEdit")
        if (fawePlugin != null) {
            faweAdapter = FaweAdapter(config.fawe)
            logger.info("FAWE integration enabled")
        } else {
            logger.warning("FAWE not found - FAWE tools will not be available")
        }
        
        mcpServer = McpServerHost(config, faweAdapter, logger)
        mcpServer.start()
        
        logger.info("SpigotMCP enabled - MCP server started on stdio:${config.mcp.stdioEnabled} http:${config.mcp.httpEnabled}:${config.mcp.port}")
    }
    
    override fun onDisable() {
        mcpServer.stop()
        logger.info("SpigotMCP disabled")
    }
}