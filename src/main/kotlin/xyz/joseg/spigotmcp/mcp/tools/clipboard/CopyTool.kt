package xyz.joseg.spigotmcp.mcp.tools.clipboard

import xyz.joseg.spigotmcp.mcp.tools.Schemas
import xyz.joseg.spigotmcp.mcp.tools.ToolDefinition
import xyz.joseg.spigotmcp.mcp.tools.pos
import xyz.joseg.spigotmcp.mcp.tools.toToolResult
import xyz.joseg.spigotmcp.mcp.tools.toolResult
import xyz.joseg.spigotmcp.worldedit.WorldEditService

fun createCopyTool(worldEdit: WorldEditService): ToolDefinition = ToolDefinition(
    name = "copy",
    description = "Copy a region to clipboard",
    inputSchemaJson = """
        {
            "type": "object",
            "properties": {"pos1": ${Schemas.POSITION}, "pos2": ${Schemas.POSITION}},
            "required": ["pos1", "pos2"]
        }
    """
) { args ->
    toolResult {
        worldEdit.copy(args.pos("pos1"), args.pos("pos2"))
            .toToolResult { "Copied region to clipboard" }
    }
}
