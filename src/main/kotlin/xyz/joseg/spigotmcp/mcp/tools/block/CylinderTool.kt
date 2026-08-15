package xyz.joseg.spigotmcp.mcp.tools.block

import io.modelcontextprotocol.spec.McpSchema
import xyz.joseg.spigotmcp.fawe.FaweAdapter
import xyz.joseg.spigotmcp.mcp.tools.ToolDefinition
import xyz.joseg.spigotmcp.util.Pos

fun createCylinderTool(fawe: FaweAdapter): ToolDefinition {
    return ToolDefinition(
        name = "cylinder",
        description = "Create a cylinder at center with given radius and height",
        inputSchemaJson = """
            {
                "type": "object",
                "properties": {
                    "center": {"type": "object", "properties": {"x": {"type": "integer"}, "y": {"type": "integer"}, "z": {"type": "integer"}, "world": {"type": "string"}}},
                    "radius": {"type": "integer", "minimum": 1},
                    "height": {"type": "integer", "minimum": 1},
                    "material": {"type": "string"}
                },
                "required": ["center", "radius", "height", "material"]
            }
        """
    ) { args ->
        val centerMap = args["center"] as Map<String, Any>
        val radius = args["radius"] as Int
        val height = args["height"] as Int
        val material = args["material"] as String
        
        val center = Pos(
            centerMap["x"] as Int,
            centerMap["y"] as Int,
            centerMap["z"] as Int,
            centerMap["world"] as String
        )
        
        val result = fawe.cylinder(center, radius, height, material)
        if (result.isSuccess) {
            McpSchema.CallToolResult(
                listOf(McpSchema.TextContent("Created cylinder: ${result.getOrNull()} blocks of $material (radius $radius, height $height)")),
                false
            )
        } else {
            McpSchema.CallToolResult(
                listOf(McpSchema.TextContent("Failed to create cylinder: ${result.exceptionOrNull()?.message}")),
                true
            )
        }
    }
}