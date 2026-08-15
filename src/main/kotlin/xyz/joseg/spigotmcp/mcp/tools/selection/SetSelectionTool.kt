package xyz.joseg.spigotmcp.mcp.tools.selection

import io.modelcontextprotocol.spec.McpSchema
import xyz.joseg.spigotmcp.mcp.tools.ToolDefinition
import xyz.joseg.spigotmcp.util.Pos

fun createSetSelectionTool(): ToolDefinition {
    return ToolDefinition(
        name = "set_selection",
        description = "Set WorldEdit selection points",
        inputSchemaJson = """
            {
                "type": "object",
                "properties": {
                    "pos1": {"type": "object", "properties": {"x": {"type": "integer"}, "y": {"type": "integer"}, "z": {"type": "integer"}, "world": {"type": "string"}}},
                    "pos2": {"type": "object", "properties": {"x": {"type": "integer"}, "y": {"type": "integer"}, "z": {"type": "integer"}, "world": {"type": "string"}}}
                }
            }
        """
    ) { args ->
        val pos1Map = args["pos1"] as? Map<String, Any>
        val pos2Map = args["pos2"] as? Map<String, Any>
        
        var pos1: Pos? = null
        var pos2: Pos? = null
        
        pos1Map?.let {
            pos1 = Pos(it["x"] as Int, it["y"] as Int, it["z"] as Int, it["world"] as String)
        }
        pos2Map?.let {
            pos2 = Pos(it["x"] as Int, it["y"] as Int, it["z"] as Int, it["world"] as String)
        }
        
        // Note: Would need integration with WE selection API
        McpSchema.CallToolResult(
            listOf(McpSchema.TextContent("Selection set (requires WE player session integration)")),
            false
        )
    }
}