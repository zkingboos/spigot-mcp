import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    kotlin("jvm") version "2.3.0"
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

    // JSON only (Java 8 compatible). MCP protocol + HTTP are hand-rolled in
    // mcp/protocol/MiniMcp.kt over java.io + com.sun.net.httpserver so the
    // shaded jar stays loadable on JVM 8 servers (vanilla MC 1.8.8).
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")

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

    // Universal floor: emit Java 8 bytecode for every source set so the single
    // shaded jar loads on any server JVM >= 8 (MC 1.8 vanilla through 1.21.x).
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

// -Xjdk-release also restricts the visible JDK API surface to Java 8, catching
// accidental use of newer JDK APIs at compile time. Apply it ONLY where the code
// must RUN on Java 8 (main + legacy). The modern source set references FAWE
// classes whose hierarchies include java.lang.Record (JDK 16+), which is fine:
// that backend only ever links on modern servers with JVM 21.
listOf("compileKotlin", "compileLegacyKotlin").forEach { name ->
    tasks.named<KotlinCompilationTask<*>>(name) {
        compilerOptions.freeCompilerArgs.add("-Xjdk-release=8")
    }
}

// Java side: pin emitted bytecode to 8 via --release.
tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
}

// Kotlin's jvmTarget=8 sets the resolution attribute on Kotlin compile classpaths.
// The modern backend compiles against FAWE 2.x (module metadata declares JVM 21),
// so that classpath must resolve against 21 even though we still emit Java 8
// bytecode (compile-against-new, emit-old: those classes only link where FAWE is).
configurations.named("modernCompileClasspath") {
    attributes {
        attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 21)
    }
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
            "Multi-Release" to "true",
            "Main-Class" to "xyz.joseg.spigotmcp.SpigotMCPPlugin",
            "Plugin-Name" to "spigot-mcp",
            "Plugin-Version" to "1.0-SNAPSHOT",
            "Plugin-Main" to "xyz.joseg.spigotmcp.SpigotMCPPlugin",
            "Plugin-Depend" to "FastAsyncWorldEdit"
        )
    }

    // Merge service files
    mergeServiceFiles()

    // Relocate the only shaded third-party package
    relocate("com.fasterxml.jackson", "xyz.joseg.spigotmcp.shaded.jackson")
}

// Make shadowJar the default jar task
tasks.jar {
    enabled = false
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
