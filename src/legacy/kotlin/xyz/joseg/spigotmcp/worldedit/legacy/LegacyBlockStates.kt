package xyz.joseg.spigotmcp.worldedit.legacy

import xyz.joseg.spigotmcp.worldedit.BlockProperties

internal object LegacyBlockStates {

    fun dataFor(material: String, properties: Map<String, String>, baseData: Int): Int {
        if (properties.isEmpty()) return baseData
        val rule = RULES.firstOrNull { it.matches(material) } ?: return baseData
        return rule.data(properties, baseData)
    }

    private interface Rule {
        fun matches(material: String): Boolean
        fun data(properties: Map<String, String>, baseData: Int): Int
    }

    private val RULES: List<Rule> = listOf(StairRule, DoorRule, TrapDoorRule, SlabRule, LogRule, TorchRule, FacingRule)

    private fun Map<String, String>.facing(): String? = this[BlockProperties.FACING]

    private fun Map<String, String>.half(): String? = this[BlockProperties.HALF]

    private fun Map<String, String>.isTopHalf(): Boolean = half() == "top" || half() == BlockProperties.HALF_UPPER

    private fun Map<String, String>.isOpen(): Boolean = this["open"] == "true"

    private object StairRule : Rule {
        private val FACING = mapOf("east" to 0, "west" to 1, "south" to 2, "north" to 3)

        override fun matches(material: String) = material.endsWith("STAIRS")

        override fun data(properties: Map<String, String>, baseData: Int): Int {
            val facing = FACING[properties.facing()] ?: baseData and 0x3
            return facing or if (properties.isTopHalf()) 4 else 0
        }
    }

    private object DoorRule : Rule {
        private val FACING = mapOf("east" to 0, "south" to 1, "west" to 2, "north" to 3)
        private val MATERIALS = setOf("WOODEN_DOOR", "IRON_DOOR_BLOCK")

        override fun matches(material: String) = material in MATERIALS || material.endsWith("_DOOR")

        override fun data(properties: Map<String, String>, baseData: Int): Int {
            if (properties.half() == BlockProperties.HALF_UPPER) {
                return 8 or if (properties["hinge"] == "right") 1 else 0
            }
            val facing = FACING[properties.facing()] ?: baseData and 0x3
            return facing or if (properties.isOpen()) 4 else 0
        }
    }

    private object TrapDoorRule : Rule {
        private val FACING = mapOf("south" to 0, "north" to 1, "east" to 2, "west" to 3)

        override fun matches(material: String) = material == "TRAP_DOOR" || material == "IRON_TRAPDOOR"

        override fun data(properties: Map<String, String>, baseData: Int): Int {
            val facing = FACING[properties.facing()] ?: baseData and 0x3
            return facing or
                (if (properties.isOpen()) 4 else 0) or
                (if (properties.isTopHalf()) 8 else 0)
        }
    }

    private object SlabRule : Rule {
        private val MATERIALS = setOf("STEP", "WOOD_STEP", "STONE_SLAB2")

        override fun matches(material: String) = material in MATERIALS

        override fun data(properties: Map<String, String>, baseData: Int): Int =
            baseData or if (properties.isTopHalf()) 8 else 0
    }

    private object LogRule : Rule {
        private val AXIS = mapOf("y" to 0, "x" to 4, "z" to 8, "none" to 12)

        override fun matches(material: String) = material == "LOG" || material == "LOG_2"

        override fun data(properties: Map<String, String>, baseData: Int): Int =
            baseData or (AXIS[properties["axis"]] ?: 0)
    }

    private object TorchRule : Rule {
        private val FACING = mapOf("east" to 1, "west" to 2, "south" to 3, "north" to 4, "up" to 5)

        override fun matches(material: String) = material == "TORCH" ||
            material == "REDSTONE_TORCH_ON" || material == "REDSTONE_TORCH_OFF"

        override fun data(properties: Map<String, String>, baseData: Int): Int =
            FACING[properties.facing()] ?: baseData
    }

    private object FacingRule : Rule {
        private val FACING = mapOf("north" to 2, "south" to 3, "west" to 4, "east" to 5)
        private val MATERIALS = setOf(
            "CHEST", "TRAPPED_CHEST", "ENDER_CHEST", "FURNACE", "BURNING_FURNACE",
            "LADDER", "WALL_SIGN", "DISPENSER", "DROPPER", "HOPPER", "PISTON_BASE",
            "PISTON_STICKY_BASE", "WALL_BANNER"
        )

        override fun matches(material: String) = material in MATERIALS

        override fun data(properties: Map<String, String>, baseData: Int): Int =
            FACING[properties.facing()] ?: baseData
    }
}
