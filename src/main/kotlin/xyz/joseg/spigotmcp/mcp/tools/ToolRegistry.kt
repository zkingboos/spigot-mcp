package xyz.joseg.spigotmcp.mcp.tools

import io.modelcontextprotocol.server.McpAsyncServer
import reactor.core.publisher.Mono

class ToolRegistry(private val server: McpAsyncServer) {
    
    fun register(tool: ToolDefinition) {
        server.addTool(tool.toAsyncTool()).block()
    }
    
    fun registerAll(vararg tools: ToolDefinition) {
        tools.forEach { register(it) }
    }
}