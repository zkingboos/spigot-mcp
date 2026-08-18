package xyz.joseg.spigotmcp.worldedit

import xyz.joseg.spigotmcp.config.WorldEditConfig
import java.lang.reflect.InvocationTargetException
import java.util.logging.Logger

object WorldEditBackends {

    private data class Candidate(
        val descriptor: BackendDescriptor,
        val probeClass: String,
        val implementationClass: String,
        val faweClass: String
    )

    private val CANDIDATES = listOf(
        Candidate(
            descriptor = BackendDescriptor(
                id = "modern",
                displayName = "WorldEdit 7 / FastAsyncWorldEdit 2.x",
                supportedVersions = "MC 1.13+"
            ),
            probeClass = "com.sk89q.worldedit.math.BlockVector3",
            implementationClass = "xyz.joseg.spigotmcp.worldedit.modern.ModernWorldEditBackend",
            faweClass = "com.fastasyncworldedit.core.Fawe"
        ),
        Candidate(
            descriptor = BackendDescriptor(
                id = "legacy",
                displayName = "WorldEdit 6 / FastAsyncWorldEdit-Reborn",
                supportedVersions = "MC 1.8 - 1.12"
            ),
            probeClass = "com.sk89q.worldedit.Vector",
            implementationClass = "xyz.joseg.spigotmcp.worldedit.legacy.LegacyWorldEditBackend",
            faweClass = "com.boydti.fawe.Fawe"
        )
    )

    val backendIds: List<String> = CANDIDATES.map { it.descriptor.id }

    fun detect(config: WorldEditConfig, logger: Logger): WorldEditBackend? {
        for (candidate in candidatesFor(config.backend)) {
            if (!isPresent(candidate.probeClass)) continue

            val backend = instantiate(candidate, config, logger) ?: continue
            val acceleration = if (isPresent(candidate.faweClass)) "FAWE accelerated" else "plain WorldEdit"
            logger.info("WorldEdit backend: ${candidate.descriptor} - $acceleration")
            return backend
        }

        logger.warning(
            "No supported WorldEdit installation found - block and clipboard tools are disabled. " +
                "Install FastAsyncWorldEdit (MC 1.13+), or FastAsyncWorldEdit-Reborn with WorldEdit 6 (MC 1.8 - 1.12)."
        )
        return null
    }

    private fun candidatesFor(requestedBackend: String): List<Candidate> {
        val requested = requestedBackend.trim().lowercase()
        if (requested == WorldEditConfig.BACKEND_AUTO) return CANDIDATES

        return CANDIDATES.filter { it.descriptor.id == requested }.ifEmpty {
            val accepted = (listOf(WorldEditConfig.BACKEND_AUTO) + backendIds).joinToString(", ")
            throw IllegalArgumentException("Unknown worldedit.backend '$requestedBackend', expected one of: $accepted")
        }
    }

    private fun instantiate(candidate: Candidate, config: WorldEditConfig, logger: Logger): WorldEditBackend? =
        try {
            Class.forName(candidate.implementationClass, true, classLoader())
                .getDeclaredConstructor(WorldEditConfig::class.java)
                .newInstance(config) as WorldEditBackend
        } catch (throwable: Throwable) {
            val cause = (throwable as? InvocationTargetException)?.targetException ?: throwable
            logger.warning("Backend '${candidate.descriptor.id}' matched but failed to initialise: $cause")
            null
        }

    private fun isPresent(className: String): Boolean =
        try {
            Class.forName(className, false, classLoader())
            true
        } catch (_: Throwable) {
            false
        }

    private fun classLoader(): ClassLoader = WorldEditBackends::class.java.classLoader
}
