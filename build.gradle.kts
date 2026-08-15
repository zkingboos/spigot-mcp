plugins {
    kotlin("jvm") version "2.3.0"
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
    compileOnly("org.spigotmc:spigot-api:1.20.2-R0.1-SNAPSHOT")
    implementation(platform("com.intellectualsites.bom:bom-newest:1.56"))
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit")
    testImplementation(kotlin("test"))

    // MCP Server - Java SDK
    implementation("io.modelcontextprotocol.sdk:mcp-core:0.17.2")
    implementation("io.modelcontextprotocol.sdk:server-servlet:0.18.3")
    implementation("io.modelcontextprotocol.sdk:mcp-json-jackson2:0.17.2")
    
    // Ktor for HTTP transport
    implementation("io.ktor:ktor-server-core:3.2.3")
    implementation("io.ktor:ktor-server-netty:3.2.3")
    implementation("io.ktor:ktor-server-sse:3.2.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.2.3")
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}
