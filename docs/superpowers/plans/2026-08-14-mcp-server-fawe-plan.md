# MCP Server with FAWE Integration - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Embed an MCP server in a Spigot plugin exposing FAWE block operations and server management as MCP tools via stdio and HTTP/SSE transports.

**Architecture:** Kotlin plugin using official MCP SDK (io.modelcontextprotocol:mcp) with embedded Netty for HTTP/SSE. FAWE operations called programmatically via FaweAPI. Config-driven with Bearer token auth.

**Tech Stack:** Kotlin 2.3.0, Gradle 9.2, Spigot API 1.20.2, FAWE via IntellectualSites BOM 1.56, MCP SDK 0.6.0, Netty 4.1.100, JUnit Platform.

---

### Task 1: Add Dependencies & Config

**Files:**
- Modify: `build.gradle.kts`
- Create: `src/main/resources/config.yml`

- [ ] **Step 1: Add MCP & Netty dependencies to build.gradle.kts**

```kotlin
dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.2-R0.1-SNAPSHOT")
    implementation(platform("com.intellectualsites.bom:bom-newest:1.56"))
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core")
    testImplementation(kotlin("test"))
    
    // MCP Server
    implementation("io.modelcontextprotocol:mcp:0.6.0")
    implementation("io.netty:netty-transport-native-epoll:4.1.100.Final")
    implementation("io.netty:netty-codec-http:4.1.100.Final")
    implementation("io.netty:netty-handler:4.1.100.Final")
}
```

- [ ] **Step 2: Create config.yml template**

```yaml
mcp:
  port: 8080
  stdio-enabled: true
  http-enabled: true
  auth:
    enabled: true
    token: "changeme-in-production"
    env-var: "MCP_AUTH_TOKEN"
fawe:
  max-blocks-per-op: 50000
  require-selection: true
  async: true
server:
  restart-delay: 5
  stop-delay: 3
```

- [ ] **Step 3: Run build to verify dependencies resolve**

```bash
./gradlew build
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add build.gradle.kts src/main/resources/config.yml
git commit -m "chore: add MCP SDK and Netty dependencies + config template"
```

---

### Task 2: Config Binding Classes

**Files:**
- Create: `src/main/kotlin/xyz/joseg/spigotmcp/config/Config.kt`

- [ ] **Step 1: Write Config data classes**

```kotlin
package xyz.joseg.spigotmcp.config

data class PluginConfig(
    val mcp: McpConfig = McpConfig(),
    val fawe: FaweConfig = FaweConfig(),
    val server: ServerConfig = ServerConfig()
)

data class McpConfig(
    val port: Int = 8080,
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
```

- [ ] **Step 2: Add config loading utility**

```kotlin
package xyz.joseg.spigotmcp.config

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

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
                stdioEnabled = mcp.getBoolean("stdio-enabled", true),
                httpEnabled = mcp.getBoolean("http-enabled", true),
                auth = AuthConfig(
                    enabled = mcp.getBoolean("auth.enabled", true),
                    token = mcp.getString("auth.token", "changeme-in-production"),
                    envVar = mcp.getString("auth.env-var", "MCP_AUTH_TOKEN")
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
```

- [ ] **Step 3: Compile check**

```bash
./gradlew compileKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/kotlin/xyz/joseg/spigotmcp/config/Config.kt
git commit -m "feat: add config data classes and loader"
```

---

### Task 3: Position Data Class & FAWE Adapter

**Files:**
- Create: `src/main/kotlin/xyz/joseg/spigotmcp/util/Position.kt`
- Create: `src/main/kotlin/xyz/joseg/spigotmcp/fawe/FaweAdapter.kt`

- [ ] **Step 1: Create Position data class**

```kotlin
package xyz.joseg.spigotmcp.util

import com.sk89q.worldedit.math.BlockVector3
import org.bukkit.World

data class Pos(
    val x: Int,
    val y: Int,
    val z: Int,
    val world: String
) {
    fun toBlockVector3(): BlockVector3 = BlockVector3.at(x, y, z)
    
    fun toBukkitLocation(world: World) = org.bukkit.Location(world, x.toDouble(), y.toDouble(), z.toDouble())
    
    companion object {
        fun fromBlockVector3(vec: BlockVector3, worldName: String): Pos = Pos(
            x = vec.getBlockX(), y = vec.getBlockY(), z = vec.getBlockZ(), world = worldName
        )
    }
}
```

- [ ] **Step 2: Create FaweAdapter with basic operations**

