package xyz.joseg.spigotmcp.mcp.tools.block

import com.sk89q.worldedit.EditSession
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import io.modelcontextprotocol.spec.McpSchema
import org.bukkit.block.data.MultipleFacing
import xyz.joseg.spigotmcp.fawe.FaweAdapter
import xyz.joseg.spigotmcp.mcp.tools.ToolDefinition

private data class PlacedBlock(val world: String, val x: Int, val y: Int, val z: Int, val material: String)

fun createBatchBlocksTool(fawe: FaweAdapter): ToolDefinition {
    return ToolDefinition(
        name = "batch_blocks",
        description = "Place multiple blocks at specific positions with different materials in a single operation.",
        inputSchemaJson = """
            {
                "type": "object",
                "properties": {
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
                                "material": {"type": "string", "description": "Block material (e.g., stone, minecraft:oak_stairs, minecraft:oak_door)"},
                                "facing": {"type": "string", "description": "Optional facing direction (north, south, east, west, up, down). For doors it applies to both halves."}
                            },
                            "required": ["x", "y", "z", "world", "material"]
                        }
                    }
                },
                "required": ["blocks"]
            }
        """
    ) { args ->
        val blocksList = args["blocks"] as List<Map<String, Any>>
        if (blocksList.isEmpty()) {
            McpSchema.CallToolResult(
                listOf(McpSchema.TextContent("Blocks list cannot be empty")),
                true
            )
        } else {
            val topWorld = args["world"] as? String
            var session: EditSession? = null
            var totalBlocks = 0
            val placed = mutableListOf<PlacedBlock>()
            
            for (blockMap in blocksList) {
                val x = blockMap["x"] as Int
                val y = blockMap["y"] as Int
                val z = blockMap["z"] as Int
                val world = (blockMap["world"] as? String) ?: topWorld ?: return@ToolDefinition McpSchema.CallToolResult(
                    listOf(McpSchema.TextContent("World name missing for block at ($x, $y, $z)")),
                    true
                )
                val material = blockMap["material"] as String
                val facing = blockMap["facing"] as? String
                
                val s = session ?: fawe.createSession(world).also { session = it }
                
                if (material.endsWith("_door")) {
                    setBlock(s, fawe, world, material, facing, x, y, z, placed, half = "lower")
                    setBlock(s, fawe, world, material, facing, x, y + 1, z, placed, half = "upper")
                    totalBlocks++
                } else {
                    setBlock(s, fawe, world, material, facing, x, y, z, placed)
                    totalBlocks++
                }
            }
            
            session?.flushQueue()
            
            val future = org.bukkit.Bukkit.getScheduler().callSyncMethod(
                xyz.joseg.spigotmcp.SpigotMCPPlugin.instance,
                java.util.concurrent.Callable {
                    for (p in placed) {
                        val bworld = fawe.getBukkitWorld(p.world)
                        val block = bworld.getBlockAt(p.x, p.y, p.z)
                        val material = p.material.removePrefix("minecraft:")
                        
                        val multi = block.blockData as? MultipleFacing
                        if (multi != null && (material.endsWith("_pane") || material == "iron_bars" || material.endsWith("_fence"))) {
                            for (face in multi.allowedFaces) {
                                multi.setFace(face, connectsTo(block.getRelative(face)))
                            }
                            block.setBlockData(multi)
                        }
                        
                        val chestData = block.blockData as? org.bukkit.block.data.type.Chest
                        if (chestData != null && material.endsWith("chest")) {
                            linkChest(chestData, block)
                        }
                        
                        block.state.update(true, true)
                    }
                    null
                }
            )
            future.get(30, java.util.concurrent.TimeUnit.SECONDS)
            
            McpSchema.CallToolResult(
                listOf(McpSchema.TextContent("Placed $totalBlocks blocks in batch")),
                false
            )
        }
    }
}

