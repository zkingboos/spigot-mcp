package xyz.joseg.spigotmcp.mcp

import com.fasterxml.jackson.databind.ObjectMapper
import io.modelcontextprotocol.json.McpJsonMapper
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper
import io.modelcontextprotocol.server.McpServer
import io.modelcontextprotocol.server.McpAsyncServer
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider
import io.modelcontextprotocol.spec.McpSchema
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ConcurrentLinkedQueue
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import xyz.joseg.spigotmcp.mcp.tools.ToolRegistry
import xyz.joseg.spigotmcp.mcp.tools.ToolDefinition
import xyz.joseg.spigotmcp.mcp.tools.block.*
import xyz.joseg.spigotmcp.mcp.tools.clipboard.*
import xyz.joseg.spigotmcp.mcp.tools.player.*
import xyz.joseg.spigotmcp.mcp.tools.selection.*
import xyz.joseg.spigotmcp.mcp.tools.server.*

// HTTP API data classes (serializable for kotlinx.serialization)
@kotlinx.serialization.Serializable
data class ToolInfo(
    val name: String,
    val description: String,
    val inputSchema: JsonObject
)

@kotlinx.serialization.Serializable
data class JsonObject(
    val properties: Map<String, JsonSchemaProperty> = emptyMap(),
    val type: String = "object",
    val required: List<String> = emptyList(),
    val additionalProperties: Boolean = false
)

@kotlinx.serialization.Serializable
data class JsonSchemaProperty(
    val type: String = "string",
    val description: String = "",
    val items: JsonSchemaProperty? = null,
    val enum: List<String>? = null,
    val properties: Map<String, JsonSchemaProperty>? = null,
    val required: List<String>? = null,
    val additionalProperties: Boolean = false
)

// Batch API data classes
data class BatchToolCallRequest(
    val calls: List<BatchToolCall> = emptyList()
)

data class BatchToolCall(
    val id: Any = 0,
    val name: String = "",
    val arguments: Map<String, Any>? = null
)

data class BatchToolCallResponse(
    val results: List<Map<String, Any>> = emptyList()
)

// JSON-RPC data classes
data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val method: String,
    val params: Map<String, Any>? = null,
    val id: Any? = null
)

data class JsonRpcResponse(
    val jsonrpc: String = "2.0",
    val result: Any? = null,
    val error: JsonRpcError? = null,
    val id: Any? = null
) {
    companion object {
        fun success(id: Any?, result: Any): JsonRpcResponse {
            return JsonRpcResponse("2.0", result, null, id)
        }
        fun error(id: Any?, code: Int, message: String, data: Any? = null): JsonRpcResponse {
            return JsonRpcResponse("2.0", null, JsonRpcError(code, message, data), id)
        }
    }
}

data class JsonRpcError(
    val code: Int,
    val message: String,
    val data: Any? = null
)

data class JsonRpcNotification(
    val jsonrpc: String = "2.0",
    val method: String,
    val params: Map<String, Any>
)

