package xyz.joseg.spigotmcp.mcp.tools.selection

import xyz.joseg.spigotmcp.mcp.tools.block.PosInput

data class SelectionInput(val pos1: PosInput?, val pos2: PosInput?)
data class GetSelectionOutput(val pos1: PosInput?, val pos2: PosInput?)