package xyz.joseg.spigotmcp.mcp.tools.block

import io.modelcontextprotocol.spec.McpSchema
import xyz.joseg.spigotmcp.fawe.FaweAdapter
import xyz.joseg.spigotmcp.mcp.tools.ToolDefinition
import xyz.joseg.spigotmcp.util.Pos

fun createSphereTool(fawe: FaweAdapter): ToolDefinition {
    return ToolDefinition(
        name = "sphere",
        description = "Create a sphere at center with given radius",
        inputSchemaJson = """
            {
                "type": "object",
                "properties": {
                    "center": {"type": "object", "properties": {"x": {"type": "integer"}, "y": {"type": "integer"}, "z": {"type": "integer"}, "world": {"type": "string"}}},
                    "radius": {"type": "integer", "minimum": 1},
                    "material": {"type": "string"}
                },
                "required": ["center", "radius", "material"]
            }
        """
    ) { args ->
        val centerMap = args["center"] as Map<String, Any>
        val radius = args["radius"] as Int
        val material = args["material"] as String
        
        val center = Pos(
            centerMap["x"] as Int,
            centerMap["y"] as Int,
            centerMap["z"] as Int,
            centerMap["world"] as String
        )
        
        val result = fawe.sphere(center, radius, material)
        if (result.isSuccess) {
            McpSchema.CallToolResult(
                listOf(McpSchema.TextContent("Created sphere: ${result.getOrNull()} blocks of $material (radius $radius)")),
                false
            )
        } else {
            McpSchema.CallToolResult(
                listOf(McpSchema.TextContent("Failed to create sphere: ${result.exceptionOrNull()?.message}")),
                true
            )
        }
    }
}