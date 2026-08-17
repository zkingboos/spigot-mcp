package xyz.joseg.spigotmcp.fawe

import com.sk89q.worldedit.EditSession
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.function.mask.BlockTypeMask
import com.sk89q.worldedit.function.mask.Mask
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.function.pattern.BlockPattern
import com.sk89q.worldedit.function.pattern.Pattern
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.math.Vector2
import com.sk89q.worldedit.math.Vector3
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.regions.CylinderRegion
import com.sk89q.worldedit.regions.EllipsoidRegion
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.session.ClipboardHolder
import com.sk89q.worldedit.session.PasteBuilder
import com.fastasyncworldedit.core.extent.clipboard.WorldCopyClipboard
import com.sk89q.worldedit.world.block.BlockTypes
import org.bukkit.World
import xyz.joseg.spigotmcp.config.FaweConfig
import xyz.joseg.spigotmcp.util.Pos
import kotlin.Result

class FaweAdapter(private val config: FaweConfig) {
    
    private val worldEdit = WorldEdit.getInstance()
    private var clipboardHolder = ClipboardHolder(
        WorldCopyClipboard.of(
            null as com.sk89q.worldedit.extent.Extent?, 
            CuboidRegion(BlockVector3.ZERO, BlockVector3.ZERO), 
            false, 
            false
        )
    )

    fun setBlocks(pos1: Pos, pos2: Pos, material: String): Result<Int> {
        return Result.runCatching { doSetBlocks(pos1, pos2, material) }
    }

    private fun doSetBlocks(pos1: Pos, pos2: Pos, material: String): Int {
        val session = createSession(pos1.world)
        val selRegion = region(pos1, pos2)
        validateRegion(selRegion)
        
        val pattern = parsePattern(material)
        val blocks = selRegion.getArea()
        require(blocks <= config.maxBlocksPerOp) { "Region too large: $blocks > ${config.maxBlocksPerOp}" }
        
        session.setBlocks(selRegion as Region, pattern)
        session.flushQueue()
        return blocks
    }

    fun replaceBlocks(pos1: Pos, pos2: Pos, fromMaterial: String, toMaterial: String): Result<Int> {
        return Result.runCatching { doReplaceBlocks(pos1, pos2, fromMaterial, toMaterial) }
    }

    private fun doReplaceBlocks(pos1: Pos, pos2: Pos, fromMaterial: String, toMaterial: String): Int {
        val session = createSession(pos1.world)
        val selRegion = region(pos1, pos2)
        validateRegion(selRegion)
        
        val fromPattern = parsePattern(fromMaterial)
        val toPattern = parsePattern(toMaterial)
        val fromBlockType = BlockTypes.get(fromMaterial)
            ?: throw IllegalArgumentException("Unknown material: $fromMaterial")
        val mask = BlockTypeMask(session.getWorld(), fromBlockType)
        
        val blocks = selRegion.getArea()
        require(blocks <= config.maxBlocksPerOp) { "Region too large: $blocks > ${config.maxBlocksPerOp}" }
        
        session.replaceBlocks(selRegion as Region, mask, toPattern)
        session.flushQueue()
        return blocks
    }

    fun walls(pos1: Pos, pos2: Pos, material: String): Result<Int> {
        return Result.runCatching { doWalls(pos1, pos2, material) }
    }

    private fun doWalls(pos1: Pos, pos2: Pos, material: String): Int {
        val session = createSession(pos1.world)
        val selRegion = region(pos1, pos2)
        validateRegion(selRegion)
        
        val pattern = parsePattern(material)
        val blocks = selRegion.getArea()
        require(blocks <= config.maxBlocksPerOp) { "Region too large: $blocks > ${config.maxBlocksPerOp}" }
        
        session.makeWalls(selRegion as Region, pattern)
        session.flushQueue()
        return blocks
    }

    fun sphere(center: Pos, radius: Int, material: String): Result<Int> {
        return Result.runCatching { doSphere(center, radius, material) }
    }

    private fun doSphere(center: Pos, radius: Int, material: String): Int {
        val session = createSession(center.world)
        val pattern = parsePattern(material)
        val centerVec = center.toBlockVector3()
        
        val radiusVec = Vector3.at(radius.toDouble(), radius.toDouble(), radius.toDouble())
        val sphereRegion = EllipsoidRegion(centerVec, radiusVec)
        val blocks = sphereRegion.getArea()
        require(blocks <= config.maxBlocksPerOp) { "Region too large: $blocks > ${config.maxBlocksPerOp}" }
        
        session.setBlocks(sphereRegion as Region, pattern)
        session.flushQueue()
        return blocks
    }

