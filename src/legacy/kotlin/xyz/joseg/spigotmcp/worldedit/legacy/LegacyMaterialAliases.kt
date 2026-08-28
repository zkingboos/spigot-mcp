package xyz.joseg.spigotmcp.worldedit.legacy

internal data class LegacyMaterial(val material: String, val data: Int = 0)

internal object LegacyMaterialAliases {

    private val COLOURS = listOf(
        "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
        "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    )

    private val COLOUR_FAMILIES = mapOf(
        "wool" to "WOOL",
        "stained_glass" to "STAINED_GLASS",
        "stained_glass_pane" to "STAINED_GLASS_PANE",
        "carpet" to "CARPET",
        "terracotta" to "STAINED_CLAY",
        "stained_clay" to "STAINED_CLAY",
        "hardened_clay" to "STAINED_CLAY",
        "glazed_terracotta" to "STAINED_CLAY",
        "concrete" to "STAINED_CLAY",
        "concrete_powder" to "STAINED_CLAY"
    )

    private val WOODS = listOf("oak", "spruce", "birch", "jungle", "acacia", "dark_oak")

    private val WOOD_STAIRS = mapOf(
        "oak" to "WOOD_STAIRS",
        "spruce" to "SPRUCE_WOOD_STAIRS",
        "birch" to "BIRCH_WOOD_STAIRS",
        "jungle" to "JUNGLE_WOOD_STAIRS",
        "acacia" to "ACACIA_STAIRS",
        "dark_oak" to "DARK_OAK_STAIRS"
    )

    private val WOOD_FENCES = mapOf(
        "oak" to "FENCE",
        "spruce" to "SPRUCE_FENCE",
        "birch" to "BIRCH_FENCE",
        "jungle" to "JUNGLE_FENCE",
        "acacia" to "ACACIA_FENCE",
        "dark_oak" to "DARK_OAK_FENCE"
    )

    private val WOOD_FENCE_GATES = mapOf(
        "oak" to "FENCE_GATE",
        "spruce" to "SPRUCE_FENCE_GATE",
        "birch" to "BIRCH_FENCE_GATE",
        "jungle" to "JUNGLE_FENCE_GATE",
        "acacia" to "ACACIA_FENCE_GATE",
        "dark_oak" to "DARK_OAK_FENCE_GATE"
    )

    private val WOOD_DOORS = mapOf(
        "oak" to "WOODEN_DOOR",
        "spruce" to "SPRUCE_DOOR",
        "birch" to "BIRCH_DOOR",
        "jungle" to "JUNGLE_DOOR",
        "acacia" to "ACACIA_DOOR",
        "dark_oak" to "DARK_OAK_DOOR"
    )

    private val ALIASES: Map<String, LegacyMaterial> by lazy {
        buildMap {
            putColourFamilies()
            putWoodFamilies()
            putAll(STONE_FAMILY)
            putAll(SLAB_FAMILY)
            putAll(STAIR_FAMILY)
            putAll(MISCELLANEOUS)
        }
    }

    fun lookup(name: String): LegacyMaterial? = ALIASES[name]

    private fun MutableMap<String, LegacyMaterial>.putColourFamilies() {
        COLOURS.forEachIndexed { index, colour ->
            COLOUR_FAMILIES.forEach { (family, material) ->
                put("${colour}_$family", LegacyMaterial(material, index))
            }
            if (colour == "light_gray") {
                COLOUR_FAMILIES.forEach { (family, material) ->
                    put("silver_$family", LegacyMaterial(material, index))
                }
            }
        }
    }

    private fun MutableMap<String, LegacyMaterial>.putWoodFamilies() {
        WOODS.forEachIndexed { index, wood ->
            val logMaterial = if (index < 4) "LOG" else "LOG_2"
            val leafMaterial = if (index < 4) "LEAVES" else "LEAVES_2"
            val variant = if (index < 4) index else index - 4

            put("${wood}_planks", LegacyMaterial("WOOD", index))
            put("${wood}_log", LegacyMaterial(logMaterial, variant))
            put("${wood}_wood", LegacyMaterial(logMaterial, variant))
            put("stripped_${wood}_log", LegacyMaterial(logMaterial, variant))
            put("${wood}_leaves", LegacyMaterial(leafMaterial, variant))
            put("${wood}_sapling", LegacyMaterial("SAPLING", index))
            put("${wood}_slab", LegacyMaterial("WOOD_STEP", index))
            put("${wood}_stairs", LegacyMaterial(WOOD_STAIRS.getValue(wood)))
            put("${wood}_fence", LegacyMaterial(WOOD_FENCES.getValue(wood)))
            put("${wood}_fence_gate", LegacyMaterial(WOOD_FENCE_GATES.getValue(wood)))
            put("${wood}_door", LegacyMaterial(WOOD_DOORS.getValue(wood)))
            put("${wood}_trapdoor", LegacyMaterial("TRAP_DOOR"))
            put("${wood}_pressure_plate", LegacyMaterial("WOOD_PLATE"))
            put("${wood}_button", LegacyMaterial("WOOD_BUTTON"))
        }
    }

    private val STONE_FAMILY = mapOf(
        "stone" to LegacyMaterial("STONE", 0),
        "smooth_stone" to LegacyMaterial("STONE", 0),
        "granite" to LegacyMaterial("STONE", 1),
        "polished_granite" to LegacyMaterial("STONE", 2),
        "diorite" to LegacyMaterial("STONE", 3),
        "polished_diorite" to LegacyMaterial("STONE", 4),
        "andesite" to LegacyMaterial("STONE", 5),
        "polished_andesite" to LegacyMaterial("STONE", 6),
        "grass_block" to LegacyMaterial("GRASS"),
        "dirt" to LegacyMaterial("DIRT", 0),
        "coarse_dirt" to LegacyMaterial("DIRT", 1),
        "podzol" to LegacyMaterial("DIRT", 2),
        "stone_bricks" to LegacyMaterial("SMOOTH_BRICK", 0),
        "mossy_stone_bricks" to LegacyMaterial("SMOOTH_BRICK", 1),
        "cracked_stone_bricks" to LegacyMaterial("SMOOTH_BRICK", 2),
        "chiseled_stone_bricks" to LegacyMaterial("SMOOTH_BRICK", 3),
        "sandstone" to LegacyMaterial("SANDSTONE", 0),
        "chiseled_sandstone" to LegacyMaterial("SANDSTONE", 1),
        "cut_sandstone" to LegacyMaterial("SANDSTONE", 2),
        "smooth_sandstone" to LegacyMaterial("SANDSTONE", 2),
        "red_sand" to LegacyMaterial("SAND", 1),
        "sand" to LegacyMaterial("SAND", 0),
        "quartz_block" to LegacyMaterial("QUARTZ_BLOCK", 0),
        "chiseled_quartz_block" to LegacyMaterial("QUARTZ_BLOCK", 1),
        "quartz_pillar" to LegacyMaterial("QUARTZ_BLOCK", 2),
        "smooth_quartz" to LegacyMaterial("QUARTZ_BLOCK", 0),
        "prismarine" to LegacyMaterial("PRISMARINE", 0),
        "prismarine_bricks" to LegacyMaterial("PRISMARINE", 1),
        "dark_prismarine" to LegacyMaterial("PRISMARINE", 2)
    )

    private val SLAB_FAMILY = mapOf(
        "stone_slab" to LegacyMaterial("STEP", 0),
        "smooth_stone_slab" to LegacyMaterial("STEP", 0),
        "sandstone_slab" to LegacyMaterial("STEP", 1),
        "cobblestone_slab" to LegacyMaterial("STEP", 3),
        "brick_slab" to LegacyMaterial("STEP", 4),
        "stone_brick_slab" to LegacyMaterial("STEP", 5),
        "nether_brick_slab" to LegacyMaterial("STEP", 6),
        "quartz_slab" to LegacyMaterial("STEP", 7)
    )

    private val STAIR_FAMILY = mapOf(
        "cobblestone_stairs" to LegacyMaterial("COBBLESTONE_STAIRS"),
        "stone_brick_stairs" to LegacyMaterial("SMOOTH_STAIRS"),
        "brick_stairs" to LegacyMaterial("BRICK_STAIRS"),
        "nether_brick_stairs" to LegacyMaterial("NETHER_BRICK_STAIRS"),
        "sandstone_stairs" to LegacyMaterial("SANDSTONE_STAIRS"),
        "red_sandstone_stairs" to LegacyMaterial("SANDSTONE_STAIRS"),
        "quartz_stairs" to LegacyMaterial("QUARTZ_STAIRS")
    )

    private val MISCELLANEOUS = mapOf(
        "bricks" to LegacyMaterial("BRICK"),
        "brick_block" to LegacyMaterial("BRICK"),
        "terracotta" to LegacyMaterial("HARD_CLAY"),
        "hardened_clay" to LegacyMaterial("HARD_CLAY"),
        "glass_pane" to LegacyMaterial("THIN_GLASS"),
        "iron_bars" to LegacyMaterial("IRON_FENCE"),
        "crafting_table" to LegacyMaterial("WORKBENCH"),
        "nether_bricks" to LegacyMaterial("NETHER_BRICK"),
        "nether_brick_fence" to LegacyMaterial("NETHER_FENCE"),
        "end_stone" to LegacyMaterial("ENDER_STONE"),
        "cobweb" to LegacyMaterial("WEB"),
        "iron_door" to LegacyMaterial("IRON_DOOR_BLOCK"),
        "trapdoor" to LegacyMaterial("TRAP_DOOR"),
        "iron_trapdoor" to LegacyMaterial("IRON_TRAPDOOR"),
        "wall_torch" to LegacyMaterial("TORCH"),
        "water" to LegacyMaterial("STATIONARY_WATER"),
        "lava" to LegacyMaterial("STATIONARY_LAVA"),
        "redstone_lamp" to LegacyMaterial("REDSTONE_LAMP_OFF"),
        "note_block" to LegacyMaterial("NOTE_BLOCK"),
        "slime_block" to LegacyMaterial("SLIME_BLOCK"),
        "cobblestone_wall" to LegacyMaterial("COBBLE_WALL", 0),
        "mossy_cobblestone_wall" to LegacyMaterial("COBBLE_WALL", 1),
        "oak_sign" to LegacyMaterial("SIGN_POST"),
        "oak_wall_sign" to LegacyMaterial("WALL_SIGN"),
        "moving_piston" to LegacyMaterial("PISTON_MOVING_PIECE"),
        "piston_head" to LegacyMaterial("PISTON_EXTENSION")
    )
}
