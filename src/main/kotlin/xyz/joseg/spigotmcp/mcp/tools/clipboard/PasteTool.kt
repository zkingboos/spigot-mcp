package xyz.joseg.spigotmcp.mcp.tools.clipboard

import xyz.joseg.spigotmcp.mcp.tools.Schemas
import xyz.joseg.spigotmcp.mcp.tools.ToolDefinition
import xyz.joseg.spigotmcp.mcp.tools.intOr
import xyz.joseg.spigotmcp.mcp.tools.pos
import xyz.joseg.spigotmcp.mcp.tools.toToolResult
import xyz.joseg.spigotmcp.mcp.tools.toolResult
import xyz.joseg.spigotmcp.worldedit.WorldEditService

fun createPasteTool(worldEdit: WorldEditService): ToolDefinition = ToolDefinition(
    name = "paste",
    description = "Paste clipboard at origin position",
    inputSchemaJson = """
        {
            "type": "object",
            "properties": {
                "origin": ${Schemas.POSITION},
                "rotation": {"type": "integer", "default": 0, "description": "Rotation in degrees (0, 90, 180, 270)"}
            },
            "required": ["origin"]
        }
    """
) { args ->
    toolResult {
        worldEdit.paste(args.pos("origin"), args.intOr("rotation", 0))
            .toToolResult { blocks -> "Pasted $blocks blocks from clipboard" }
    }
}
