plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.serialization") version "2.3.0"
    id("com.gradleup.shadow") version "9.6.1"
}

group = "xyz.joseg"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/groups/public/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
}

// ---------------------------------------------------------------------------
// Supported platforms
//
// `main`   - version agnostic. Compiled against the OLDEST supported Bukkit API
//            so it is impossible to accidentally depend on a 1.13+ only method.
//            It never references WorldEdit.
// `modern` - WorldEdit 7 / FastAsyncWorldEdit 2.x backend (MC 1.13+).
// `legacy` - WorldEdit 6 / FastAsyncWorldEdit-Reborn backend (MC 1.8 - 1.12).
//
// Both backends implement the same interface and are merged into a single JAR.
// Only one of them is ever class-loaded, decided at runtime by WorldEditBackends.
// ---------------------------------------------------------------------------
val spigotModern = "1.21.4-R0.1-SNAPSHOT"
val spigotLegacy = "1.8.8-R0.1-SNAPSHOT"
val faweModern = "2.15.3"
val worldEditLegacy = "6.1"

val backendSourceSets = listOf("modern", "legacy")

sourceSets {
    backendSourceSets.forEach { backend ->
        create(backend) {
            compileClasspath += getByName("main").output
            runtimeClasspath += getByName("main").output
        }
    }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:$spigotLegacy")
    testImplementation(kotlin("test"))

    // MCP Server - Java SDK 0.18.x (server-servlet only up to 0.18.3)
    implementation("io.modelcontextprotocol.sdk:mcp-core:0.18.3")
    implementation("io.modelcontextprotocol.sdk:server-servlet:0.18.3")
    implementation("io.modelcontextprotocol.sdk:mcp-json-jackson2:0.18.3")

    // Jetty 11 for servlet-based MCP HTTP transport (matches Tomcat 11 in server-servlet)
    implementation("org.eclipse.jetty:jetty-server:11.0.23")
    implementation("org.eclipse.jetty:jetty-servlet:11.0.23")
    implementation("org.eclipse.jetty:jetty-http:11.0.23")
    implementation("org.eclipse.jetty:jetty-io:11.0.23")
    implementation("org.eclipse.jetty:jetty-util:11.0.23")
    implementation("org.eclipse.jetty:jetty-webapp:11.0.23")

    // kotlinx dependencies for serialization and coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    // --- modern backend: WorldEdit 7 / FAWE 2.x on MC 1.13+ -----------------
    "modernCompileOnly"(kotlin("stdlib"))
    "modernCompileOnly"("org.spigotmc:spigot-api:$spigotModern")
    "modernCompileOnly"("com.fastasyncworldedit:FastAsyncWorldEdit-Core:$faweModern")
    "modernCompileOnly"("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit:$faweModern")

    // --- legacy backend: WorldEdit 6 / FAWE-Reborn on MC 1.8 - 1.12 ---------
    // FAWE-Reborn ships no `WorldEdit` class of its own: it overrides a subset
    // of WorldEdit 6 classes and injects itself at runtime, so the backend is
    // compiled against upstream WorldEdit 6 and accelerated by FAWE in place.
    "legacyCompileOnly"(kotlin("stdlib"))
    "legacyCompileOnly"("org.spigotmc:spigot-api:$spigotLegacy")
    // worldedit-bukkit 6.1 declares org.bukkit:bukkit:1.7.9-R0.2, which no longer exists on any
    // reachable repository. The Bukkit API we compile against is spigot-api above, so the whole
    // transitive graph of the legacy WorldEdit artifacts is dropped.
    "legacyCompileOnly"("com.sk89q.worldedit:worldedit-core:$worldEditLegacy") { isTransitive = false }
    "legacyCompileOnly"("com.sk89q.worldedit:worldedit-bukkit:$worldEditLegacy") { isTransitive = false }
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

// ShadowJar configuration to create fat JAR with all dependencies
tasks.shadowJar {
    archiveClassifier.set("")

    // Both backends ship in the same JAR; the unused one is simply never loaded.
    backendSourceSets.forEach { from(sourceSets[it].output) }

    // Include plugin.yml and config.yml in the JAR
    from("src/main/resources") {
        include("plugin.yml")
        include("config.yml")
    }

    manifest {
        attributes(
            "Main-Class" to "xyz.joseg.spigotmcp.SpigotMCPPlugin",
            "Plugin-Name" to "spigot-mcp",
            "Plugin-Version" to "1.0-SNAPSHOT",
            "Plugin-Main" to "xyz.joseg.spigotmcp.SpigotMCPPlugin",
            "Plugin-Depend" to "FastAsyncWorldEdit"
        )
    }

    // Merge service files
    mergeServiceFiles()

    // Relocate conflicting packages
    relocate("io.modelcontextprotocol", "xyz.joseg.spigotmcp.shaded.modelcontextprotocol")
    relocate("io.ktor", "xyz.joseg.spigotmcp.shaded.ktor")
    relocate("com.fasterxml.jackson", "xyz.joseg.spigotmcp.shaded.jackson")
    relocate("reactor", "xyz.joseg.spigotmcp.shaded.reactor")
    relocate("org.jetbrains.kotlinx", "xyz.joseg.spigotmcp.shaded.kotlinx")
}

// Make shadowJar the default jar task
tasks.jar {
    enabled = false
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
