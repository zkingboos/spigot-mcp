package xyz.joseg.spigotmcp.worldedit

import xyz.joseg.spigotmcp.util.Cuboid
import xyz.joseg.spigotmcp.util.Pos

data class BlockPlacement(val pos: Pos, val block: BlockSpec)

data class BackendDescriptor(
    val id: String,
    val displayName: String,
    val supportedVersions: String
) {
    override fun toString(): String = "$displayName ($supportedVersions)"
}

interface WorldEditBackend {

    val descriptor: BackendDescriptor

    fun setBlocks(region: Cuboid, block: BlockSpec): Int

    fun replaceBlocks(region: Cuboid, from: BlockSpec, to: BlockSpec): Int

    fun makeWalls(region: Cuboid, block: BlockSpec): Int

    fun makeSphere(center: Pos, radius: Int, block: BlockSpec): Int

    fun makeCylinder(center: Pos, radius: Int, height: Int, block: BlockSpec): Int

    fun placeBlocks(placements: List<BlockPlacement>): Int

    fun copy(region: Cuboid)

    fun paste(origin: Pos, rotationDegrees: Int): Int

    fun clearClipboard()

    val clipboardVolume: Long
}
