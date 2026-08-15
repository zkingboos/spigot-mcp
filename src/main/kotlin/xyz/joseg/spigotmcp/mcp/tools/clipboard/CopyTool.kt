package xyz.joseg.spigotmcp.mcp.tools.clipboard

import io.modelcontextprotocol.spec.McpSchema
import xyz.joseg.spigotmcp.fawe.FaweAdapter
import xyz.joseg.spigotmcp.mcp.tools.ToolDefinition
import xyz.joseg.spigotmcp.util.Pos

fun createCopyTool(fawe: FaweAdapter): ToolDefinition {
    return ToolDefinition(
        name = "copy",
        description = "Copy a region to clipboard",
        inputSchemaJson = """
            {
                "type": "object",
                "properties": {
                    "pos1": {"type": "object", "properties": {"x": {"type": "integer"}, "y": {"type": "integer"}, "z": {"type": "integer"}, "world": {"type": "string"}}},
                    "pos2": {"type": "object", "properties": {"x": {"type": "integer"}, "y": {"type": "integer"}, "z": {"type": "integer"}, "world": {"type": "string"}}}
                },
                "required": ["pos1", "pos2"]
            }
        """
    ) { args ->
        val pos1Map = args["pos1"] as Map<String, Any>
        val pos2Map = args["pos2"] as Map<String, Any>
        
        val pos1 = Pos(
            pos1Map["x"] as Int,
            pos1Map["y"] as Int,
            pos1Map["z"] as Int,
            pos1Map["world"] as String
        )
        val pos2 = Pos(
            pos2Map["x"] as Int,
            pos2Map["y"] as Int,
            pos2Map["z"] as Int,
            pos2Map["world"] as String
        )
        
        val result = fawe.copy(pos1, pos2)
        if (result.isSuccess) {
            McpSchema.CallToolResult(
                listOf(McpSchema.TextContent("Copied region to clipboard")),
                false
            )
        } else {
            McpSchema.CallToolResult(
                listOf(McpSchema.TextContent("Failed to copy: ${result.exceptionOrNull()?.message}")),
                true
            )
        }
    }
}