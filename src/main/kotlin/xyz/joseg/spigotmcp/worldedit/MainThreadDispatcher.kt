package xyz.joseg.spigotmcp.worldedit

import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

/**
 * WorldEdit mutates the world through the Bukkit API, and every Paper derived server - including
 * the 1.8 forks - rejects that from anywhere but the main thread ("Asynchronous block remove!").
 *
 * MCP tool calls arrive on HTTP worker threads, so world mutations are handed to the server thread
 * and awaited. Validation stays on the calling thread; only the edit itself is dispatched.
 */
class MainThreadDispatcher(
    private val plugin: Plugin,
    private val timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS
) {

    fun <T> call(block: () -> T): T {
        if (Bukkit.isPrimaryThread()) return block()

        val future = Bukkit.getScheduler().callSyncMethod(plugin, Callable(block))
        return try {
            future.get(timeoutSeconds, TimeUnit.SECONDS)
        } catch (execution: ExecutionException) {
            throw execution.cause ?: execution
        }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_SECONDS = 180L
    }
}
