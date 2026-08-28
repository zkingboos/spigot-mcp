package xyz.joseg.spigotmcp.mcp.tools.block

import xyz.joseg.spigotmcp.mcp.tools.Schemas
import xyz.joseg.spigotmcp.mcp.tools.ToolDefinition
import xyz.joseg.spigotmcp.mcp.tools.map
import xyz.joseg.spigotmcp.mcp.tools.pos
import xyz.joseg.spigotmcp.mcp.tools.string
import xyz.joseg.spigotmcp.mcp.tools.toToolResult
import xyz.joseg.spigotmcp.mcp.tools.toolResult
import xyz.joseg.spigotmcp.worldedit.WorldEditService

fun createReplaceBlocksTool(worldEdit: WorldEditService): ToolDefinition = ToolDefinition(
    name = "replace_blocks",
    description = "Replace one block material with another inside a region",
    inputSchemaJson = """
        {
            "type": "object",
            "properties": {
                "region": ${Schemas.REGION},
                "from": {"type": "string", "description": "Material to replace. ${Schemas.MATERIAL_DESCRIPTION}"},
                "to": {"type": "string", "description": "Material to replace with. ${Schemas.MATERIAL_DESCRIPTION}"}
            },
            "required": ["region", "from", "to"]
        }
    """
) { args ->
    toolResult {
        val region = args.map("region")
        val from = args.string("from")
        val to = args.string("to")

        worldEdit.replaceBlocks(region.pos("pos1"), region.pos("pos2"), from, to)
            .toToolResult { blocks -> "Replaced $from with $to across $blocks blocks" }
    }
}
