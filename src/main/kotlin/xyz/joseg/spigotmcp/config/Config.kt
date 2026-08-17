package xyz.joseg.spigotmcp.config

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

data class PluginConfig(
    val mcp: McpConfig = McpConfig(),
    val fawe: FaweConfig = FaweConfig(),
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

data class FaweConfig(
    val maxBlocksPerOp: Int = 50000,
    val requireSelection: Boolean = true,
    val async: Boolean = true
)

data class ServerConfig(
    val restartDelay: Int = 5,
    val stopDelay: Int = 3
)

object ConfigLoader {
    fun load(pluginFolder: File): PluginConfig {
        val file = File(pluginFolder, "config.yml")
        if (!file.exists()) {
            return PluginConfig()
        }
        val yaml = YamlConfiguration.loadConfiguration(file)
        return parseConfig(yaml)
    }

    private fun parseConfig(yaml: YamlConfiguration): PluginConfig {
        val mcp = yaml.getConfigurationSection("mcp") ?: return PluginConfig()
        val fawe = yaml.getConfigurationSection("fawe") ?: return PluginConfig()
        val server = yaml.getConfigurationSection("server") ?: return PluginConfig()

        return PluginConfig(
            mcp = McpConfig(
                port = mcp.getInt("port", 8080),
                bindAddress = mcp.getString("bind-address") ?: "0.0.0.0",
                stdioEnabled = mcp.getBoolean("stdio-enabled", true),
                httpEnabled = mcp.getBoolean("http-enabled", true),
                auth = AuthConfig(
                    enabled = mcp.getBoolean("auth.enabled", true),
                    token = mcp.getString("auth.token") ?: "changeme-in-production",
                    envVar = mcp.getString("auth.env-var") ?: "MCP_AUTH_TOKEN"
                )
            ),
            fawe = FaweConfig(
                maxBlocksPerOp = fawe.getInt("max-blocks-per-op", 50000),
                requireSelection = fawe.getBoolean("require-selection", true),
                async = fawe.getBoolean("async", true)
            ),
            server = ServerConfig(
                restartDelay = server.getInt("restart-delay", 5),
                stopDelay = server.getInt("stop-delay", 3)
            )
        )
    }
}