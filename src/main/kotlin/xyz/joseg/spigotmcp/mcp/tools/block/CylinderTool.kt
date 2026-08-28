package xyz.joseg.spigotmcp.mcp.tools.block

import xyz.joseg.spigotmcp.mcp.tools.Schemas
import xyz.joseg.spigotmcp.mcp.tools.ToolDefinition
import xyz.joseg.spigotmcp.mcp.tools.int
import xyz.joseg.spigotmcp.mcp.tools.pos
import xyz.joseg.spigotmcp.mcp.tools.string
import xyz.joseg.spigotmcp.mcp.tools.toToolResult
import xyz.joseg.spigotmcp.mcp.tools.toolResult
import xyz.joseg.spigotmcp.worldedit.WorldEditService

fun createCylinderTool(worldEdit: WorldEditService): ToolDefinition = ToolDefinition(
    name = "cylinder",
    description = "Create a filled vertical cylinder centered on a position",
    inputSchemaJson = """
        {
            "type": "object",
            "properties": {
                "center": ${Schemas.POSITION},
                "radius": {"type": "integer", "description": "Cylinder radius in blocks"},
                "height": {"type": "integer", "description": "Cylinder height in blocks"},
                "material": {"type": "string", "description": "${Schemas.MATERIAL_DESCRIPTION}"}
            },
            "required": ["center", "radius", "height", "material"]
        }
    """
) { args ->
    toolResult {
        val center = args.pos("center")
        val radius = args.int("radius")
        val height = args.int("height")
        val material = args.string("material")

        worldEdit.cylinder(center, radius, height, material)
            .toToolResult { blocks -> "Created cylinder r=$radius h=$height with $blocks blocks of $material" }
    }
}
