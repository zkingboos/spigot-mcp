package xyz.joseg.spigotmcp

import org.bukkit.plugin.java.JavaPlugin
import xyz.joseg.spigotmcp.config.ConfigLoader
import xyz.joseg.spigotmcp.config.PluginConfig
import xyz.joseg.spigotmcp.fawe.FaweAdapter
import xyz.joseg.spigotmcp.mcp.McpServerHost

class SpigotMCPPlugin : JavaPlugin() {
    
    private lateinit var config: PluginConfig
    private lateinit var faweAdapter: FaweAdapter
    private lateinit var mcpServer: McpServerHost
    
    override fun onEnable() {
        saveDefaultConfig()
        config = ConfigLoader.load(dataFolder)
        
        faweAdapter = FaweAdapter(config.fawe)
        mcpServer = McpServerHost(config, faweAdapter)
        
        mcpServer.start()
        
        logger.info("SpigotMCP enabled - MCP server started on stdio:${config.mcp.stdioEnabled} http:${config.mcp.httpEnabled}:${config.mcp.port}")
    }
    
    override fun onDisable() {
        mcpServer.stop()
        logger.info("SpigotMCP disabled")
    }
}