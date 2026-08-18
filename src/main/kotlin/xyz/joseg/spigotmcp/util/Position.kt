package xyz.joseg.spigotmcp.util

import org.bukkit.World

data class Pos(
    val x: Int,
    val y: Int,
    val z: Int,
    val world: String
) {
    fun toBukkitLocation(bukkitWorld: World) =
        org.bukkit.Location(bukkitWorld, x.toDouble(), y.toDouble(), z.toDouble())

    fun offset(dx: Int = 0, dy: Int = 0, dz: Int = 0) = copy(x = x + dx, y = y + dy, z = z + dz)
}

data class Cuboid(val min: Pos, val max: Pos) {

    val world: String get() = min.world

    val volume: Long
        get() = (max.x - min.x + 1).toLong() *
            (max.y - min.y + 1).toLong() *
            (max.z - min.z + 1).toLong()

    companion object {
        fun of(a: Pos, b: Pos): Cuboid {
            require(a.world == b.world) {
                "pos1 and pos2 must be in the same world (got '${a.world}' and '${b.world}')"
            }
            return Cuboid(
                min = Pos(minOf(a.x, b.x), minOf(a.y, b.y), minOf(a.z, b.z), a.world),
                max = Pos(maxOf(a.x, b.x), maxOf(a.y, b.y), maxOf(a.z, b.z), a.world)
            )
        }
    }
}