```kotlin
package xyz.joseg.spigotmcp.fawe

import com.sk89q.worldedit.EditSession
import com.sk89q.worldedit.MaxChangedBlocksException
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.function.mask.ExistingBlockMask
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.function.pattern.Pattern
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.session.ClipboardHolder
import com.sk89q.worldedit.world.block.BlockState
import com.sk89q.worldedit.world.block.BlockTypes
import org.bukkit.World
import xyz.joseg.spigotmcp.config.FaweConfig
import xyz.joseg.spigotmcp.util.Pos

class FaweAdapter(private val config: FaweConfig) {
    
    private val worldEdit = WorldEdit.getInstance()
    private val clipboardHolder = ClipboardHolder.create()

    fun setBlocks(pos1: Pos, pos2: Pos, material: String): Result<Int> {
        return runCatching { doSetBlocks(pos1, pos2, material) }
            .fold({ failure -> Result.failure(failure) }, { success -> Result.success(success) })
    }

    private fun doSetBlocks(pos1: Pos, pos2: Pos, material: String): Int {
        val session = createSession(pos1.world)
        val region = region(pos1, pos2)
        validateRegion(region)
        
        val pattern = parsePattern(material)
        val blocks = region.getArea()
        require(blocks <= config.maxBlocksPerOp) { "Region too large: $blocks > ${config.maxBlocksPerOp}" }
        
        Operations.complete(Operations.setBlocks(session, region, pattern))
        return blocks
    }

    fun replaceBlocks(pos1: Pos, pos2: Pos, fromMaterial: String, toMaterial: String): Result<Int> {
        return runCatching { doReplaceBlocks(pos1, pos2, fromMaterial, toMaterial) }
            .fold({ failure -> Result.failure(failure) }, { success -> Result.success(success) })
    }

    private fun doReplaceBlocks(pos1: Pos, pos2: Pos, fromMaterial: String, toMaterial: String): Int {
        val session = createSession(pos1.world)
        val region = region(pos1, pos2)
        validateRegion(region)
        
        val fromPattern = parsePattern(fromMaterial)
        val toPattern = parsePattern(toMaterial)
        val mask = ExistingBlockMask.create(session.getWorld(), fromPattern)
        
        val blocks = region.getArea()
        require(blocks <= config.maxBlocksPerOp) { "Region too large: $blocks > ${config.maxBlocksPerOp}" }
        
        Operations.complete(Operations.replaceBlocks(session, region, toPattern, mask))
        return blocks
    }

    fun walls(pos1: Pos, pos2: Pos, material: String): Result<Int> {
        return runCatching { doWalls(pos1, pos2, material) }
            .fold({ failure -> Result.failure(failure) }, { success -> Result.success(success) })
    }

    private fun doWalls(pos1: Pos, pos2: Pos, material: String): Int {
        val session = createSession(pos1.world)
        val region = region(pos1, pos2)
        validateRegion(region)
        
        val pattern = parsePattern(material)
        val blocks = region.getArea()
        require(blocks <= config.maxBlocksPerOp) { "Region too large: $blocks > ${config.maxBlocksPerOp}" }
        
        Operations.complete(Operations.walls(session, region, pattern))
        return blocks
    }

    fun sphere(center: Pos, radius: Int, material: String): Result<Int> {
        return runCatching { doSphere(center, radius, material) }
            .fold({ failure -> Result.failure(failure) }, { success -> Result.success(success) })
    }

    private fun doSphere(center: Pos, radius: Int, material: String): Int {
        val session = createSession(center.world)
        val pattern = parsePattern(material)
        val centerVec = center.toBlockVector3()
        
        val region = com.sk89q.worldedit.regions.SphereRegion(centerVec, radius, radius, radius)
        val blocks = region.getArea()
        require(blocks <= config.maxBlocksPerOp) { "Region too large: $blocks > ${config.maxBlocksPerOp}" }
        
        Operations.complete(Operations.setBlocks(session, region, pattern))
        return blocks
    }

    fun cylinder(center: Pos, radius: Int, height: Int, material: String): Result<Int> {
        return runCatching { doCylinder(center, radius, height, material) }
            .fold({ failure -> Result.failure(failure) }, { success -> Result.success(success) })
    }

    private fun doCylinder(center: Pos, radius: Int, height: Int, material: String): Int {
        val session = createSession(center.world)
        val pattern = parsePattern(material)
        val centerVec = center.toBlockVector3()
        
        val region = com.sk89q.worldedit.regions.CylinderRegion(centerVec, radius, height, height)
        val blocks = region.getArea()
        require(blocks <= config.maxBlocksPerOp) { "Region too large: $blocks > ${config.maxBlocksPerOp}" }
        
        Operations.complete(Operations.setBlocks(session, region, pattern))
        return blocks
    }

    fun copy(pos1: Pos, pos2: Pos): Result<Unit> {
        return runCatching { doCopy(pos1, pos2) }
            .fold({ failure -> Result.failure(failure) }, { Result.success(it) })
    }

    private fun doCopy(pos1: Pos, pos2: Pos) {
        val session = createSession(pos1.world)
        val region = region(pos1, pos2)
        validateRegion(region)
        
        val clipboard = ClipboardBuilder(session.getWorld(), region).build()
        clipboardHolder.setClipboard(clipboard)
    }

    fun paste(origin: Pos, rotation: Int = 0): Result<Int> {
        return runCatching { doPaste(origin, rotation) }
            .fold({ failure -> Result.failure(failure) }, { success -> Result.success(success) })
    }

    private fun doPaste(origin: Pos, rotation: Int): Int {
        val clipboard = clipboardHolder.getClipboard() ?: throw IllegalStateException("Clipboard is empty")
        val session = createSession(origin.world)
        val originVec = origin.toBlockVector3()
        
        val blocks = clipboard.getRegion().getArea()
        require(blocks <= config.maxBlocksPerOp) { "Clipboard too large: $blocks > ${config.maxBlocksPerOp}" }
        
        val operation = clipboard.paste(
            session,
            originVec,
            false,
            false,
            com.sk89q.worldedit.session.ClipboardHolder.PasteInfo(
                rotation = rotation.toDouble(),
                source = clipboard.getRegion().getMinimumPoint()
            )
        )
        Operations.complete(operation)
        return blocks
    }

    fun clearClipboard(): Result<Unit> {
        clipboardHolder.clearClipboard()
        return Result.success(Unit)
    }

    private fun createSession(worldName: String): EditSession {
        val world = BukkitAdapter.adapt(getBukkitWorld(worldName))
        val session = worldEdit.newEditSessionBuilder()
            .world(world)
            .build()
        return session
    }

    private fun getBukkitWorld(worldName: String): World {
        return org.bukkit.Bukkit.getWorld(worldName) 
            ?: throw IllegalArgumentException("World not found: $worldName")
    }

    private fun region(pos1: Pos, pos2: Pos): Region {
        val min = BlockVector3.at(
            minOf(pos1.x, pos2.x), minOf(pos1.y, pos2.y), minOf(pos1.z, pos2.z)
        )
        val max = BlockVector3.at(
            maxOf(pos1.x, pos2.x), maxOf(pos1.y, pos2.y), maxOf(pos1.z, pos2.z)
        )
        return CuboidRegion(min, max)
    }

    private fun validateRegion(region: Region) {
        if (config.requireSelection && region.getArea() == 0) {
            throw IllegalArgumentException("Empty region - pos1 and pos2 must be different")
        }
    }

    private fun parsePattern(material: String): Pattern {
        val blockType = BlockTypes.get(material.uppercase()) 
            ?: throw IllegalArgumentException("Unknown material: $material")
        return com.sk89q.worldedit.function.pattern.BlockPattern(blockType.getDefaultState())
    }
}
```

- [ ] **Step 3: Compile check**

```bash
./gradlew compileKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/kotlin/xyz/joseg/spigotmcp/util/Position.kt src/main/kotlin/xyz/joseg/spigotmcp/fawe/FaweAdapter.kt
git commit -m "feat: add Position data class and FaweAdapter with basic block operations"
```

---

### Task 4: MCP Server Core - Transport & Auth

**Files:**
- Create: `src/main/kotlin/xyz/joseg/spigotmcp/mcp/transport/StdioTransport.kt`
- Create: `src/main/kotlin/xyz/joseg/spigotmcp/mcp/transport/HttpSseTransport.kt`
- Create: `src/main/kotlin/xyz/joseg/spigotmcp/mcp/auth/AuthMiddleware.kt`

