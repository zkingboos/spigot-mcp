package xyz.joseg.spigotmcp.mcp.tools.clipboard

import io.modelcontextprotocol.spec.McpSchema
import xyz.joseg.spigotmcp.fawe.FaweAdapter
import xyz.joseg.spigotmcp.mcp.tools.ToolDefinition

fun createClearClipboardTool(fawe: FaweAdapter): ToolDefinition {
    return ToolDefinition(
        name = "clear_clipboard",
        description = "Clear the clipboard",
        inputSchemaJson = """{"type": "object", "properties": {}}"""
    ) { args ->
        val result = fawe.clearClipboard()
        if (result.isSuccess) {
            McpSchema.CallToolResult(
                listOf(McpSchema.TextContent("Clipboard cleared")),
                false
            )
        } else {
            McpSchema.CallToolResult(
                listOf(McpSchema.TextContent("Failed to clear clipboard: ${result.exceptionOrNull()?.message}")),
                true
            )
        }
    }
}