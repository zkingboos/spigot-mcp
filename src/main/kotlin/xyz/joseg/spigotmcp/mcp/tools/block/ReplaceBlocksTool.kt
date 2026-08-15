package xyz.joseg.spigotmcp.mcp.tools.block

import io.modelcontextprotocol.spec.McpSchema
import xyz.joseg.spigotmcp.fawe.FaweAdapter
import xyz.joseg.spigotmcp.mcp.tools.ToolDefinition
import xyz.joseg.spigotmcp.util.Pos

fun createReplaceBlocksTool(fawe: FaweAdapter): ToolDefinition {
    return ToolDefinition(
        name = "replace_blocks",
        description = "Replace all blocks of one material with another in a region",
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
                    "from": {"type": "string", "description": "Material to replace"},
                    "to": {"type": "string", "description": "Material to replace with"}
                },
                "required": ["region", "from", "to"]
            }
        """
    ) { args ->
        val regionMap = args["region"] as Map<String, Any>
        val from = args["from"] as String
        val to = args["to"] as String
        
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
        
        val result = fawe.replaceBlocks(pos1, pos2, from, to)
        if (result.isSuccess) {
            McpSchema.CallToolResult(
                listOf(McpSchema.TextContent("Replaced ${result.getOrNull()} blocks: $from → $to")),
                false
            )
        } else {
            McpSchema.CallToolResult(
                listOf(McpSchema.TextContent("Failed to replace blocks: ${result.exceptionOrNull()?.message}")),
                true
            )
        }
    }
}