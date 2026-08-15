package xyz.joseg.spigotmcp.mcp.transport

import com.fasterxml.jackson.databind.ObjectMapper
import io.modelcontextprotocol.json.McpJsonMapper
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper
import io.modelcontextprotocol.server.McpServer
import io.modelcontextprotocol.server.McpAsyncServer
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StdioTransport(
    private val serverFactory: () -> McpAsyncServer,
    private val scope: CoroutineScope
) {
    private val jsonMapper: io.modelcontextprotocol.json.McpJsonMapper by lazy {
        val objectMapper = ObjectMapper()
        objectMapper.findAndRegisterModules()
        io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper(objectMapper)
    }
    
    fun start() {
        scope.launch(Dispatchers.IO) {
            val provider = StdioServerTransportProvider(jsonMapper)
            
            // Create async server spec and build
            val spec = McpServer.async(provider)
            val server = serverFactory()
            
            // The server starts automatically when built
            // Keep reference to prevent GC
        }
    }
}