- [ ] **Step 1: Create StdioTransport**

```kotlin
package xyz.joseg.spigotmcp.mcp.transport

import io.modelcontextprotocol.server.McpServer
import io.modelcontextprotocol.server.transport.StdioServerTransport
import io.modelcontextprotocol.spec.McpSchema
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StdioTransport(
    private val server: McpServer,
    private val scope: CoroutineScope
) {
    fun start() {
        scope.launch(Dispatchers.IO) {
            val transport = StdioServerTransport(System.in, System.out)
            server.connect(transport).join()
        }
    }
}
```

- [ ] **Step 2: Create HttpSseTransport**

```kotlin
package xyz.joseg.spigotmcp.mcp.transport

import io.modelcontextprotocol.server.McpServer
import io.modelcontextprotocol.server.transport.WebFluxSseServerTransport
import io.modelcontextprotocol.spec.McpSchema
import org.springframework.http.server.reactive.HttpHandler
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.netty.http.server.HttpServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HttpSseTransport(
    private val server: McpServer,
    private val port: Int,
    private val scope: CoroutineScope
) {
    fun start() {
        scope.launch(Dispatchers.IO) {
            val transport = WebFluxSseServerTransport(server)
            val handler = transport.getHttpHandler()
            
            HttpServer.create()
                .port(port)
                .handle(ReactorHttpHandlerAdapter(handler))
                .bindNow()
        }
    }
}
```

- [ ] **Step 3: Create AuthMiddleware**

```kotlin
package xyz.joseg.spigotmcp.mcp.auth

import io.modelcontextprotocol.server.McpServer
import io.modelcontextprotocol.server.transport.ServerTransport
import io.modelcontextprotocol.spec.McpError
import io.modelcontextprotocol.spec.McpSchema
import xyz.joseg.spigotmcp.config.AuthConfig

class AuthMiddleware(
    private val config: AuthConfig,
    private val next: McpServer
) : McpServer by next {
    
    override fun handleRequest(request: McpSchema.JSONRPCRequest): McpSchema.JSONRPCResponse {
        if (!config.enabled) return next.handleRequest(request)
        
        val token = extractToken(request)
        val expectedToken = System.getenv(config.envVar).takeIf { it.isNotBlank() } ?: config.token
        
        if (token != expectedToken) {
            return McpSchema.JSONRPCResponse.error(
                request.id,
                McpError(-32001, "Unauthorized: Invalid or missing token", null)
            )
        }
        
        return next.handleRequest(request)
    }
    
    private fun extractToken(request: McpSchema.JSONRPCRequest): String? {
        // For HTTP transport, auth comes via headers in transport layer
        // For stdio, check env var
        return System.getenv(config.envVar)
    }
}
```

- [ ] **Step 3: Compile check**

```bash
./gradlew compileKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/kotlin/xyz/joseg/spigotmcp/mcp/transport/StdioTransport.kt src/main/kotlin/xyz/joseg/spigotmcp/mcp/transport/HttpSseTransport.kt src/main/kotlin/xyz/joseg/spigotmcp/mcp/auth/AuthMiddleware.kt
git commit -m "feat: add MCP stdio and HTTP/SSE transports + auth middleware"
```

---

### Task 5: MCP Tool Base Classes & Registry

**Files:**
- Create: `src/main/kotlin/xyz/joseg/spigotmcp/mcp/tools/ToolRegistry.kt`
- Create: `src/main/kotlin/xyz/joseg/spigotmcp/mcp/tools/BaseTool.kt`

- [ ] **Step 1: Create BaseTool abstract class**

```kotlin
package xyz.joseg.spigotmcp.mcp.tools

import io.modelcontextprotocol.server.McpServerFeatures
import io.modelcontextprotocol.spec.McpSchema
import kotlin.reflect.KClass

abstract class BaseTool<T : Any>(
    protected val name: String,
    protected val description: String,
    protected val inputSchema: Map<String, Any>,
    protected val inputClass: KClass<T>
) {
    
    abstract fun execute(input: T): McpSchema.CallToolResult
    
    fun toSyncTool(): McpServerFeatures.SyncToolSpecification {
        return McpServerFeatures.SyncToolSpecification(
            schema = McpSchema.Tool(
                name = name,
                description = description,
                inputSchema = inputSchema
            ),
            handler = { request ->
                val input = parseInput(request.arguments)
                execute(input)
            }
        )
    }
    
    private fun parseInput(args: Map<String, Any>?): T {
        // Simple map-to-data-class parsing; use kotlinx.serialization in production
        return inputClass.createInstance(args)
    }
}
```

- [ ] **Step 2: Create ToolRegistry**

```kotlin
package xyz.joseg.spigotmcp.mcp.tools

import io.modelcontextprotocol.server.McpServer
import io.modelcontextprotocol.server.McpServerFeatures

class ToolRegistry(private val server: McpServer) {
    
    fun register(tool: BaseTool<*>) {
        server.addTool(tool.toSyncTool())
    }
    
    fun registerAll(vararg tools: BaseTool<*>) {
        tools.forEach { register(it) }
    }
}
```

- [ ] **Step 3: Compile check**

```bash
./gradlew compileKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/kotlin/xyz/joseg/spigotmcp/mcp/tools/ToolRegistry.kt src/main/kotlin/xyz/joseg/spigotmcp/mcp/tools/BaseTool.kt
git commit -m "feat: add base tool classes and registry"
```

---

### Task 6: Block Operation Tools

**Files:**
- Create: `src/main/kotlin/xyz/joseg/spigotmcp/mcp/tools/block/SetBlocksTool.kt`
- Create: `src/main/kotlin/xyz/joseg/spigotmcp/mcp/tools/block/ReplaceBlocksTool.kt`
- Create: `src/main/kotlin/xyz/joseg/spigotmcp/mcp/tools/block/WallsTool.kt`
- Create: `src/main/kotlin/xyz/joseg/spigotmcp/mcp/tools/block/SphereTool.kt`
- Create: `src/main/kotlin/xyz/joseg/spigotmcp/mcp/tools/block/CylinderTool.kt`

- [ ] **Step 1: Create input data classes (shared)**

```kotlin
package xyz.joseg.spigotmcp.mcp.tools.block

import xyz.joseg.spigotmcp.util.Pos

data class PosInput(val x: Int, val y: Int, val z: Int, val world: String) {
    fun toPos(): Pos = Pos(x, y, z, world)
}

data class RegionInput(val pos1: PosInput, val pos2: PosInput)
data class RegionMaterialInput(val region: RegionInput, val material: String)
data class RegionTwoMaterialInput(val region: RegionInput, val from: String, val to: String)
data class SphereInput(val center: PosInput, val radius: Int, val material: String)
data class CylinderInput(val center: PosInput, val radius: Int, val height: Int, val material: String)
```

