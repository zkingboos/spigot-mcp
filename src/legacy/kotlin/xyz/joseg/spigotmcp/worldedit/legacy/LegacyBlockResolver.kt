package xyz.joseg.spigotmcp.worldedit.legacy

import com.sk89q.worldedit.blocks.BaseBlock
import com.sk89q.worldedit.blocks.BlockType
import org.bukkit.Material
import xyz.joseg.spigotmcp.worldedit.BlockSpec

internal object LegacyBlockResolver {

    fun resolve(spec: BlockSpec): BaseBlock {
        val (name, explicitData) = split(spec.simpleId)
        val alias = LegacyMaterialAliases.lookup(name)
        val materialName = (alias?.material ?: name).uppercase()

        val id = blockId(materialName)
            ?: throw IllegalArgumentException("Unknown material for this Minecraft version: ${spec.id}")

        val baseData = explicitData ?: alias?.data ?: 0
        return BaseBlock(id, LegacyBlockStates.dataFor(materialName, spec.properties, baseData))
    }

    private fun split(id: String): Pair<String, Int?> {
        val separator = id.lastIndexOf(':')
        if (separator <= 0) return id to null

        val data = id.substring(separator + 1).toIntOrNull() ?: return id to null
        return id.substring(0, separator) to data
    }

    private fun blockId(materialName: String): Int? {
        materialName.toIntOrNull()?.let { return it }

        @Suppress("DEPRECATION")
        Material.matchMaterial(materialName)?.takeIf { it.isBlock }?.let { return it.id }

        return BlockType.lookup(materialName.lowercase())?.id
    }
}
