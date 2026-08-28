package xyz.joseg.spigotmcp.mcp.protocol

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import xyz.joseg.spigotmcp.mcp.tools.ToolDefinition
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.logging.Logger

data class TextContent(val text: String)

data class CallToolResult(val content: List<TextContent>, val isError: Boolean = false)

data class McpResponse(val json: String, val sessionId: String? = null)

/**
 * Minimal tools-only MCP server core (JSON-RPC 2.0 over stdio and plain HTTP).
 * Replaces the MCP Java SDK + Jetty so the shaded jar stays loadable on JVM 8+
 * servers (vanilla MC 1.8.8). Stateless-friendly: sessions are optional.
 */
class MiniMcpServer(
    private val name: String,
    private val version: String,
    private val logger: Logger
) {
    private val mapper = ObjectMapper().apply { findAndRegisterModules() }
    private val tools = LinkedHashMap<String, ToolDefinition>()
    private val sessions = ConcurrentHashMap.newKeySet<String>()

    fun register(def: ToolDefinition) {
        tools[def.name] = def
    }

    fun registeredTools(): List<ToolDefinition> = tools.values.toList()

    /** JSON array with name/description/inputSchema of every tool - used by the REST listing. */
    fun restToolsJson(): String {
        val arr = mapper.createArrayNode()
        tools.values.forEach { def ->
            arr.addObject().apply {
                put("name", def.name)
                put("description", def.description)
                set<JsonNode>("inputSchema", mapper.readTree(def.inputSchemaJson))
            }
        }
        return mapper.writeValueAsString(arr)
    }

    fun toolsListJson(): JsonNode {
        val arr = mapper.createArrayNode()
        tools.values.forEach { def ->
            arr.addObject().apply {
                put("name", def.name)
                put("description", def.description)
                set<JsonNode>("inputSchema", mapper.readTree(def.inputSchemaJson))
            }
        }
        return mapper.createObjectNode().set<JsonNode>("tools", arr)
    }

    /** Handles one JSON-RPC message. Returns the response, or null for notifications. */
    fun handle(body: String): McpResponse? {
        val root: JsonNode = try {
            mapper.readTree(body)
        } catch (_: Exception) {
            return errorResponse(null, -32700, "Parse error")
        }
        if (!root.isObject) return errorResponse(null, -32600, "Invalid Request")

        val idNode = root.get("id")
        if (idNode == null || idNode.isNull) return null // notification: nothing to send

        val method = root.path("method").asText("")
        return try {
            when (method) {
                "initialize" -> initialize(idNode, root.path("params").path("protocolVersion").asText(""))
                "ping" -> resultResponse(idNode, mapper.createObjectNode())
                "tools/list" -> resultResponse(idNode, toolsListJson())
                "tools/call" -> callTool(idNode, root.path("params"))
                else -> errorResponse(idNode, -32601, "Method not found: $method")
            }
        } catch (t: Throwable) {
            logger.warning("MCP handler failure ($method): $t")
            errorResponse(idNode, -32603, "Internal error: ${t.message ?: t::class.java.simpleName}")
        }
    }

    private fun initialize(idNode: JsonNode, requestedProtocol: String): McpResponse {
        // Echo the client's protocol version when given; otherwise latest we speak.
        val proto = requestedProtocol.ifBlank { PROTOCOL_VERSION }
        val sessionId = UUID.randomUUID().toString()
        sessions.add(sessionId)
        val result = mapper.createObjectNode().apply {
            put("protocolVersion", proto)
            putObject("capabilities").putObject("tools").put("listChanged", false)
            putObject("serverInfo").apply {
                put("name", name)
                put("version", version)
            }
        }
        return McpResponse(resultResponse(idNode, result).json, sessionId)
    }

    private fun callTool(idNode: JsonNode, params: JsonNode): McpResponse {
        val name = params.path("name").asText("")
        val tool = tools[name]
            ?: return errorResponse(idNode, -32602, "Unknown tool: $name")

        val argsNode = params.path("arguments")
        val args: Map<String, Any> =
            if (argsNode.isObject) mapper.convertValue(argsNode, object : TypeReference<Map<String, Any>>() {})
            else emptyMap()

        // Tool failures become structured isError results, never thrown past this point.
        val result = try {
            tool.execute(args)
        } catch (t: Throwable) {
            CallToolResult(listOf(TextContent(t.message ?: t.toString())), isError = true)
        }

        val payload = mapper.createObjectNode().apply {
            val contentArr = putArray("content")
            result.content.forEach { c ->
                contentArr.addObject().apply {
                    put("type", "text")
                    put("text", c.text)
                }
            }
            put("isError", result.isError)
        }
        return resultResponse(idNode, payload)
    }

    private fun resultResponse(idNode: JsonNode, result: JsonNode): McpResponse =
        McpResponse(mapper.writeValueAsString(mapper.createObjectNode().apply {
            put("jsonrpc", "2.0")
            set<JsonNode>("id", idNode)
            set<JsonNode>("result", result)
        }))

    private fun errorResponse(idNode: JsonNode?, code: Int, message: String): McpResponse =
        McpResponse(mapper.writeValueAsString(mapper.createObjectNode().apply {
            put("jsonrpc", "2.0")
            if (idNode != null) set<JsonNode>("id", idNode) else putNull("id")
            putObject("error").apply {
                put("code", code)
                put("message", message)
            }
        }))

    private companion object {
        const val PROTOCOL_VERSION = "2025-03-26"
    }
}

