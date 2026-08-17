package xyz.joseg.spigotmcp.mcp.tools.player

import io.modelcontextprotocol.spec.McpSchema
import xyz.joseg.spigotmcp.mcp.tools.ToolDefinition
import xyz.joseg.spigotmcp.util.Pos
import org.bukkit.Bukkit

fun createGetPlayerPositionTool(): ToolDefinition {
    return ToolDefinition(
        name = "get_player_position",
        description = "Get a player's current position",
        inputSchemaJson = """
            {
                "type": "object",
                "properties": {
                    "playerName": {"type": "string", "description": "Player name (or UUID)"}
                },
                "required": ["playerName"]
            }
        """
    ) { args ->
        val playerName = args["playerName"] as String
        
        val player = Bukkit.getPlayer(playerName) ?: Bukkit.getOfflinePlayer(playerName).player
        
        if (player == null) {
            McpSchema.CallToolResult(
                listOf(McpSchema.TextContent("Player not found: $playerName")),
                true
            )
        } else {
            val pos = Pos(
                player.location.blockX,
                player.location.blockY,
                player.location.blockZ,
                player.world.name
            )
            McpSchema.CallToolResult(
                listOf(McpSchema.TextContent("Player $playerName position: x=${pos.x}, y=${pos.y}, z=${pos.z}, world=${pos.world}")),
                false
            )
        }
    }
}