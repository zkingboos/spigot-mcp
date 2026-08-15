package xyz.joseg.spigotmcp.mcp.tools

import com.fasterxml.jackson.databind.ObjectMapper
import io.modelcontextprotocol.json.McpJsonMapper
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper
import io.modelcontextprotocol.server.McpServerFeatures
import io.modelcontextprotocol.spec.McpSchema
import reactor.core.publisher.Mono
import java.util.function.BiFunction

// Simple tool data class - avoids Kotlin compiler caching issues
data class ToolDefinition(
    val name: String,
    val description: String,
    val inputSchemaJson: String,
    val execute: (Map<String, Any>) -> McpSchema.CallToolResult
) {
    private val jsonMapper: McpJsonMapper by lazy {
        val objectMapper = ObjectMapper()
        objectMapper.findAndRegisterModules()
        JacksonMcpJsonMapper(objectMapper)
    }
    
    fun toAsyncTool(): McpServerFeatures.AsyncToolSpecification {
        val schema = McpSchema.Tool.builder()
            .name(name)
            .description(description)
            .inputSchema(jsonMapper, inputSchemaJson)
            .build()
        
        val handler: BiFunction<
            io.modelcontextprotocol.server.McpAsyncServerExchange,
            Map<String?, Any?>,
            reactor.core.publisher.Mono<io.modelcontextprotocol.spec.McpSchema.CallToolResult>?
        > = BiFunction { exchange, args ->
            val result = execute(args as Map<String, Any>)
            Mono.just(result)
        }
        
        return McpServerFeatures.AsyncToolSpecification(schema, handler)
    }
}