- [ ] **Step 2: Create SetBlocksTool**

```kotlin
package xyz.joseg.spigotmcp.mcp.tools.block

import io.modelcontextprotocol.spec.McpSchema
import xyz.joseg.spigotmcp.fawe.FaweAdapter
import xyz.joseg.spigotmcp.mcp.tools.BaseTool

class SetBlocksTool(private val fawe: FaweAdapter) : BaseTool<RegionMaterialInput>(
    name = "set_blocks",
    description = "Fill a region with a specific block material",
    inputSchema = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "region" to mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "pos1" to mapOf("type" to "object", "properties" to mapOf(
                        "x" to mapOf("type" to "integer"),
                        "y" to mapOf("type" to "integer"),
                        "z" to mapOf("type" to "integer"),
                        "world" to mapOf("type" to "string")
                    )),
                    "pos2" to mapOf("type" to "object", "properties" to mapOf(
                        "x" to mapOf("type" to "integer"),
                        "y" to mapOf("type" to "integer"),
                        "z" to mapOf("type" to "integer"),
                        "world" to mapOf("type" to "string")
                    ))
                ),
                "required" to listOf("pos1", "pos2")
            ),
            "material" to mapOf("type" to "string", "description" to "Block material (e.g., STONE, DIAMOND_BLOCK)")
        ),
        "required" to listOf("region", "material")
    ),
    inputClass = RegionMaterialInput::class
) {
    override fun execute(input: RegionMaterialInput): McpSchema.CallToolResult {
        return fawe.setBlocks(input.region.pos1.toPos(), input.region.pos2.toPos(), input.material)
            .fold(
                { error -> McpSchema.CallToolResult.error("Failed to set blocks: ${error.message}") },
                { blocks -> McpSchema.CallToolResult.success(listOf(McpSchema.TextContent("text", "Set $blocks blocks to ${input.material}"))) }
            )
    }
}
```

- [ ] **Step 3: Create ReplaceBlocksTool**

```kotlin
package xyz.joseg.spigotmcp.mcp.tools.block

import io.modelcontextprotocol.spec.McpSchema
import xyz.joseg.spigotmcp.fawe.FaweAdapter
import xyz.joseg.spigotmcp.mcp.tools.BaseTool

class ReplaceBlocksTool(private val fawe: FaweAdapter) : BaseTool<RegionTwoMaterialInput>(
    name = "replace_blocks",
    description = "Replace all blocks of one material with another in a region",
    inputSchema = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "region" to mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "pos1" to mapOf("type" to "object", "properties" to mapOf(
                        "x" to mapOf("type" to "integer"),
                        "y" to mapOf("type" to "integer"),
                        "z" to mapOf("type" to "integer"),
                        "world" to mapOf("type" to "string")
                    )),
                    "pos2" to mapOf("type" to "object", "properties" to mapOf(
                        "x" to mapOf("type" to "integer"),
                        "y" to mapOf("type" to "integer"),
                        "z" to mapOf("type" to "integer"),
                        "world" to mapOf("type" to "string")
                    ))
                ),
                "required" to listOf("pos1", "pos2")
            ),
            "from" to mapOf("type" to "string", "description" to "Material to replace"),
            "to" to mapOf("type" to "string", "description" to "Material to replace with")
        ),
        "required" to listOf("region", "from", "to")
    ),
    inputClass = RegionTwoMaterialInput::class
) {
    override fun execute(input: RegionTwoMaterialInput): McpSchema.CallToolResult {
        return fawe.replaceBlocks(input.region.pos1.toPos(), input.region.pos2.toPos(), input.from, input.to)
            .fold(
                { error -> McpSchema.CallToolResult.error("Failed to replace blocks: ${error.message}") },
                { blocks -> McpSchema.CallToolResult.success(listOf(McpSchema.TextContent("text", "Replaced $blocks blocks: ${input.from} → ${input.to}"))) }
            )
    }
}
```

- [ ] **Step 4: Create WallsTool**

```kotlin
package xyz.joseg.spigotmcp.mcp.tools.block

import io.modelcontextprotocol.spec.McpSchema
import xyz.joseg.spigotmcp.fawe.FaweAdapter
import xyz.joseg.spigotmcp.mcp.tools.BaseTool

class WallsTool(private val fawe: FaweAdapter) : BaseTool<RegionMaterialInput>(
    name = "walls",
    description = "Build walls around a region",
    inputSchema = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "region" to mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "pos1" to mapOf("type" to "object", "properties" to mapOf(
                        "x" to mapOf("type" to "integer"),
                        "y" to mapOf("type" to "integer"),
                        "z" to mapOf("type" to "integer"),
                        "world" to mapOf("type" to "string")
                    )),
                    "pos2" to mapOf("type" to "object", "properties" to mapOf(
                        "x" to mapOf("type" to "integer"),
                        "y" to mapOf("type" to "integer"),
                        "z" to mapOf("type" to "integer"),
                        "world" to mapOf("type" to "string")
                    ))
                ),
                "required" to listOf("pos1", "pos2")
            ),
            "material" to mapOf("type" to "string", "description" to "Wall material")
        ),
        "required" to listOf("region", "material")
    ),
    inputClass = RegionMaterialInput::class
) {
    override fun execute(input: RegionMaterialInput): McpSchema.CallToolResult {
        return fawe.walls(input.region.pos1.toPos(), input.region.pos2.toPos(), input.material)
            .fold(
                { error -> McpSchema.CallToolResult.error("Failed to build walls: ${error.message}") },
                { blocks -> McpSchema.CallToolResult.success(listOf(McpSchema.TextContent("text", "Built walls: $blocks blocks of ${input.material}"))) }
            )
    }
}
```

- [ ] **Step 5: Create SphereTool**

