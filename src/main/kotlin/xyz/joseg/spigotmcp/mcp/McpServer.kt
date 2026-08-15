package xyz.joseg.spigotmcp.mcp

import com.fasterxml.jackson.databind.ObjectMapper
import io.modelcontextprotocol.json.McpJsonMapper
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper
import io.modelcontextprotocol.server.McpServer
import io.modelcontextprotocol.server.McpAsyncServer
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider
import io.modelcontextprotocol.spec.McpSchema
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Duration

class McpServerHost(
    private val config: xyz.joseg.spigotmcp.config.PluginConfig,
    private val faweAdapter: xyz.joseg.spigotmcp.fawe.FaweAdapter
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var asyncServer: McpAsyncServer? = null
    private var stdioTransport: io.modelcontextprotocol.server.transport.StdioServerTransportProvider? = null
    
    private val jsonMapper: McpJsonMapper by lazy {
        val objectMapper = ObjectMapper()
        objectMapper.findAndRegisterModules()
        JacksonMcpJsonMapper(objectMapper)
    }
    
    fun start() {
        scope.launch {
            // stdio transport
            if (config.mcp.stdioEnabled) {
                stdioTransport = io.modelcontextprotocol.server.transport.StdioServerTransportProvider(jsonMapper)
                val spec = McpServer.async(stdioTransport!!)
                configureSpec(spec)
                asyncServer = spec.build()
            }
            
            // HTTP transport - simplified for now
            if (config.mcp.httpEnabled) {
                startHttpServer()
            }
        }
    }
    
    private fun configureSpec(spec: io.modelcontextprotocol.server.McpServer.AsyncSpecification<*>) {
        val capabilities = McpSchema.ServerCapabilities(
            null, null, null, null, null, null
        )
        
        spec.serverInfo(McpSchema.Implementation("spigot-mcp", "1.0.0"))
            .capabilities(capabilities)
            .instructions("MCP Server for Spigot with FAWE integration")
            .jsonMapper(jsonMapper)
        
        // Register tools
        registerTools(spec)
    }
    
    private fun registerTools(spec: io.modelcontextprotocol.server.McpServer.AsyncSpecification<*>) {
        // TODO: Register FAWE tools
    }
    
    private fun startHttpServer() {
        // TODO: Implement HTTP transport
    }
    
    fun stop() {
        asyncServer?.closeGracefully()
        stdioTransport?.closeGracefully()
        scope.coroutineContext[Job]?.cancel()
    }
}