class McpServerHost(
    private val config: xyz.joseg.spigotmcp.config.PluginConfig,
    private val faweAdapter: xyz.joseg.spigotmcp.fawe.FaweAdapter?,
    private val logger: java.util.logging.Logger
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var stdioServer: McpAsyncServer? = null
    private var stdioTransport: StdioServerTransportProvider? = null
    private var httpServer: McpAsyncServer? = null
    private var jettyServer: Server? = null
    private val sseConnections = ConcurrentHashMap<String, SseConnection>()
    private val httpServerRunning = AtomicBoolean(false)
    
    private val jsonMapper: McpJsonMapper by lazy {
        val objectMapper = ObjectMapper()
        objectMapper.findAndRegisterModules()
        JacksonMcpJsonMapper(objectMapper)
    }
    
    private val jacksonMapper = ObjectMapper().apply { findAndRegisterModules() }
    
    private val kotlinxJson = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    private val registeredTools = mutableListOf<ToolDefinition>()
    
    fun start() {
        scope.launch {
            // stdio transport (separate server instance)
            if (config.mcp.stdioEnabled) {
                startStdioServer()
            }
            
            // HTTP MCP transport (separate server instance)
            if (config.mcp.httpEnabled) {
                startHttpServer()
            }
        }
    }
    
    private fun startStdioServer() {
        stdioTransport = StdioServerTransportProvider(jsonMapper)
        val spec = McpServer.async(stdioTransport!!)
        configureSpec(spec)
        stdioServer = spec.build()
        registerToolsOnServer(stdioServer!!)
    }
    
    private fun startHttpServer() {
        if (httpServerRunning.getAndSet(true)) return
        
        scope.launch {
            // Create Jetty server for MCP servlet transport
            val jetty = Server(config.mcp.port)
            val context = ServletContextHandler(ServletContextHandler.SESSIONS)
            context.contextPath = "/"
            jetty.handler = context
            
            // MCP Streamable HTTP transport (POST /)
            val streamableProvider = HttpServletStreamableServerTransportProvider.builder()
                .jsonMapper(jsonMapper)
                .mcpEndpoint("/")
                .build()
            context.addServlet(ServletHolder(streamableProvider), "/")
            
            // MCP SSE transport (GET /sse)
            val sseProvider = HttpServletSseServerTransportProvider.builder()
                .jsonMapper(jsonMapper)
                .sseEndpoint("/sse")
                .messageEndpoint("/")
                .build()
            context.addServlet(ServletHolder(sseProvider), "/sse")
            
            // Create HTTP server with streamable transport
            val httpSpec = McpServer.async(streamableProvider)
            configureSpec(httpSpec)
            httpServer = httpSpec.build()
            registerToolsOnServer(httpServer!!)
            
            // REST API endpoints (health, tools list, etc.) - under /api to avoid conflict with MCP root
            context.addServlet(ServletHolder(RestApiServlet()), "/api/*")
            context.addServlet(ServletHolder(HealthServlet()), "/api/health")
            
            jettyServer = jetty
            jetty.start()
            
            logger.info("MCP HTTP server started on ${config.mcp.bindAddress}:${config.mcp.port}")
            logger.info("  POST / - MCP JSON-RPC endpoint (streamable)")
            logger.info("  GET  /sse - MCP SSE notifications endpoint")
            logger.info("  GET  /api/health - Health check")
            logger.info("  GET  /api/tools - List available tools (REST)")
        }
    }
    
    private fun convertInputSchema(inputSchema: Any?): JsonObject {
        if (inputSchema == null) return JsonObject()
        
        val jsonStr = inputSchema as? String ?: jacksonMapper.writeValueAsString(inputSchema)
        return try {
            val typeRef = object : com.fasterxml.jackson.core.type.TypeReference<Map<String, Any>>() {}
            val map = jacksonMapper.readValue(jsonStr, typeRef)
            JsonObject(
                type = map["type"] as? String ?: "object",
                required = (map["required"] as? List<String>) ?: emptyList(),
                additionalProperties = map["additionalProperties"] as? Boolean ?: false,
                properties = parseProperties(map["properties"] as? Map<String, Any> ?: emptyMap())
            )
        } catch (e: Exception) {
            JsonObject()
        }
    }
    
    private fun configureSpec(spec: McpServer.AsyncSpecification<*>) {
        val capabilities = McpSchema.ServerCapabilities.builder()
            .tools(true)
            .build()
        
        spec.serverInfo(McpSchema.Implementation("spigot-mcp", "1.0.0"))
            .capabilities(capabilities)
            .instructions("MCP Server for Spigot with FAWE integration")
            .jsonMapper(jsonMapper)
    }
    
    private fun registerToolsOnServer(server: McpAsyncServer) {
        // Register tools directly on the server instance
        // Block operation tools (require FAWE)
        faweAdapter?.let { adapter ->
            val tools = listOf(
                createSetBlocksTool(adapter),
                createReplaceBlocksTool(adapter),
                createWallsTool(adapter),
                createSphereTool(adapter),
                createCylinderTool(adapter),
                createBatchBlocksTool(adapter)
            )
            registeredTools.addAll(tools)
            tools.forEach { server.addTool(it.toAsyncTool()).block() }
        }
        
        // Clipboard tools (require FAWE)
        faweAdapter?.let { adapter ->
            val tools = listOf(
                createCopyTool(adapter),
                createPasteTool(adapter),
                createClearClipboardTool(adapter)
            )
            registeredTools.addAll(tools)
            tools.forEach { server.addTool(it.toAsyncTool()).block() }
        }
        
        // Selection tools (no FAWE required)
        val selectionTools = listOf(
            createGetSelectionTool(),
            createSetSelectionTool()
        )
        registeredTools.addAll(selectionTools)
        selectionTools.forEach { server.addTool(it.toAsyncTool()).block() }
        
        // Server management tools (no FAWE required)
        val serverTools = listOf(
            createRestartServerTool(config.server),
            createStopServerTool(config.server),
            createServerStatusTool()
        )
        registeredTools.addAll(serverTools)
        serverTools.forEach { server.addTool(it.toAsyncTool()).block() }
        
        // Player tools (no FAWE required)
        val playerTools = listOf(
            createGetPlayerPositionTool()
        )
        registeredTools.addAll(playerTools)
        playerTools.forEach { server.addTool(it.toAsyncTool()).block() }
    }
    
    fun broadcastNotification(method: String, params: Map<String, Any>) {
        val notification = JsonRpcNotification("2.0", method, params)
        val json = jacksonMapper.writeValueAsString(notification)
        
        val eventData = "event: notification\ndata: $json\n\n"
        
        for ((_, connection) in sseConnections) {
            connection.sendRaw(eventData)
        }
    }
    
    fun stop() {
        stdioServer?.closeGracefully()
        stdioTransport?.closeGracefully()
        httpServer?.closeGracefully()
        jettyServer?.stop()
        scope.coroutineContext[Job]?.cancel()
        httpServerRunning.set(false)
        sseConnections.clear()
    }
    
    private fun parseProperties(props: Map<String, Any>): Map<String, JsonSchemaProperty> {
        return props.mapValues { (_, value) ->
            val propMap = value as? Map<String, Any> ?: emptyMap()
            JsonSchemaProperty(
                type = propMap["type"] as? String ?: "string",
                description = propMap["description"] as? String ?: "",
                items = propMap["items"]?.let { (it as? Map<String, Any>)?.let { parseProperties(it) }?.values?.firstOrNull() },
                enum = propMap["enum"] as? List<String>,
                properties = (propMap["properties"] as? Map<String, Any>)?.let { parseProperties(it) },
                required = propMap["required"] as? List<String>,
                additionalProperties = propMap["additionalProperties"] as? Boolean ?: false
            )
        }
    }
    
    // Servlet for health check
    private inner class HealthServlet : HttpServlet() {
        override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
            resp.contentType = "text/plain"
            resp.writer.write("OK")
        }
    }
    
    // Servlet for REST API
    private inner class RestApiServlet : HttpServlet() {
        override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
            val path = req.pathInfo ?: ""
            when (path) {
                "/tools" -> {
                    resp.contentType = "application/json"
                    val tools = registeredTools.map { tool ->
                        ToolInfo(
                            name = tool.name,
                            description = tool.description,
                            inputSchema = convertInputSchema(tool.inputSchemaJson)
                        )
                    }
                    val toolsJson = jacksonMapper.writeValueAsString(tools)
                    resp.writer.write(toolsJson)
                }
                else -> {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND)
                }
            }
        }
        
        override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
            val path = req.pathInfo ?: ""
            when (path) {
                "/tools/call" -> {
                    resp.contentType = "application/json"
                    resp.status = HttpServletResponse.SC_NOT_IMPLEMENTED
                    val errorResponse = mapOf(
                        "error" to "Tool execution via HTTP not yet implemented. Use MCP protocol over /mcp endpoint.",
                        "mcpEndpoint" to "/mcp"
                    )
                    val json = jacksonMapper.writeValueAsString(errorResponse)
                    resp.writer.write(json)
                }
                "/tools/batch" -> {
                    resp.contentType = "application/json"
                    try {
                        val request = req.reader.readText()
                        val batchRequest = jacksonMapper.readValue(request, BatchToolCallRequest::class.java)
                        val results = mutableListOf<Any>()
                        
                        for (call in batchRequest.calls) {
                            val toolName = call.name
                            val arguments = call.arguments ?: emptyMap()
                            
                            // Use the existing handleCallTool logic
                            val result = try {
                                // Create a mock request for handleCallTool
                                val jsonRpcRequest = JsonRpcRequest(
                                    method = "tools/call",
                                    params = mapOf("name" to toolName, "arguments" to arguments),
                                    id = call.id
                                )
                                handleCallToolInternal(jsonRpcRequest)
                            } catch (e: Exception) {
                                mapOf(
                                    "id" to call.id,
                                    "error" to mapOf(
                                        "code" to -32603,
                                        "message" to "Tool execution failed: $toolName - ${e.message}"
                                    )
                                )
                            }
                            
                            results.add(result)
                        }
                        
                        val response = mapOf("results" to results)
                        resp.writer.write(jacksonMapper.writeValueAsString(response))
                    } catch (e: Exception) {
                        resp.status = HttpServletResponse.SC_BAD_REQUEST
                        val errorResponse = mapOf("error" to "Invalid batch request: ${e.message}")
                        resp.writer.write(jacksonMapper.writeValueAsString(errorResponse))
                    }
                }
                else -> {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND)
                }
            }
        }
    }
    
    private fun handleCallToolInternal(request: JsonRpcRequest): Map<String, Any> {
        val params = request.params ?: emptyMap()
        val toolName = params["name"] as? String ?: ""
        val toolArgs = params["arguments"] as? Map<String, Any> ?: emptyMap()
        val callId = request.id ?: ""
        
        var errorMessage: String? = null
        val result = try {
            registeredTools.find { it.name == toolName }?.execute(toolArgs)
        } catch (e: Exception) {
            errorMessage = e.message
            null
        }
        
        return if (result != null) {
            mapOf(
                "id" to callId,
                "result" to result
            )
        } else {
            val detail = errorMessage?.let { " - $it" } ?: ""
            mapOf(
                "id" to callId,
                "error" to mapOf(
                    "code" to -32603,
                    "message" to "Tool execution failed: $toolName$detail"
                )
            )
        }
    }
    
    // JSON-RPC notification for SSE
    private data class JsonRpcNotification(
        val jsonrpc: String = "2.0",
        val method: String,
        val params: Map<String, Any>
    )
    
    // SSE connection for notifications
    private inner class SseConnection(
        val sessionId: String,
        private val json: Json
    ) {
        private val queue = ConcurrentLinkedQueue<String>()
        @Volatile var closed = false
        
        fun send(event: String, data: Any) {
            if (closed) return
            val jsonStr = jacksonMapper.writeValueAsString(data)
            queue.add("event: $event\ndata: $jsonStr\n\n")
        }
        
        fun sendRaw(eventData: String) {
            if (closed) return
            queue.add(eventData)
        }
        
        fun poll(): String? = queue.poll()
        
        fun close() {
            closed = true
        }
    }
}