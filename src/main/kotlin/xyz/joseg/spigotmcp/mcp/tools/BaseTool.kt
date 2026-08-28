package xyz.joseg.spigotmcp.mcp.tools

data class ToolDefinition(
    val name: String,
    val description: String,
    val inputSchemaJson: String,
    val execute: (Map<String, Any>) -> xyz.joseg.spigotmcp.mcp.protocol.CallToolResult
)
