package xyz.joseg.spigotmcp.worldedit.modern

import com.sk89q.worldedit.EditSession
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.function.mask.BlockTypeMask
import com.sk89q.worldedit.function.operation.ForwardExtentCopy
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.function.pattern.BlockPattern
import com.sk89q.worldedit.function.pattern.Pattern
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.math.Vector2
import com.sk89q.worldedit.math.Vector3
import com.sk89q.worldedit.math.transform.AffineTransform
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.regions.CylinderRegion
import com.sk89q.worldedit.regions.EllipsoidRegion
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.session.ClipboardHolder
import com.sk89q.worldedit.world.block.BlockState
import com.sk89q.worldedit.world.block.BlockTypes
import org.bukkit.Bukkit
import org.bukkit.World
import xyz.joseg.spigotmcp.config.WorldEditConfig
import xyz.joseg.spigotmcp.util.Cuboid
import xyz.joseg.spigotmcp.util.Pos
import xyz.joseg.spigotmcp.worldedit.BackendDescriptor
import xyz.joseg.spigotmcp.worldedit.BlockPlacement
import xyz.joseg.spigotmcp.worldedit.BlockSpec
import xyz.joseg.spigotmcp.worldedit.WorldEditBackend

class ModernWorldEditBackend(private val config: WorldEditConfig) : WorldEditBackend {

    override val descriptor = BackendDescriptor(
        id = "modern",
        displayName = "WorldEdit 7 / FastAsyncWorldEdit 2.x",
        supportedVersions = "MC 1.13+"
    )

    private val worldEdit: WorldEdit = WorldEdit.getInstance()
    private var clipboard: Clipboard? = null

    override val clipboardVolume: Long
        get() = clipboard?.region?.volume ?: 0L

    override fun setBlocks(region: Cuboid, block: BlockSpec): Int = withSession(region.world) { session ->
        session.setBlocks(cuboid(region) as Region, pattern(block))
    }

    override fun replaceBlocks(region: Cuboid, from: BlockSpec, to: BlockSpec): Int =
        withSession(region.world) { session ->
            val mask = BlockTypeMask(session, resolve(from).blockType)
            session.replaceBlocks(cuboid(region) as Region, mask, pattern(to))
        }

    override fun makeWalls(region: Cuboid, block: BlockSpec): Int = withSession(region.world) { session ->
        session.makeWalls(cuboid(region) as Region, pattern(block))
    }

    override fun makeSphere(center: Pos, radius: Int, block: BlockSpec): Int =
        withSession(center.world) { session ->
            val radii = Vector3.at(radius.toDouble(), radius.toDouble(), radius.toDouble())
            session.setBlocks(EllipsoidRegion(vector(center), radii) as Region, pattern(block))
        }

    override fun makeCylinder(center: Pos, radius: Int, height: Int, block: BlockSpec): Int =
        withSession(center.world) { session ->
            val radii = Vector2.at(radius.toDouble(), radius.toDouble())
            val region = CylinderRegion(vector(center), radii, center.y - height / 2, center.y + height / 2)
            session.setBlocks(region as Region, pattern(block))
        }

    override fun placeBlocks(placements: List<BlockPlacement>): Int {
        var placed = 0
        placements.groupBy { it.pos.world }.forEach { (world, inWorld) ->
            withSession(world) { session ->
                inWorld.forEach { placement ->
                    session.setBlock(vector(placement.pos), resolve(placement.block))
                    placed++
                }
            }
        }
        ModernBlockDecorator.decorate(placements)
        return placed
    }

    override fun copy(region: Cuboid) {
        withSession(region.world) { session ->
            val cuboidRegion = cuboid(region)
            val target = BlockArrayClipboard(cuboidRegion)
            target.origin = cuboidRegion.minimumPoint
            Operations.complete(ForwardExtentCopy(session, cuboidRegion, target, cuboidRegion.minimumPoint))
            clipboard = target
        }
    }

    override fun paste(origin: Pos, rotationDegrees: Int): Int {
        val source = clipboard ?: throw IllegalStateException("Clipboard is empty")
        return withSession(origin.world) { session ->
            val holder = ClipboardHolder(source)
            if (rotationDegrees != 0) {
                holder.transform = AffineTransform().rotateY(-rotationDegrees.toDouble())
            }
            val operation = holder.createPaste(session)
                .to(vector(origin))
                .ignoreAirBlocks(false)
                .copyEntities(false)
                .copyBiomes(false)
                .build()
            Operations.complete(operation)
            source.region.volume.toInt()
        }
    }

    override fun clearClipboard() {
        clipboard = null
    }

    private inline fun <T> withSession(worldName: String, action: (EditSession) -> T): T {
        val world = BukkitAdapter.adapt(bukkitWorld(worldName))
        val session = worldEdit.newEditSessionBuilder()
            .world(world)
            .maxBlocks(config.maxBlocksPerOp)
            .build()
        return try {
            action(session)
        } finally {
            session.close()
        }
    }

    private fun bukkitWorld(name: String): World =
        Bukkit.getWorld(name) ?: throw IllegalArgumentException("World not found: $name")

    private fun cuboid(region: Cuboid) = CuboidRegion(vector(region.min), vector(region.max))

    private fun vector(pos: Pos): BlockVector3 = BlockVector3.at(pos.x, pos.y, pos.z)

    private fun pattern(block: BlockSpec): Pattern = BlockPattern(resolve(block))

    private fun resolve(block: BlockSpec): BlockState {
        if (block.properties.isNotEmpty()) {
            fromBlockData(block.toString())?.let { return it }
        }
        fromBlockType(block)?.let { return it }
        return fromBlockData(block.id)
            ?: throw IllegalArgumentException("Unknown material: ${block.id}")
    }

    private fun fromBlockType(block: BlockSpec): BlockState? =
        (BlockTypes.get(block.id) ?: BlockTypes.get("minecraft:${block.simpleId}"))?.defaultState

    private fun fromBlockData(state: String): BlockState? =
        runCatching { BukkitAdapter.adapt(Bukkit.createBlockData(state)) }.getOrNull()
}
