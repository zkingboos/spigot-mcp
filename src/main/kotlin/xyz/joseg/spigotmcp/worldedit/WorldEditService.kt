package xyz.joseg.spigotmcp.worldedit

import xyz.joseg.spigotmcp.config.WorldEditConfig
import xyz.joseg.spigotmcp.util.Cuboid
import xyz.joseg.spigotmcp.util.Pos
import kotlin.math.PI
import kotlin.math.ceil

data class BlockPlacementRequest(
    val pos: Pos,
    val material: String,
    val facing: String? = null
)

class WorldEditService(
    private val backend: WorldEditBackend,
    private val config: WorldEditConfig
) {
    val backendName: String get() = backend.descriptor.displayName

    fun setBlocks(pos1: Pos, pos2: Pos, material: String): Result<Int> = runCatching {
        backend.setBlocks(region(pos1, pos2), block(material))
    }

    fun replaceBlocks(pos1: Pos, pos2: Pos, from: String, to: String): Result<Int> = runCatching {
        backend.replaceBlocks(region(pos1, pos2), block(from), block(to))
    }

    fun walls(pos1: Pos, pos2: Pos, material: String): Result<Int> = runCatching {
        backend.makeWalls(region(pos1, pos2), block(material))
    }

    fun sphere(center: Pos, radius: Int, material: String): Result<Int> = runCatching {
        require(radius > 0) { "Radius must be positive (got $radius)" }
        enforceLimit(sphereVolume(radius), "Sphere")
        backend.makeSphere(center, radius, block(material))
    }

    fun cylinder(center: Pos, radius: Int, height: Int, material: String): Result<Int> = runCatching {
        require(radius > 0) { "Radius must be positive (got $radius)" }
        require(height > 0) { "Height must be positive (got $height)" }
        enforceLimit(cylinderVolume(radius, height), "Cylinder")
        backend.makeCylinder(center, radius, height, block(material))
    }

    fun placeBlocks(requests: List<BlockPlacementRequest>): Result<Int> = runCatching {
        require(requests.isNotEmpty()) { "Blocks list cannot be empty" }
        val placements = requests.flatMap(::plan)
        enforceLimit(placements.size.toLong(), "Batch")
        backend.placeBlocks(placements)
    }

    fun copy(pos1: Pos, pos2: Pos): Result<Unit> = runCatching {
        backend.copy(region(pos1, pos2))
    }

    fun paste(origin: Pos, rotationDegrees: Int = 0): Result<Int> = runCatching {
        val volume = backend.clipboardVolume
        check(volume > 0) { "Clipboard is empty" }
        enforceLimit(volume, "Clipboard")
        backend.paste(origin, normaliseRotation(rotationDegrees))
    }

    fun clearClipboard(): Result<Unit> = runCatching { backend.clearClipboard() }

    private fun plan(request: BlockPlacementRequest): List<BlockPlacement> {
        val facing = request.facing?.let { mapOf(BlockProperties.FACING to it) }.orEmpty()
        val spec = block(request.material).withProperties(facing)

        if (!isDoor(spec)) return listOf(BlockPlacement(request.pos, spec))

        return listOf(
            BlockPlacement(request.pos, spec.withHalf(BlockProperties.HALF_LOWER)),
            BlockPlacement(request.pos.offset(dy = 1), spec.withHalf(BlockProperties.HALF_UPPER))
        )
    }

    private fun BlockSpec.withHalf(half: String) = withProperties(mapOf(BlockProperties.HALF to half))

    private fun isDoor(spec: BlockSpec): Boolean {
        val name = spec.simpleId.substringBefore(':')
        return name.endsWith("_door") || name in LEGACY_DOOR_IDS
    }

    private fun block(material: String): BlockSpec = BlockSpec.parse(material)

    private fun region(pos1: Pos, pos2: Pos): Cuboid {
        val region = Cuboid.of(pos1, pos2)
        if (config.requireSelection) {
            require(region.min != region.max) { "Empty region - pos1 and pos2 must be different" }
        }
        enforceLimit(region.volume, "Region")
        return region
    }

    private fun enforceLimit(blocks: Long, subject: String) {
        require(blocks <= config.maxBlocksPerOp) {
            "$subject too large: $blocks > ${config.maxBlocksPerOp}"
        }
    }

    private fun sphereVolume(radius: Int): Long =
        ceil(4.0 / 3.0 * PI * radius * radius * radius).toLong()

    private fun cylinderVolume(radius: Int, height: Int): Long =
        ceil(PI * radius * radius * height).toLong()

    private fun normaliseRotation(degrees: Int): Int {
        require(degrees % 90 == 0) { "Rotation must be a multiple of 90 degrees (got $degrees)" }
        return ((degrees % 360) + 360) % 360
    }

    private companion object {
        val LEGACY_DOOR_IDS = setOf("wooden_door", "iron_door_block")
    }
}
