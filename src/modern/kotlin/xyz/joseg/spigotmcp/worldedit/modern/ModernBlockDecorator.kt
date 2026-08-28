package xyz.joseg.spigotmcp.worldedit.modern

import org.bukkit.Bukkit
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.MultipleFacing
import org.bukkit.block.data.type.Chest
import xyz.joseg.spigotmcp.SpigotMCPPlugin
import xyz.joseg.spigotmcp.worldedit.BlockPlacement
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

internal object ModernBlockDecorator {

    private const val SYNC_TIMEOUT_SECONDS = 30L

    private val HORIZONTAL = listOf(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)

    fun decorate(placements: List<BlockPlacement>) {
        if (placements.isEmpty()) return

        Bukkit.getScheduler()
            .callSyncMethod(SpigotMCPPlugin.instance, Callable { placements.forEach(::decorate) })
            .get(SYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    private fun decorate(placement: BlockPlacement) {
        val world = Bukkit.getWorld(placement.pos.world) ?: return
        val block = world.getBlockAt(placement.pos.x, placement.pos.y, placement.pos.z)
        val material = placement.block.simpleId

        if (connectsToNeighbours(material)) {
            connect(block)
        }
        if (material.endsWith("chest")) {
            linkChest(block)
        }
        block.state.update(true, true)
    }

    private fun connectsToNeighbours(material: String): Boolean =
        material.endsWith("_pane") || material == "iron_bars" || material.endsWith("_fence")

    private fun connect(block: Block) {
        val facing = block.blockData as? MultipleFacing ?: return
        facing.allowedFaces.forEach { face ->
            facing.setFace(face, connectsTo(block.getRelative(face)))
        }
        block.blockData = facing
    }

    private fun connectsTo(neighbour: Block): Boolean {
        val name = neighbour.type.name
        return name.endsWith("_PANE") || name == "IRON_BARS" || name.endsWith("_FENCE") ||
            name.endsWith("_FENCE_GATE") || name.endsWith("_WALL") || name == "GLASS" ||
            neighbour.type.isOccluding
    }

    private fun linkChest(block: Block) {
        val chest = block.blockData as? Chest ?: return
        if (chest.type != Chest.Type.SINGLE) return

        val neighbours = HORIZONTAL
            .map { face -> face to block.getRelative(face) }
            .filter { (_, candidate) -> (candidate.blockData as? Chest)?.type == Chest.Type.SINGLE }
        if (neighbours.size != 1) return

        val (neighbourFace, neighbourBlock) = neighbours.single()
        val neighbourChest = neighbourBlock.blockData as Chest

        val perpendicular =
            if (neighbourFace == BlockFace.EAST || neighbourFace == BlockFace.WEST) BlockFace.NORTH else BlockFace.EAST
        val facing = chest.facing
        val shared =
            if (facing.modX == perpendicular.modX && facing.modZ == perpendicular.modZ) facing else perpendicular

        chest.facing = shared
        neighbourChest.facing = shared

        if (neighbourFace == rotateClockwise(shared)) {
            chest.type = Chest.Type.LEFT
            neighbourChest.type = Chest.Type.RIGHT
        } else {
            chest.type = Chest.Type.RIGHT
            neighbourChest.type = Chest.Type.LEFT
        }

        block.blockData = chest
        neighbourBlock.blockData = neighbourChest
    }

    private fun rotateClockwise(face: BlockFace): BlockFace = when (face) {
        BlockFace.NORTH -> BlockFace.EAST
        BlockFace.EAST -> BlockFace.SOUTH
        BlockFace.SOUTH -> BlockFace.WEST
        BlockFace.WEST -> BlockFace.NORTH
        else -> face
    }
}