private fun rotateY(face: org.bukkit.block.BlockFace, clockwise: Boolean): org.bukkit.block.BlockFace = when (face) {
    org.bukkit.block.BlockFace.NORTH -> if (clockwise) org.bukkit.block.BlockFace.EAST else org.bukkit.block.BlockFace.WEST
    org.bukkit.block.BlockFace.EAST -> if (clockwise) org.bukkit.block.BlockFace.SOUTH else org.bukkit.block.BlockFace.NORTH
    org.bukkit.block.BlockFace.SOUTH -> if (clockwise) org.bukkit.block.BlockFace.WEST else org.bukkit.block.BlockFace.EAST
    org.bukkit.block.BlockFace.WEST -> if (clockwise) org.bukkit.block.BlockFace.NORTH else org.bukkit.block.BlockFace.SOUTH
    else -> face
}

private fun connectsTo(neighbor: org.bukkit.block.Block): Boolean {
    val name = neighbor.type.name
    return name.endsWith("_PANE") || name == "IRON_BARS" || name.endsWith("_FENCE") ||
        name.endsWith("_FENCE_GATE") || name.endsWith("_WALL") || name == "GLASS" ||
        neighbor.type.isOccluding()
}

private fun linkChest(chestData: org.bukkit.block.data.type.Chest, block: org.bukkit.block.Block) {
    if (chestData.type != org.bukkit.block.data.type.Chest.Type.SINGLE) return
    
    val singles = listOf(
        org.bukkit.block.BlockFace.NORTH to block.getRelative(org.bukkit.block.BlockFace.NORTH),
        org.bukkit.block.BlockFace.SOUTH to block.getRelative(org.bukkit.block.BlockFace.SOUTH),
        org.bukkit.block.BlockFace.EAST to block.getRelative(org.bukkit.block.BlockFace.EAST),
        org.bukkit.block.BlockFace.WEST to block.getRelative(org.bukkit.block.BlockFace.WEST)
    ).filter { (it.second.blockData as? org.bukkit.block.data.type.Chest)?.type == org.bukkit.block.data.type.Chest.Type.SINGLE }
    if (singles.size != 1) return
    
    val (neighborFace, neighborBlock) = singles.first()
    val nbData = neighborBlock.blockData as org.bukkit.block.data.type.Chest
    
    val perpendicular = if (neighborFace == org.bukkit.block.BlockFace.EAST || neighborFace == org.bukkit.block.BlockFace.WEST) {
        org.bukkit.block.BlockFace.NORTH
    } else {
        org.bukkit.block.BlockFace.EAST
    }
    val facing = chestData.facing
    val desired = if (facing.modX == perpendicular.modX && facing.modZ == perpendicular.modZ) facing else perpendicular
    chestData.facing = desired
    nbData.facing = desired
    
    if (neighborFace == rotateY(desired, clockwise = true)) {
        chestData.type = org.bukkit.block.data.type.Chest.Type.LEFT
        nbData.type = org.bukkit.block.data.type.Chest.Type.RIGHT
    } else {
        chestData.type = org.bukkit.block.data.type.Chest.Type.RIGHT
        nbData.type = org.bukkit.block.data.type.Chest.Type.LEFT
    }
    block.setBlockData(chestData)
    neighborBlock.setBlockData(nbData)
}

private fun setBlock(
    session: EditSession,
    fawe: FaweAdapter,
    world: String,
    material: String,
    facing: String?,
    x: Int,
    y: Int,
    z: Int,
    placed: MutableList<PlacedBlock>,
    half: String? = null
) {
    val stateStr = buildString {
        append(material)
        val props = mutableListOf<String>()
        facing?.let { props.add("facing=$it") }
        half?.let { props.add("half=$it") }
        if (props.isNotEmpty()) append(props.joinToString(",", prefix = "[", postfix = "]"))
    }
    val pattern = try {
        fawe.parseBlockStatePattern(stateStr)
    } catch (e: Exception) {
        fawe.parsePattern(material)
    }
    val blockVec = BlockVector3.at(x, y, z)
    val region = CuboidRegion(blockVec, blockVec)
    session.setBlocks(region as com.sk89q.worldedit.regions.Region, pattern)
    placed.add(PlacedBlock(world, x, y, z, material))
}