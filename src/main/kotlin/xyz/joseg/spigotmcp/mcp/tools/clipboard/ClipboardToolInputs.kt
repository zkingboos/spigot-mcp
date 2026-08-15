package xyz.joseg.spigotmcp.mcp.tools.clipboard

import xyz.joseg.spigotmcp.mcp.tools.block.PosInput

data class CopyInput(val pos1: PosInput, val pos2: PosInput)
data class PasteInput(val origin: PosInput, val rotation: Int = 0)