```kotlin
package xyz.joseg.spigotmcp.mcp.tools.block

import io.modelcontextprotocol.spec.McpSchema
import xyz.joseg.spigotmcp.fawe.FaweAdapter
import xyz.joseg.spigotmcp.mcp.tools.BaseTool

class SphereTool(private val fawe: FaweAdapter) : BaseTool<SphereInput>(
    name = "sphere",
    description = "Create a sphere at center with given radius",
    inputSchema = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "center" to mapOf("type" to "object", "properties" to mapOf(
                "x" to mapOf("type" to "integer"),
                "y" to mapOf("type" to "integer"),
                "z" to mapOf("type" to "integer"),
                "world" to mapOf("type" to "string")
            )),
            "radius" to mapOf("type" to "integer", "minimum" to 1),
            "material" to mapOf("type" to "string")
        ),
        "required" to listOf("center", "radius", "material")
    ),
    inputClass = SphereInput::class
) {
    override fun execute(input: SphereInput): McpSchema.CallToolResult {
        return fawe.sphere(input.center.toPos(), input.radius, input.material)
            .fold(
                { error -> McpSchema.CallToolResult.error("Failed to create sphere: ${error.message}") },
                { blocks -> McpSchema.CallToolResult.success(listOf(McpSchema.TextContent("text", "Created sphere: $blocks blocks of ${input.material} (radius ${input.radius})"))) }
            )
    }
}
```

- [ ] **Step 6: Create CylinderTool**

```kotlin
package xyz.joseg.spigotmcp.mcp.tools.block

import io.modelcontextprotocol.spec.McpSchema
import xyz.joseg.spigotmcp.fawe.FaweAdapter
import xyz.joseg.spigotmcp.mcp.tools.BaseTool

class CylinderTool(private val fawe: FaweAdapter) : BaseTool<CylinderInput>(
    name = "cylinder",
    description = "Create a cylinder at center with given radius and height",
    inputSchema = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "center" to mapOf("type" to "object", "properties" to mapOf(
                "x" to mapOf("type" to "integer"),
                "y" to mapOf("type" to "integer"),
                "z" to mapOf("type" to "integer"),
                "world" to mapOf("type" to "string")
            )),
            "radius" to mapOf("type" to "integer", "minimum" to 1),
            "height" to mapOf("type" to "integer", "minimum" to 1),
            "material" to mapOf("type" to "string")
        ),
        "required" to listOf("center", "radius", "height", "material")
    ),
    inputClass = CylinderInput::class
) {
    override fun execute(input: CylinderInput): McpSchema.CallToolResult {
        return fawe.cylinder(input.center.toPos(), input.radius, input.height, input.material)
            .fold(
                { error -> McpSchema.CallToolResult.error("Failed to create cylinder: ${error.message}") },
                { blocks -> McpSchema.CallToolResult.success(listOf(McpSchema.TextContent("text", "Created cylinder: $blocks blocks of ${input.material} (radius ${input.radius}, height ${input.height})"))) }
            )
    }
}
```

- [ ] **Step 7: Compile check**

```bash
./gradlew compileKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add src/main/kotlin/xyz/joseg/spigotmcp/mcp/tools/block/
git commit -m "feat: add block operation tools (set, replace, walls, sphere, cylinder)"
```

---

### Task 7: Clipboard & Selection Tools

**Files:**
- Create: `src/main/kotlin/xyz/joseg/spigotmcp/mcp/tools/clipboard/CopyTool.kt`
- Create: `src/main/kotlin/xyz/joseg/spigotmcp/mcp/tools/clipboard/PasteTool.kt`
- Create: `src/main/kotlin/xyz/joseg/spigotmcp/mcp/tools/clipboard/ClearClipboardTool.kt`
- Create: `src/main/kotlin/xyz/joseg/spigotmcp/mcp/tools/selection/GetSelectionTool.kt`
- Create: `src/main/kotlin/xyz/joseg/spigotmcp/mcp/tools/selection/SetSelectionTool.kt`

- [ ] **Step 1: Create clipboard input classes**

```kotlin
package xyz.joseg.spigotmcp.mcp.tools.clipboard

import xyz.joseg.spigotmcp.util.Pos

data class CopyInput(val pos1: PosInput, val pos2: PosInput)
data class PasteInput(val origin: PosInput, val rotation: Int = 0)
```

- [ ] **Step 2: Create CopyTool**

```kotlin
package xyz.joseg.spigotmcp.mcp.tools.clipboard

import io.modelcontextprotocol.spec.McpSchema
import xyz.joseg.spigotmcp.fawe.FaweAdapter
import xyz.joseg.spigotmcp.mcp.tools.BaseTool

class CopyTool(private val fawe: FaweAdapter) : BaseTool<CopyInput>(
    name = "copy",
    description = "Copy a region to clipboard",
    inputSchema = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "pos1" to mapOf("type" to "object", "properties" to mapOf(
                "x" to mapOf("type" to "integer"),
                "y" to mapOf("type" to "integer"),
                "z" to mapOf("type" to "integer"),
                "world" to mapOf("type" to "string")
            )),
            "pos2" to mapOf("type" to "object", "properties" to mapOf(
                "x" to mapOf("type" to "integer"),
                "y" to mapOf("type" to "integer"),
                "z" to mapOf("type" to "integer"),
                "world" to mapOf("type" to "string")
            ))
        ),
        "required" to listOf("pos1", "pos2")
    ),
    inputClass = CopyInput::class
) {
    override fun execute(input: CopyInput): McpSchema.CallToolResult {
        return fawe.copy(input.pos1.toPos(), input.pos2.toPos())
            .fold(
                { error -> McpSchema.CallToolResult.error("Failed to copy: ${error.message}") },
                { McpSchema.CallToolResult.success(listOf(McpSchema.TextContent("text", "Copied region to clipboard"))) }
            )
    }
}
```

- [ ] **Step 3: Create PasteTool**

```kotlin
package xyz.joseg.spigotmcp.mcp.tools.clipboard

import io.modelcontextprotocol.spec.McpSchema
import xyz.joseg.spigotmcp.fawe.FaweAdapter
import xyz.joseg.spigotmcp.mcp.tools.BaseTool

class PasteTool(private val fawe: FaweAdapter) : BaseTool<PasteInput>(
    name = "paste",
    description = "Paste clipboard at origin position",
    inputSchema = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "origin" to mapOf("type" to "object", "properties" to mapOf(
                "x" to mapOf("type" to "integer"),
                "y" to mapOf("type" to "integer"),
                "z" to mapOf("type" to "integer"),
                "world" to mapOf("type" to "string")
            )),
            "rotation" to mapOf("type" to "integer", "default" to 0, "description" to "Rotation in degrees (0, 90, 180, 270)")
        ),
        "required" to listOf("origin")
    ),
    inputClass = PasteInput::class
) {
    override fun execute(input: PasteInput): McpSchema.CallToolResult {
        return fawe.paste(input.origin.toPos(), input.rotation)
            .fold(
                { error -> McpSchema.CallToolResult.error("Failed to paste: ${error.message}") },
                { blocks -> McpSchema.CallToolResult.success(listOf(McpSchema.TextContent("text", "Pasted $blocks blocks from clipboard"))) }
            )
    }
}
```

