package xyz.joseg.spigotmcp.config

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

data class PluginConfig(
    val mcp: McpConfig = McpConfig(),
    val worldEdit: WorldEditConfig = WorldEditConfig(),
    val server: ServerConfig = ServerConfig()
)

data class McpConfig(
    val port: Int = 8080,
    val bindAddress: String = "0.0.0.0",
    val stdioEnabled: Boolean = true,
    val httpEnabled: Boolean = true,
    val auth: AuthConfig = AuthConfig()
)

data class AuthConfig(
    val enabled: Boolean = true,
    val token: String = "changeme-in-production",
    val envVar: String = "MCP_AUTH_TOKEN"
)

data class WorldEditConfig(
    val backend: String = BACKEND_AUTO,
    val maxBlocksPerOp: Int = 50_000,
    val requireSelection: Boolean = true,
    val async: Boolean = true
) {
    companion object {
        const val BACKEND_AUTO = "auto"
    }
}

data class ServerConfig(
    val restartDelay: Int = 5,
    val stopDelay: Int = 3
)

object ConfigLoader {

    fun load(pluginFolder: File): PluginConfig {
        val file = File(pluginFolder, "config.yml")
        if (!file.exists()) return PluginConfig()
        return parse(YamlConfiguration.loadConfiguration(file))
    }

    private fun parse(yaml: YamlConfiguration): PluginConfig = PluginConfig(
        mcp = parseMcp(yaml.getConfigurationSection("mcp")),
        worldEdit = parseWorldEdit(
            yaml.getConfigurationSection("worldedit") ?: yaml.getConfigurationSection("fawe")
        ),
        server = parseServer(yaml.getConfigurationSection("server"))
    )

    private fun parseMcp(section: ConfigurationSection?): McpConfig {
        val defaults = McpConfig()
        if (section == null) return defaults
        return McpConfig(
            port = section.getInt("port", defaults.port),
            bindAddress = section.getString("bind-address") ?: defaults.bindAddress,
            stdioEnabled = section.getBoolean("stdio-enabled", defaults.stdioEnabled),
            httpEnabled = section.getBoolean("http-enabled", defaults.httpEnabled),
            auth = AuthConfig(
                enabled = section.getBoolean("auth.enabled", defaults.auth.enabled),
                token = section.getString("auth.token") ?: defaults.auth.token,
                envVar = section.getString("auth.env-var") ?: defaults.auth.envVar
            )
        )
    }

    private fun parseWorldEdit(section: ConfigurationSection?): WorldEditConfig {
        val defaults = WorldEditConfig()
        if (section == null) return defaults
        return WorldEditConfig(
            backend = section.getString("backend") ?: defaults.backend,
            maxBlocksPerOp = section.getInt("max-blocks-per-op", defaults.maxBlocksPerOp),
            requireSelection = section.getBoolean("require-selection", defaults.requireSelection),
            async = section.getBoolean("async", defaults.async)
        )
    }

    private fun parseServer(section: ConfigurationSection?): ServerConfig {
        val defaults = ServerConfig()
        if (section == null) return defaults
        return ServerConfig(
            restartDelay = section.getInt("restart-delay", defaults.restartDelay),
            stopDelay = section.getInt("stop-delay", defaults.stopDelay)
        )
    }
}
