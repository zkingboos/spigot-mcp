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

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.21.4-R0.1-SNAPSHOT")
    // FAWE 2.15.3 for MC 1.21.4 (from PaperMC repo)
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core:2.15.3")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit:2.15.3")
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