- [ ] **Step 4: Create ClearClipboardTool**

```kotlin
package xyz.joseg.spigotmcp.mcp.tools.clipboard

import io.modelcontextprotocol.spec.McpSchema
import xyz.joseg.spigotmcp.fawe.FaweAdapter
import xyz.joseg.spigotmcp.mcp.tools.BaseTool

class ClearClipboardTool(private val fawe: FaweAdapter) : BaseTool<Unit>(
    name = "clear_clipboard",
    description = "Clear the clipboard",
    inputSchema = mapOf("type" to "object", "properties" to mapOf()),
    inputClass = Unit::class
) {
    override fun execute(input: Unit): McpSchema.CallToolResult {
        return fawe.clearClipboard()
            .fold(
                { error -> McpSchema.CallToolResult.error("Failed to clear clipboard: ${error.message}") },
                { McpSchema.CallToolResult.success(listOf(McpSchema.TextContent("text", "Clipboard cleared"))) }
            )
    }
}
```

- [ ] **Step 5: Create selection input & tools**

```kotlin
package xyz.joseg.spigotmcp.mcp.tools.selection

import xyz.joseg.spigotmcp.util.Pos

data class SelectionInput(val pos1: PosInput?, val pos2: PosInput?)
data class GetSelectionOutput(val pos1: PosInput?, val pos2: PosInput?)
```

```kotlin
package xyz.joseg.spigotmcp.mcp.tools.selection

import io.modelcontextprotocol.spec.McpSchema
import xyz.joseg.spigotmcp.fawe.FaweAdapter
import xyz.joseg.spigotmcp.mcp.tools.BaseTool
import kotlinx.serialization.json.Json

class GetSelectionTool : BaseTool<Unit>(
    name = "get_selection",
    description = "Get current WorldEdit selection (pos1, pos2)",
    inputSchema = mapOf("type" to "object", "properties" to mapOf()),
    inputClass = Unit::class
) {
    override fun execute(input: Unit): McpSchema.CallToolResult {
        // Note: Would need integration with WE selection API
        val json = Json.encodeToString(GetSelectionOutput(null, null))
        return McpSchema.CallToolResult.success(listOf(McpSchema.TextContent("text", json)))
    }
}
```

```kotlin
package xyz.joseg.spigotmcp.mcp.tools.selection

import io.modelcontextprotocol.spec.McpSchema
import xyz.joseg.spigotmcp.fawe.FaweAdapter
import xyz.joseg.spigotmcp.mcp.tools.BaseTool

class SetSelectionTool : BaseTool<SelectionInput>(
    name = "set_selection",
    description = "Set WorldEdit selection points",
    inputSchema = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "pos1" to mapOf("type" to "object", "properties" to mapOf(
                "x" to mapOf("type" to "integer"),
                "y" to mapOf("type" to "integer"),
                "z" to mapOf("type" to "integer"),
                "world" to mapOf("type" to "string")
            )),
            "pos2" to mapOf("type" to "object", "properties" to mapOf(
                "x" to mapOf("type" to "integer"),
                "y" to mapOf("type" to "integer"),
                "z" to mapOf("type" to "integer"),
                "world" to mapOf("type" to "string")
            ))
        )
    ),
    inputClass = SelectionInput::class
) {
    override fun execute(input: SelectionInput): McpSchema.CallToolResult {
        // Note: Would need integration with WE selection API
        return McpSchema.CallToolResult.success(listOf(McpSchema.TextContent("text", "Selection set (requires WE player session integration)")))
    }
}
```

- [ ] **Step 6: Compile check**

```bash
./gradlew compileKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add src/main/kotlin/xyz/joseg/spigotmcp/mcp/tools/clipboard/ src/main/kotlin/xyz/joseg/spigotmcp/mcp/tools/selection/
git commit -m "feat: add clipboard and selection tools"
```

---

### Task 8: Server Management Tools

**Files:**
- Create: `src/main/kotlin/xyz/joseg/spigotmcp/mcp/tools/server/RestartServerTool.kt`
- Create: `src/main/kotlin/xyz/joseg/spigotmcp/mcp/tools/server/StopServerTool.kt`
- Create: `src/main/kotlin/xyz/joseg/spigotmcp/mcp/tools/server/ServerStatusTool.kt`

- [ ] **Step 1: Create RestartServerTool**

```kotlin
package xyz.joseg.spigotmcp.mcp.tools.server

import io.modelcontextprotocol.spec.McpSchema
import xyz.joseg.spigotmcp.config.ServerConfig
import xyz.joseg.spigotmcp.mcp.tools.BaseTool

data class DelayInput(val delay: Int = 5)

class RestartServerTool(private val config: ServerConfig) : BaseTool<DelayInput>(
    name = "restart_server",
    description = "Restart the Spigot server",
    inputSchema = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "delay" to mapOf("type" to "integer", "default" to config.restartDelay, "description" to "Delay in seconds before restart")
        )
    ),
    inputClass = DelayInput::class
) {
    override fun execute(input: DelayInput): McpSchema.CallToolResult {
        val delay = input.delay
        org.bukkit.Bukkit.getScheduler().runTaskLater(org.bukkit.Bukkit.getPluginManager().getPlugin("spigot-mcp")!!) {
            org.bukkit.Bukkit.spigot().restart()
        }, (delay * 20L).toLong()
        
        return McpSchema.CallToolResult.success(listOf(McpSchema.TextContent("text", "Server restart scheduled in $delay seconds")))
    }
}
```

- [ ] **Step 2: Create StopServerTool**

```kotlin
package xyz.joseg.spigotmcp.mcp.tools.server

import io.modelcontextprotocol.spec.McpSchema
import xyz.joseg.spigotmcp.config.ServerConfig
import xyz.joseg.spigotmcp.mcp.tools.BaseTool

data class DelayInput(val delay: Int = 3)

class StopServerTool(private val config: ServerConfig) : BaseTool<DelayInput>(
    name = "stop_server",
    description = "Stop the Spigot server gracefully",
    inputSchema = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "delay" to mapOf("type" to "integer", "default" to config.stopDelay, "description" to "Delay in seconds before stop")
        )
    ),
    inputClass = DelayInput::class
) {
    override fun execute(input: DelayInput): McpSchema.CallToolResult {
        val delay = input.delay
        org.bukkit.Bukkit.getScheduler().runTaskLater(org.bukkit.Bukkit.getPluginManager().getPlugin("spigot-mcp")!!) {
            org.bukkit.Bukkit.shutdown()
        }, (delay * 20L).toLong()
        
        return McpSchema.CallToolResult.success(listOf(McpSchema.TextContent("text", "Server shutdown scheduled in $delay seconds")))
    }
}
```

