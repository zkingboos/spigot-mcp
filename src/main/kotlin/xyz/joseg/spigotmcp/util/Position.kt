package xyz.joseg.spigotmcp.util

import com.sk89q.worldedit.math.BlockVector3
import org.bukkit.World

data class Pos(
    val x: Int,
    val y: Int,
    val z: Int,
    val world: String
) {
    fun toBlockVector3(): BlockVector3 = BlockVector3.at(x, y, z)
    
    fun toBukkitLocation(world: World) = org.bukkit.Location(world, x.toDouble(), y.toDouble(), z.toDouble())
    
    companion object {
        fun fromBlockVector3(vec: BlockVector3, worldName: String): Pos = Pos(
            x = vec.getBlockX(), y = vec.getBlockY(), z = vec.getBlockZ(), world = worldName
        )
    }
}