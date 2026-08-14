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
    testImplementation(kotlin("test"))

    // MCP Server
    implementation("io.modelcontextprotocol:kotlin-sdk:0.9.0")
    implementation("io.netty:netty-transport-native-epoll:4.1.100.Final")
    implementation("io.netty:netty-codec-http:4.1.100.Final")
    implementation("io.netty:netty-handler:4.1.100.Final")
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}
