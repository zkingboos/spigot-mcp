# Spigot MCP - Agent Instructions

## Project Overview
Kotlin/Gradle Spigot Minecraft plugin (1.20.2). No source code yet.

## Commands
```bash
./gradlew build          # Compile, test, package
./gradlew test           # Run tests (JUnit Platform)
./gradlew jar            # Build plugin JAR only
./gradlew check          # Run all verification (compile + test)
```

## Key Configuration
- **Gradle**: 9.2.0 (wrapper)
- **Kotlin**: 2.3.0, JVM 25 toolchain
- **Code style**: `kotlin.code.style=official` (ktlint)
- **Test framework**: JUnit Platform via `kotlin-test`

## Dependencies (compileOnly unless noted)
- `org.spigotmc:spigot-api:1.20.2-R0.1-SNAPSHOT`
- `com.fastasyncworldedit:FastAsyncWorldEdit-Core` (via IntellectualSites BOM 1.56)
- `org.jetbrains.kotlin:kotlin-test` (testImplementation)

## Repository Structure
```
spigot-mcp/
├── build.gradle.kts       # Build config
├── settings.gradle.kts    # Root project + foojay resolver
├── gradle.properties      # kotlin.code.style=official
├── gradlew / gradlew.bat  # Wrapper scripts
└── src/                   # Not created yet (main/kotlin, test/kotlin)
```

## Conventions
- Source: `src/main/kotlin`, Tests: `src/test/kotlin`
- Package: `xyz.joseg` (from `group`)
- No existing lint/typecheck tasks beyond standard Gradle + ktlint via code style

## Gotchas
- Spigot API from `hub.spigotmc.org` + Paper/EngineHub repos
- FAWE dependency via BOM (version managed externally)
- JAR output: `build/libs/spigot-mcp-1.0-SNAPSHOT.jar`