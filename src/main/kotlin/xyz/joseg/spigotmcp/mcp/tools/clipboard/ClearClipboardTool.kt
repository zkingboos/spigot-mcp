package xyz.joseg.spigotmcp.mcp.tools.clipboard

import xyz.joseg.spigotmcp.mcp.tools.ToolDefinition
import xyz.joseg.spigotmcp.mcp.tools.toToolResult
import xyz.joseg.spigotmcp.mcp.tools.toolResult
import xyz.joseg.spigotmcp.worldedit.WorldEditService

fun createClearClipboardTool(worldEdit: WorldEditService): ToolDefinition = ToolDefinition(
    name = "clear_clipboard",
    description = "Clear the clipboard",
    inputSchemaJson = """{"type": "object", "properties": {}}"""
) { _ ->
    toolResult {
        worldEdit.clearClipboard().toToolResult { "Clipboard cleared" }
    }
}