/** Plain-JDK HTTP transport (com.sun.net.httpserver) - keeps the jar JVM 8 compatible. */
fun startMcpHttp(server: MiniMcpServer, bindAddress: String, port: Int, logger: Logger): HttpServer {
    val http = HttpServer.create(InetSocketAddress(bindAddress, port), 0)
    http.executor = Executors.newCachedThreadPool()

    http.createContext("/") { exchange ->
        try {
            when (exchange.requestMethod.uppercase()) {
                "POST" -> {
                    val body = exchange.requestBody.readBytes().toString(Charsets.UTF_8)
                    val response = server.handle(body)
                    if (response == null) {
                        exchange.sendResponseHeaders(202, -1) // notification accepted
                    } else {
                        val bytes = response.json.toByteArray(Charsets.UTF_8)
                        exchange.responseHeaders.add("Content-Type", "application/json")
                        val sid = response.sessionId ?: exchange.requestHeaders.getFirst("Mcp-Session-Id")
                        if (!sid.isNullOrBlank()) exchange.responseHeaders.add("Mcp-Session-Id", sid)
                        exchange.sendResponseHeaders(200, bytes.size.toLong())
                        exchange.responseBody.use { it.write(bytes) }
                    }
                }
                else -> exchange.sendResponseHeaders(405, -1)
            }
        } catch (t: Throwable) {
            logger.warning("MCP HTTP transport error: $t")
            runCatching { exchange.sendResponseHeaders(500, -1) }
        } finally {
            exchange.close()
        }
    }

    http.createContext("/api/health") { exchange ->
        try {
            val bytes = "OK".toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "text/plain")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        } finally {
            exchange.close()
        }
    }

    http.createContext("/api/tools") { exchange ->
        try {
            val bytes = server.restToolsJson().toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        } finally {
            exchange.close()
        }
    }

    http.start()
    logger.info("MCP HTTP server started on $bindAddress:$port (stateless JSON-RPC)")
    return http
}

/** Line-delimited JSON-RPC over stdin/stdout. Returns the daemon thread, already started. */
fun startMcpStdio(server: MiniMcpServer, logger: Logger): Thread {
    val thread = Thread({
        val reader = BufferedReader(InputStreamReader(System.`in`))
        while (true) {
            val line = reader.readLine() ?: break
            if (line.isBlank()) continue
            val response = server.handle(line) ?: continue
            println(response.json)
        }
    }, "spigot-mcp-stdio")
    thread.isDaemon = true
    thread.start()
    logger.info("MCP stdio transport started")
    return thread
}
