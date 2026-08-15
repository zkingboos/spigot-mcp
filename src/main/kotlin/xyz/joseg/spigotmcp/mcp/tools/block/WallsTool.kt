package xyz.joseg.spigotmcp.mcp.tools.block

import io.modelcontextprotocol.spec.McpSchema
import xyz.joseg.spigotmcp.fawe.FaweAdapter
import xyz.joseg.spigotmcp.mcp.tools.ToolDefinition
import xyz.joseg.spigotmcp.util.Pos

fun createWallsTool(fawe: FaweAdapter): ToolDefinition {
    return ToolDefinition(
        name = "walls",
        description = "Build walls around a region",
        inputSchemaJson = """
            {
                "type": "object",
                "properties": {
                    "region": {
                        "type": "object",
                        "properties": {
                            "pos1": {"type": "object", "properties": {"x": {"type": "integer"}, "y": {"type": "integer"}, "z": {"type": "integer"}, "world": {"type": "string"}}},
                            "pos2": {"type": "object", "properties": {"x": {"type": "integer"}, "y": {"type": "integer"}, "z": {"type": "integer"}, "world": {"type": "string"}}}
                        },
                        "required": ["pos1", "pos2"]
                    },
                    "material": {"type": "string", "description": "Wall material"}
                },
                "required": ["region", "material"]
            }
        """
    ) { args ->
        val regionMap = args["region"] as Map<String, Any>
        val material = args["material"] as String
        
        val pos1Map = regionMap["pos1"] as Map<String, Any>
        val pos2Map = regionMap["pos2"] as Map<String, Any>
        
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
        
        val result = fawe.walls(pos1, pos2, material)
        if (result.isSuccess) {
            McpSchema.CallToolResult(
                listOf(McpSchema.TextContent("Built walls: ${result.getOrNull()} blocks of $material")),
                false
            )
        } else {
            McpSchema.CallToolResult(
                listOf(McpSchema.TextContent("Failed to build walls: ${result.exceptionOrNull()?.message}")),
                true
            )
        }
    }
}