package xyz.joseg.spigotmcp.mcp.tools.selection

import io.modelcontextprotocol.spec.McpSchema
import xyz.joseg.spigotmcp.mcp.tools.ToolDefinition
import kotlinx.serialization.json.Json

fun createGetSelectionTool(): ToolDefinition {
    return ToolDefinition(
        name = "get_selection",
        description = "Get current WorldEdit selection (pos1, pos2)",
        inputSchemaJson = """{"type": "object", "properties": {}}"""
    ) { args ->
        // Note: Would need integration with WE selection API
        val json = Json.encodeToString(mapOf("pos1" to null, "pos2" to null))
        McpSchema.CallToolResult(
            listOf(McpSchema.TextContent(json)),
            false
        )
    }
}