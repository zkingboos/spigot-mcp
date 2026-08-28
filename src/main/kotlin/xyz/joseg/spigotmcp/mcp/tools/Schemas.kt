package xyz.joseg.spigotmcp.mcp.tools

object Schemas {

    val POSITION = """
        {
            "type": "object",
            "properties": {
                "x": {"type": "integer"},
                "y": {"type": "integer"},
                "z": {"type": "integer"},
                "world": {"type": "string"}
            },
            "required": ["x", "y", "z", "world"]
        }
    """

    val REGION = """
        {
            "type": "object",
            "properties": {"pos1": $POSITION, "pos2": $POSITION},
            "required": ["pos1", "pos2"]
        }
    """

    val MATERIAL_DESCRIPTION =
        "Block material. Modern names (stone, minecraft:oak_stairs[facing=north]) and 1.8 " +
            "names or ids (WOOL:14, 35:14) are both accepted; the active backend resolves them."
}