    fun cylinder(center: Pos, radius: Int, height: Int, material: String): Result<Int> {
        return Result.runCatching { doCylinder(center, radius, height, material) }
    }

    private fun doCylinder(center: Pos, radius: Int, height: Int, material: String): Int {
        val session = createSession(center.world)
        val pattern = parsePattern(material)
        val centerVec = center.toBlockVector3()
        
        val radiusVec = Vector2.at(radius.toDouble(), radius.toDouble())
        val minY = center.y - height / 2
        val maxY = center.y + height / 2
        val cylinderRegion = CylinderRegion(centerVec, radiusVec, minY, maxY)
        val blocks = cylinderRegion.getArea()
        require(blocks <= config.maxBlocksPerOp) { "Region too large: $blocks > ${config.maxBlocksPerOp}" }
        
        session.setBlocks(cylinderRegion as Region, pattern)
        session.flushQueue()
        return blocks
    }

    fun copy(pos1: Pos, pos2: Pos): Result<Unit> {
        return Result.runCatching { doCopy(pos1, pos2) }
    }

    private fun doCopy(pos1: Pos, pos2: Pos) {
        val session = createSession(pos1.world)
        val selRegion = region(pos1, pos2)
        validateRegion(selRegion)
        
        val clipboard = WorldCopyClipboard.of(session as com.sk89q.worldedit.extent.Extent, selRegion, false, false)
        clipboardHolder = ClipboardHolder(clipboard)
    }

    fun paste(origin: Pos, rotation: Int = 0): Result<Int> {
        return Result.runCatching { doPaste(origin, rotation) }
    }

    private fun doPaste(origin: Pos, rotation: Int): Int {
        val clipboard = clipboardHolder.getClipboard() ?: throw IllegalStateException("Clipboard is empty")
        val session = createSession(origin.world)
        val originVec = origin.toBlockVector3()
        
        val blocks = clipboard.getRegion().getArea()
        require(blocks <= config.maxBlocksPerOp) { "Clipboard too large: $blocks > ${config.maxBlocksPerOp}" }
        
        val pasteBuilder = clipboardHolder.createPaste(session)
            .to(originVec)
            .ignoreAirBlocks(false)
            .copyEntities(false)
            .copyBiomes(false)
        
        val operation = pasteBuilder.build()
        if (operation != null) {
            Operations.complete(operation)
        }
        return blocks
    }

    fun clearClipboard(): Result<Unit> {
        val emptyClipboard = WorldCopyClipboard.of(
            null as com.sk89q.worldedit.extent.Extent?, 
            CuboidRegion(BlockVector3.ZERO, BlockVector3.ZERO), 
            false, 
            false
        )
        clipboardHolder = ClipboardHolder(emptyClipboard)
        return Result.success(Unit)
    }

    fun createSession(worldName: String): EditSession {
        val world = BukkitAdapter.adapt(getBukkitWorld(worldName))
        val session = worldEdit.newEditSessionBuilder()
            .world(world)
            .build()
        return session
    }

    fun getBukkitWorld(worldName: String): World {
        return org.bukkit.Bukkit.getWorld(worldName) 
            ?: throw IllegalArgumentException("World not found: $worldName")
    }

    private fun region(pos1: Pos, pos2: Pos): Region {
        val min = BlockVector3.at(
            minOf(pos1.x, pos2.x), minOf(pos1.y, pos2.y), minOf(pos1.z, pos2.z)
        )
        val max = BlockVector3.at(
            maxOf(pos1.x, pos2.x), maxOf(pos1.y, pos2.y), maxOf(pos1.z, pos2.z)
        )
        return CuboidRegion(min, max)
    }

    private fun validateRegion(region: Region) {
        if (config.requireSelection && region.getArea() == 0) {
            throw IllegalArgumentException("Empty region - pos1 and pos2 must be different")
        }
    }

    fun parsePattern(material: String): Pattern {
        val blockType = BlockTypes.get(material) 
            ?: throw IllegalArgumentException("Unknown material: $material")
        return BlockPattern(blockType.defaultState)
    }

    fun parseBlockStatePattern(stateString: String): Pattern {
        val blockData = org.bukkit.Bukkit.createBlockData(stateString)
        return BlockPattern(BukkitAdapter.adapt(blockData))
    }
}