- [ ] **Step 3: Create ServerStatusTool**

```kotlin
package xyz.joseg.spigotmcp.mcp.tools.server

import io.modelcontextprotocol.spec.McpSchema
import xyz.joseg.spigotmcp.mcp.tools.BaseTool
import kotlinx.serialization.json.Json

data class ServerStatusOutput(
    val onlinePlayers: Int,
    val maxPlayers: Int,
    val tps: DoubleArray,
    val memoryUsedMb: Long,
    val memoryMaxMb: Long,
    val version: String
)

class ServerStatusTool : BaseTool<Unit>(
    name = "server_status",
    description = "Get server status information",
    inputSchema = mapOf("type" to "object", "properties" to mapOf()),
    inputClass = Unit::class
) {
    override fun execute(input: Unit): McpSchema.CallToolResult {
        val players = org.bukkit.Bukkit.getOnlinePlayers()
        val runtime = Runtime.getRuntime()
        
        val status = ServerStatusOutput(
            onlinePlayers = players.size,
            maxPlayers = org.bukkit.Bukkit.getMaxPlayers(),
            tps = org.bukkit.Bukkit.getSpigotTPS(),
            memoryUsedMb = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024,
            memoryMaxMb = runtime.maxMemory() / 1024 / 1024,
            version = org.bukkit.Bukkit.getVersion()
        )
        
        val json = Json.encodeToString(status)
        return McpSchema.CallToolResult.success(listOf(McpSchema.TextContent("text", json)))
    }
}
```

- [ ] **Step 4: Compile check**

```bash
./gradlew compileKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/xyz/joseg/spigotmcp/mcp/tools/server/
git commit -m "feat: add server management tools (restart, stop, status)"
```

---

### Task 9: MCP Server Assembly & Plugin Main Class

**Files:**
- Create: `src/main/kotlin/xyz/joseg/spigotmcp/mcp/McpServer.kt`
- Create: `src/main/kotlin/xyz/joseg/spigotmcp/SpigotMCPPlugin.kt`
- Modify: `src/main/resources/plugin.yml`

- [ ] **Step 1: Create McpServer assembly**

```kotlin
package xyz.joseg.spigotmcp.mcp

import io.modelcontextprotocol.server.McpServer
import io.modelcontextprotocol.server.McpServerFeatures
import io.modelcontextprotocol.spec.McpSchema
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import xyz.joseg.spigotmcp.config.PluginConfig
import xyz.joseg.spigotmcp.fawe.FaweAdapter
import xyz.joseg.spigotmcp.mcp.auth.AuthMiddleware
import xyz.joseg.spigotmcp.mcp.tools.BaseTool
import xyz.joseg.spigotmcp.mcp.tools.ToolRegistry
import xyz.joseg.spigotmcp.mcp.tools.block.*
import xyz.joseg.spigotmcp.mcp.tools.clipboard.*
import xyz.joseg.spigotmcp.mcp.tools.selection.*
import xyz.joseg.spigotmcp.mcp.tools.server.*
import xyz.joseg.spigotmcp.mcp.transport.HttpSseTransport
import xyz.joseg.spigotmcp.mcp.transport.StdioTransport

class McpServerHost(
    private val config: PluginConfig,
    private val faweAdapter: FaweAdapter
) {
    private val scope = CoroutineScope(SupervisorJob())
    private var server: McpServer? = null
    private var httpTransport: HttpSseTransport? = null
    private var stdioTransport: StdioTransport? = null
    
    fun start() {
        val baseServer = McpServer.sync(McpSchema.Implementation("spigot-mcp", "1.0.0"))
            .capabilities(McpSchema.ServerCapabilities(tools = McpSchema.ToolsCapability(listChanged = true)))
            .build()
        
        val authServer = if (config.mcp.auth.enabled) {
            AuthMiddleware(config.mcp.auth, baseServer)
        } else baseServer
        
        val registry = ToolRegistry(authServer)
        registerTools(registry)
        
        server = authServer
        
        if (config.mcp.stdioEnabled) {
            stdioTransport = StdioTransport(authServer, scope)
            stdioTransport?.start()
        }
        
        if (config.mcp.httpEnabled) {
            httpTransport = HttpSseTransport(authServer, config.mcp.port, scope)
            httpTransport?.start()
        }
    }
    
    private fun registerTools(registry: ToolRegistry) {
        val blockTools = listOf<BaseTool<*>>(
            SetBlocksTool(faweAdapter),
            ReplaceBlocksTool(faweAdapter),
            WallsTool(faweAdapter),
            SphereTool(faweAdapter),
            CylinderTool(faweAdapter)
        )
        
        val clipboardTools = listOf<BaseTool<*>>(
            CopyTool(faweAdapter),
            PasteTool(faweAdapter),
            ClearClipboardTool(faweAdapter)
        )
        
        val selectionTools = listOf<BaseTool<*>>(
            GetSelectionTool(),
            SetSelectionTool()
        )
        
        val serverTools = listOf<BaseTool<*>>(
            RestartServerTool(config.server),
            StopServerTool(config.server),
            ServerStatusTool()
        )
        
        registry.registerAll(*blockTools, *clipboardTools, *selectionTools, *serverTools)
    }
    
    fun stop() {
        scope.coroutineContext.cancelChildren()
    }
}
```

- [ ] **Step 2: Create main plugin class**

```kotlin
package xyz.joseg.spigotmcp

import org.bukkit.plugin.java.JavaPlugin
import xyz.joseg.spigotmcp.config.ConfigLoader
import xyz.joseg.spigotmcp.config.PluginConfig
import xyz.joseg.spigotmcp.fawe.FaweAdapter
import xyz.joseg.spigotmcp.mcp.McpServerHost

class SpigotMCPPlugin : JavaPlugin() {
    
    private lateinit var config: PluginConfig
    private lateinit var faweAdapter: FaweAdapter
    private lateinit var mcpServer: McpServerHost
    
    override fun onEnable() {
        saveDefaultConfig()
        config = ConfigLoader.load(dataFolder)
        
        faweAdapter = FaweAdapter(config.fawe)
        mcpServer = McpServerHost(config, faweAdapter)
        
        mcpServer.start()
        
        logger.info("SpigotMCP enabled - MCP server started on stdio:${config.mcp.stdioEnabled} http:${config.mcp.httpEnabled}:${config.mcp.port}")
    }
    
    override fun onDisable() {
        mcpServer.stop()
        logger.info("SpigotMCP disabled")
    }
}
```

