package xyz.joseg.spigotmcp.mcp.tools.clipboard

import io.modelcontextprotocol.spec.McpSchema
import xyz.joseg.spigotmcp.fawe.FaweAdapter
import xyz.joseg.spigotmcp.mcp.tools.ToolDefinition
import xyz.joseg.spigotmcp.util.Pos

fun createPasteTool(fawe: FaweAdapter): ToolDefinition {
    return ToolDefinition(
        name = "paste",
        description = "Paste clipboard at origin position",
        inputSchemaJson = """
            {
                "type": "object",
                "properties": {
                    "origin": {"type": "object", "properties": {"x": {"type": "integer"}, "y": {"type": "integer"}, "z": {"type": "integer"}, "world": {"type": "string"}}},
                    "rotation": {"type": "integer", "default": 0, "description": "Rotation in degrees (0, 90, 180, 270)"}
                },
                "required": ["origin"]
            }
        """
    ) { args ->
        val originMap = args["origin"] as Map<String, Any>
        val rotation = (args["rotation"] as? Int) ?: 0
        
        val origin = Pos(
            originMap["x"] as Int,
            originMap["y"] as Int,
            originMap["z"] as Int,
            originMap["world"] as String
        )
        
        val result = fawe.paste(origin, rotation)
        if (result.isSuccess) {
            McpSchema.CallToolResult(
                listOf(McpSchema.TextContent("Pasted ${result.getOrNull()} blocks from clipboard")),
                false
            )
        } else {
            McpSchema.CallToolResult(
                listOf(McpSchema.TextContent("Failed to paste: ${result.exceptionOrNull()?.message}")),
                true
            )
        }
    }
}