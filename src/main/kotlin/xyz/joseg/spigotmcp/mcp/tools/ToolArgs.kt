package xyz.joseg.spigotmcp.mcp.tools

import io.modelcontextprotocol.spec.McpSchema
import xyz.joseg.spigotmcp.util.Pos

object ToolArgs {

    fun success(text: String): McpSchema.CallToolResult =
        McpSchema.CallToolResult(listOf(McpSchema.TextContent(text)), false)

    fun failure(text: String): McpSchema.CallToolResult =
        McpSchema.CallToolResult(listOf(McpSchema.TextContent(text)), true)
}

fun <T> Result<T>.toToolResult(describe: (T) -> String): McpSchema.CallToolResult =
    fold(
        onSuccess = { ToolArgs.success(describe(it)) },
        onFailure = { ToolArgs.failure(it.message ?: it.toString()) }
    )

inline fun toolResult(block: () -> McpSchema.CallToolResult): McpSchema.CallToolResult =
    runCatching(block).getOrElse { ToolArgs.failure(it.message ?: it.toString()) }

@Suppress("UNCHECKED_CAST")
fun Map<String, Any>.map(key: String): Map<String, Any> =
    this[key] as? Map<String, Any> ?: throw IllegalArgumentException("Missing object argument '$key'")

@Suppress("UNCHECKED_CAST")
fun Map<String, Any>.mapList(key: String): List<Map<String, Any>> =
    this[key] as? List<Map<String, Any>> ?: throw IllegalArgumentException("Missing array argument '$key'")

fun Map<String, Any>.string(key: String): String =
    this[key] as? String ?: throw IllegalArgumentException("Missing string argument '$key'")

fun Map<String, Any>.stringOrNull(key: String): String? = this[key] as? String

fun Map<String, Any>.int(key: String): Int =
    (this[key] as? Number)?.toInt() ?: throw IllegalArgumentException("Missing integer argument '$key'")

fun Map<String, Any>.intOr(key: String, fallback: Int): Int = (this[key] as? Number)?.toInt() ?: fallback

fun Map<String, Any>.pos(key: String, defaultWorld: String? = null): Pos = map(key).toPos(defaultWorld)

fun Map<String, Any>.toPos(defaultWorld: String? = null): Pos {
    val x = int("x")
    val y = int("y")
    val z = int("z")
    val world = stringOrNull("world") ?: defaultWorld
        ?: throw IllegalArgumentException("Missing world name for position ($x, $y, $z)")
    return Pos(x, y, z, world)
}
