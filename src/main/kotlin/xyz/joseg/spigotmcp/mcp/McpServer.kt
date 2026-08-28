package xyz.joseg.spigotmcp.mcp

import com.sun.net.httpserver.HttpServer
import xyz.joseg.spigotmcp.config.PluginConfig
import xyz.joseg.spigotmcp.mcp.protocol.MiniMcpServer
import xyz.joseg.spigotmcp.mcp.protocol.startMcpHttp
import xyz.joseg.spigotmcp.mcp.protocol.startMcpStdio
import xyz.joseg.spigotmcp.mcp.tools.ToolDefinition
import xyz.joseg.spigotmcp.mcp.tools.block.createBatchBlocksTool
import xyz.joseg.spigotmcp.mcp.tools.block.createCylinderTool
import xyz.joseg.spigotmcp.mcp.tools.block.createReplaceBlocksTool
import xyz.joseg.spigotmcp.mcp.tools.block.createSetBlocksTool
import xyz.joseg.spigotmcp.mcp.tools.block.createSphereTool
import xyz.joseg.spigotmcp.mcp.tools.block.createWallsTool
import xyz.joseg.spigotmcp.mcp.tools.clipboard.createClearClipboardTool
import xyz.joseg.spigotmcp.mcp.tools.clipboard.createCopyTool
import xyz.joseg.spigotmcp.mcp.tools.clipboard.createPasteTool
import xyz.joseg.spigotmcp.mcp.tools.player.createGetPlayerPositionTool
import xyz.joseg.spigotmcp.mcp.tools.selection.createGetSelectionTool
import xyz.joseg.spigotmcp.mcp.tools.selection.createSetSelectionTool
import xyz.joseg.spigotmcp.mcp.tools.server.createServerStatusTool
import xyz.joseg.spigotmcp.mcp.tools.server.createRestartServerTool
import xyz.joseg.spigotmcp.mcp.tools.server.createStopServerTool
import xyz.joseg.spigotmcp.worldedit.WorldEditService
import java.util.logging.Logger

class McpServerHost(
    private val config: PluginConfig,
    private val worldEdit: WorldEditService?,
    private val logger: Logger
) {
    private val mcp = MiniMcpServer(name = "spigot-mcp", version = "1.0.0", logger = logger)
    private var httpServer: HttpServer? = null
    private var stdioThread: Thread? = null

    fun start() {
        registerTools()

        if (config.mcp.stdioEnabled) {
            stdioThread = startMcpStdio(mcp, logger)
        }
        if (config.mcp.httpEnabled) {
            httpServer = startMcpHttp(mcp, config.mcp.bindAddress, config.mcp.port, logger)
        }

        logger.info(
            "SpigotMCP MCP layer ready - tools registered: ${mcp.registeredTools().size}, " +
                "stdio:${config.mcp.stdioEnabled} http:${config.mcp.httpEnabled}:${config.mcp.port}"
        )
    }

    fun stop() {
        httpServer?.stop(0)
        stdioThread?.interrupt()
    }

    private fun registerTools() {
        // Registered once, shared by every transport - no per-server duplication.
        val blockAndClipboard = worldEdit?.let { we ->
            listOf(
                createSetBlocksTool(we),
                createReplaceBlocksTool(we),
                createWallsTool(we),
                createSphereTool(we),
                createCylinderTool(we),
                createBatchBlocksTool(we),
                createCopyTool(we),
                createPasteTool(we),
                createClearClipboardTool(we)
            )
        } ?: emptyList()

        val alwaysAvailable = listOf(
            createGetSelectionTool(),
            createSetSelectionTool(),
            createRestartServerTool(config.server),
            createStopServerTool(config.server),
            createServerStatusTool(),
            createGetPlayerPositionTool()
        )

        (blockAndClipboard + alwaysAvailable).forEach { def: ToolDefinition -> mcp.register(def) }
    }
}
