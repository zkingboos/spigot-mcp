package xyz.joseg.spigotmcp.mcp.tools.block

import xyz.joseg.spigotmcp.mcp.tools.Schemas
import xyz.joseg.spigotmcp.mcp.tools.ToolDefinition
import xyz.joseg.spigotmcp.mcp.tools.int
import xyz.joseg.spigotmcp.mcp.tools.pos
import xyz.joseg.spigotmcp.mcp.tools.string
import xyz.joseg.spigotmcp.mcp.tools.toToolResult
import xyz.joseg.spigotmcp.mcp.tools.toolResult
import xyz.joseg.spigotmcp.worldedit.WorldEditService

fun createSphereTool(worldEdit: WorldEditService): ToolDefinition = ToolDefinition(
    name = "sphere",
    description = "Create a filled sphere centered on a position",
    inputSchemaJson = """
        {
            "type": "object",
            "properties": {
                "center": ${Schemas.POSITION},
                "radius": {"type": "integer", "description": "Sphere radius in blocks"},
                "material": {"type": "string", "description": "${Schemas.MATERIAL_DESCRIPTION}"}
            },
            "required": ["center", "radius", "material"]
        }
    """
) { args ->
    toolResult {
        val center = args.pos("center")
        val radius = args.int("radius")
        val material = args.string("material")

        worldEdit.sphere(center, radius, material)
            .toToolResult { blocks -> "Created sphere of radius $radius with $blocks blocks of $material" }
    }
}