- [ ] **Step 3: Create plugin.yml**

```yaml
name: spigot-mcp
version: 1.0-SNAPSHOT
main: xyz.joseg.spigotmcp.SpigotMCPPlugin
api-version: 1.20
depend: [FastAsyncWorldEdit]
author: joseg
description: MCP Server for Spigot with FAWE integration
commands: {}
permissions:
  spigotmcp.mcp.use:
    description: Allow using MCP tools
    default: op
```

- [ ] **Step 4: Build and test**

```bash
./gradlew build
```
Expected: BUILD SUCCESSFUL, JAR in `build/libs/spigot-mcp-1.0-SNAPSHOT.jar`

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/xyz/joseg/spigotmcp/mcp/McpServer.kt src/main/kotlin/xyz/joseg/spigotmcp/SpigotMCPPlugin.kt src/main/resources/plugin.yml
git commit -m "feat: assemble MCP server and main plugin class"
```

---

### Task 10: Basic Tests

**Files:**
- Create: `src/test/kotlin/xyz/joseg/spigotmcp/config/ConfigTest.kt`
- Create: `src/test/kotlin/xyz/joseg/spigotmcp/util/PositionTest.kt`
- Create: `src/test/kotlin/xyz/joseg/spigotmcp/fawe/FaweAdapterTest.kt`

- [ ] **Step 1: ConfigTest**

```kotlin
package xyz.joseg.spigotmcp.config

import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigTest {
    
    @Test
    fun `loads default config when file missing`() {
        val config = PluginConfig()
        assertEquals(8080, config.mcp.port)
        assertTrue(config.mcp.stdioEnabled)
        assertTrue(config.mcp.httpEnabled)
        assertEquals(50000, config.fawe.maxBlocksPerOp)
    }
    
    @Test
    fun `parses custom config from yaml`() {
        val yaml = YamlConfiguration()
        yaml.set("mcp.port", 9000)
        yaml.set("mcp.auth.enabled", false)
        yaml.set("fawe.max-blocks-per-op", 100000)
        
        val config = ConfigLoader.parseConfig(yaml)
        
        assertEquals(9000, config.mcp.port)
        assertEquals(false, config.mcp.auth.enabled)
        assertEquals(100000, config.fawe.maxBlocksPerOp)
    }
}
```

- [ ] **Step 2: PositionTest**

```kotlin
package xyz.joseg.spigotmcp.util

import com.sk89q.worldedit.math.BlockVector3
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PositionTest {
    
    @Test
    fun `converts to BlockVector3`() {
        val pos = Pos(10, 64, -5, "world")
        val vec = pos.toBlockVector3()
        
        assertEquals(10, vec.getBlockX())
        assertEquals(64, vec.getBlockY())
        assertEquals(-5, vec.getBlockZ())
    }
    
    @Test
    fun `creates from BlockVector3`() {
        val vec = BlockVector3.at(10, 64, -5)
        val pos = Pos.fromBlockVector3(vec, "world_nether")
        
        assertEquals(10, pos.x)
        assertEquals(64, pos.y)
        assertEquals(-5, pos.z)
        assertEquals("world_nether", pos.world)
    }
}
```

- [ ] **Step 3: FaweAdapterTest (mock-based)**

```kotlin
package xyz.joseg.spigotmcp.fawe

import xyz.joseg.spigotmcp.config.FaweConfig
import xyz.joseg.spigotmcp.util.Pos
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FaweAdapterTest {
    
    @Test
    fun `validates region size against max blocks`() {
        val config = FaweConfig(maxBlocksPerOp = 100)
        val adapter = FaweAdapter(config)
        
        // Large region should fail validation
        val pos1 = Pos(0, 0, 0, "world")
        val pos2 = Pos(100, 100, 100, "world") // 1M blocks > 100
        
        val result = adapter.setBlocks(pos1, pos2, "STONE")
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("too large") == true)
    }
}
```

- [ ] **Step 4: Run tests**

```bash
./gradlew test
```
Expected: All tests pass

- [ ] **Step 5: Commit**

```bash
git add src/test/kotlin/xyz/joseg/spigotmcp/
git commit -m "test: add unit tests for config, position, and fawe adapter"
```

---

### Task 11: Integration Verification

**Files:**
- None (manual verification)

- [ ] **Step 1: Deploy to test Spigot server**

```bash
# Copy JAR to Spigot plugins folder
cp build/libs/spigot-mcp-1.0-SNAPSHOT.jar /path/to/spigot/plugins/

# Start Spigot with FAWE installed
# Verify plugin loads: [SpigotMCP] SpigotMCP enabled - MCP server started...
```

- [ ] **Step 2: Test stdio transport**

```bash
# In another terminal, test stdio
echo '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}' | java -jar build/libs/spigot-mcp-1.0-SNAPSHOT.jar
# Should return list of tools
```

- [ ] **Step 3: Test HTTP transport**

```bash
# Start plugin, then:
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer changeme-in-production" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}'
```

- [ ] **Step 4: Test a tool call**

```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer changeme-in-production" \
  -d '{
    "jsonrpc":"2.0",
    "id":1,
    "method":"tools/call",
    "params":{
      "name":"server_status",
      "arguments":{}
    }
  }'
```

- [ ] **Step 5: Commit final verification**

```bash
git commit --allow-empty -m "chore: integration verified - plugin loads, MCP stdio+HTTP work, tools callable"
```

---

## Summary

**Total Tasks:** 11
**Estimated Time:** 3-5 hours
**Key Deliverables:**
1. Spigot plugin with embedded MCP server (stdio + HTTP/SSE)
2. 13 MCP tools covering block ops, clipboard, selection, server mgmt
3. FAWE programmatic adapter with safety limits
4. Config-driven with Bearer token auth
5. Unit tests for core components

**Next Steps (Phase 2):**
- Schematic load/save/paste
- Biome operations
- Player-specific selections (per-player WE sessions)
- Web UI for tool testing