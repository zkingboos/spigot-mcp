package xyz.joseg.spigotmcp.mcp.tools.block

import xyz.joseg.spigotmcp.util.Pos

data class PosInput(val x: Int, val y: Int, val z: Int, val world: String) {
    fun toPos(): Pos = Pos(x, y, z, world)
}

data class RegionInput(val pos1: PosInput, val pos2: PosInput)
data class RegionMaterialInput(val region: RegionInput, val material: String)
data class RegionTwoMaterialInput(val region: RegionInput, val from: String, val to: String)
data class SphereInput(val center: PosInput, val radius: Int, val material: String)
data class CylinderInput(val center: PosInput, val radius: Int, val height: Int, val material: String)