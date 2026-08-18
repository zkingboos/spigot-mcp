package xyz.joseg.spigotmcp.mcp.tools.block

import xyz.joseg.spigotmcp.mcp.tools.Schemas
import xyz.joseg.spigotmcp.mcp.tools.ToolDefinition
import xyz.joseg.spigotmcp.mcp.tools.map
import xyz.joseg.spigotmcp.mcp.tools.pos
import xyz.joseg.spigotmcp.mcp.tools.string
import xyz.joseg.spigotmcp.mcp.tools.toToolResult
import xyz.joseg.spigotmcp.mcp.tools.toolResult
import xyz.joseg.spigotmcp.worldedit.WorldEditService

fun createWallsTool(worldEdit: WorldEditService): ToolDefinition = ToolDefinition(
    name = "walls",
    description = "Build the four vertical walls of a region, leaving floor and ceiling untouched",
    inputSchemaJson = """
        {
            "type": "object",
            "properties": {
                "region": ${Schemas.REGION},
                "material": {"type": "string", "description": "${Schemas.MATERIAL_DESCRIPTION}"}
            },
            "required": ["region", "material"]
        }
    """
) { args ->
    toolResult {
        val region = args.map("region")
        val material = args.string("material")

        worldEdit.walls(region.pos("pos1"), region.pos("pos2"), material)
            .toToolResult { blocks -> "Built walls with $blocks blocks of $material" }
    }
}
