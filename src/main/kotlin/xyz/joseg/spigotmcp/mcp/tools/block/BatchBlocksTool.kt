package xyz.joseg.spigotmcp.mcp.tools.block

import xyz.joseg.spigotmcp.mcp.tools.Schemas
import xyz.joseg.spigotmcp.mcp.tools.ToolDefinition
import xyz.joseg.spigotmcp.mcp.tools.mapList
import xyz.joseg.spigotmcp.mcp.tools.string
import xyz.joseg.spigotmcp.mcp.tools.stringOrNull
import xyz.joseg.spigotmcp.mcp.tools.toPos
import xyz.joseg.spigotmcp.mcp.tools.toToolResult
import xyz.joseg.spigotmcp.mcp.tools.toolResult
import xyz.joseg.spigotmcp.worldedit.BlockPlacementRequest
import xyz.joseg.spigotmcp.worldedit.WorldEditService

fun createBatchBlocksTool(worldEdit: WorldEditService): ToolDefinition = ToolDefinition(
    name = "batch_blocks",
    description = "Place multiple blocks at specific positions with different materials in a single operation.",
    inputSchemaJson = """
        {
            "type": "object",
            "properties": {
                "world": {"type": "string", "description": "Default world name for entries that omit it"},
                "blocks": {
                    "type": "array",
                    "description": "List of blocks to place",
                    "items": {
                        "type": "object",
                        "properties": {
                            "x": {"type": "integer", "description": "X coordinate"},
                            "y": {"type": "integer", "description": "Y coordinate"},
                            "z": {"type": "integer", "description": "Z coordinate"},
                            "world": {"type": "string", "description": "World name"},
                            "material": {"type": "string", "description": "${Schemas.MATERIAL_DESCRIPTION}"},
                            "facing": {"type": "string", "description": "Optional facing direction (north, south, east, west, up, down). For doors it applies to both halves."}
                        },
                        "required": ["x", "y", "z", "material"]
                    }
                }
            },
            "required": ["blocks"]
        }
    """
) { args ->
    toolResult {
        val defaultWorld = args.stringOrNull("world")
        val requests = args.mapList("blocks").map { entry ->
            BlockPlacementRequest(
                pos = entry.toPos(defaultWorld),
                material = entry.string("material"),
                facing = entry.stringOrNull("facing")
            )
        }

        worldEdit.placeBlocks(requests).toToolResult { blocks -> "Placed $blocks blocks in batch" }
    }
}
