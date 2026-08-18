package xyz.joseg.spigotmcp.worldedit.legacy

import com.sk89q.worldedit.EditSession
import com.sk89q.worldedit.Vector
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.blocks.BaseBlock
import com.sk89q.worldedit.bukkit.BukkitUtil
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard
import com.sk89q.worldedit.function.operation.ForwardExtentCopy
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.function.pattern.BlockPattern
import com.sk89q.worldedit.function.pattern.Patterns
import com.sk89q.worldedit.math.transform.AffineTransform
import com.sk89q.worldedit.patterns.Pattern
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.session.ClipboardHolder
import com.sk89q.worldedit.world.World
import com.sk89q.worldedit.world.registry.WorldData
import org.bukkit.Bukkit
import xyz.joseg.spigotmcp.config.WorldEditConfig
import xyz.joseg.spigotmcp.util.Cuboid
import xyz.joseg.spigotmcp.util.Pos
import xyz.joseg.spigotmcp.worldedit.BackendDescriptor
import xyz.joseg.spigotmcp.worldedit.BlockPlacement
import xyz.joseg.spigotmcp.worldedit.BlockSpec
import xyz.joseg.spigotmcp.worldedit.WorldEditBackend

class LegacyWorldEditBackend(private val config: WorldEditConfig) : WorldEditBackend {

    override val descriptor = BackendDescriptor(
        id = "legacy",
        displayName = "WorldEdit 6 / FastAsyncWorldEdit-Reborn",
        supportedVersions = "MC 1.8 - 1.12"
    )

    private val worldEdit: WorldEdit = WorldEdit.getInstance()
    private var clipboard: BlockArrayClipboard? = null
    private var clipboardWorldData: WorldData? = null

    override val clipboardVolume: Long
        get() = clipboard?.region?.area?.toLong() ?: 0L

    override fun setBlocks(region: Cuboid, block: BlockSpec): Int = withSession(region.world) { session ->
        session.setBlocks(cuboid(region) as Region, pattern(block))
    }

    override fun replaceBlocks(region: Cuboid, from: BlockSpec, to: BlockSpec): Int =
        withSession(region.world) { session ->
            session.replaceBlocks(cuboid(region) as Region, setOf(resolve(from)), pattern(to))
        }

    override fun makeWalls(region: Cuboid, block: BlockSpec): Int = withSession(region.world) { session ->
        session.makeWalls(cuboid(region) as Region, pattern(block))
    }

    override fun makeSphere(center: Pos, radius: Int, block: BlockSpec): Int =
        withSession(center.world) { session ->
            session.makeSphere(vector(center), pattern(block), radius.toDouble(), true)
        }

    override fun makeCylinder(center: Pos, radius: Int, height: Int, block: BlockSpec): Int =
        withSession(center.world) { session ->
            val base = vector(center.copy(y = center.y - height / 2))
            session.makeCylinder(base, pattern(block), radius.toDouble(), height, true)
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
        return placed
    }

    override fun copy(region: Cuboid) {
        withSession(region.world) { session ->
            val cuboidRegion = cuboid(region)
            val target = BlockArrayClipboard(cuboidRegion)
            target.origin = cuboidRegion.minimumPoint
            Operations.completeLegacy(ForwardExtentCopy(session, cuboidRegion, target, cuboidRegion.minimumPoint))
            clipboard = target
            clipboardWorldData = session.world.worldData
        }
    }

    override fun paste(origin: Pos, rotationDegrees: Int): Int {
        val source = clipboard ?: throw IllegalStateException("Clipboard is empty")
        return withSession(origin.world) { session ->
            val worldData = clipboardWorldData ?: session.world.worldData
            val holder = ClipboardHolder(source, worldData)
            if (rotationDegrees != 0) {
                holder.transform = AffineTransform().rotateY(-rotationDegrees.toDouble())
            }
            val operation = holder.createPaste(session, session.world.worldData)
                .to(vector(origin))
                .ignoreAirBlocks(false)
                .build()
            Operations.completeLegacy(operation)
            source.region.area
        }
    }

    override fun clearClipboard() {
        clipboard = null
        clipboardWorldData = null
    }

    private inline fun <T> withSession(worldName: String, action: (EditSession) -> T): T {
        val session = worldEdit.editSessionFactory.getEditSession(world(worldName), config.maxBlocksPerOp)
        return try {
            action(session)
        } finally {
            session.flushQueue()
        }
    }

    private fun world(name: String): World {
        val bukkitWorld = Bukkit.getWorld(name) ?: throw IllegalArgumentException("World not found: $name")
        return BukkitUtil.getLocalWorld(bukkitWorld)
    }

    private fun cuboid(region: Cuboid) = CuboidRegion(vector(region.min), vector(region.max))

    private fun vector(pos: Pos): Vector = Vector(pos.x, pos.y, pos.z)

    private fun pattern(block: BlockSpec): Pattern = Patterns.wrap(BlockPattern(resolve(block)))

    private fun resolve(block: BlockSpec): BaseBlock = LegacyBlockResolver.resolve(block)
}
