# Copilot instructions

## Build and test

Run Gradle through the repository wrapper:

```sh
./gradlew build -Pmc=all
./gradlew :fabric-1.21.8:build -Pmc=fabric-1.21.8
./gradlew :neoforge-1.21.8:build -Pmc=neoforge-1.21.8
```

`settings.gradle` only includes modules selected by `-Pmc=` (or `mc=` in gitignored `local.properties`; default `latest`), from the version list in `versions.properties`. A module must be selected to build it. See `CONTRIBUTING.md`. To add a version, use the `add-minecraft-version` skill. Core unit tests use JUnit 5:

```sh
./gradlew :WhitelistSyncCore:test
./gradlew :WhitelistSyncCore:test --tests "net.rmnad.core.config.WhitelistSyncConfigTest"
./gradlew :WhitelistSyncCore:test --tests "net.rmnad.core.config.WhitelistSyncConfigTest.defaultsMatchExpectedValues"
```

## Architecture

- `WhitelistSyncCore` contains platform-independent config, database/web services, sync threads, JSON readers, and DTOs. Loader modules include its shadow JAR; the core targets Java 17 and relocates bundled dependencies to avoid conflicts.
- Loader-specific modules implement `IServerControl` and connect core callbacks to Minecraft server APIs and events. SQLite/MySQL use periodic polling; Web mode uses a SignalR socket for real-time updates.
- This is a multi-project Gradle build with separate `fabric-<version>` and `neoforge-<version>` projects. Current supported loaders are Fabric and NeoForge; do not treat older Forge entries in `README.md` as active modules.

## Repository conventions

- Before changing a loader source, determine whether it is shared or version-specific. Shared Fabric sources/resources live in `fabric-common/`; shared NeoForge sources/resources live in `neoforge-common/`. API-dependent code and loader metadata live in each versioned module, so fixes may need to be applied to multiple versions.
- Keep loader build configuration centralized in `gradle/fabric.gradle` and `gradle/neoforge.gradle`. A versioned module's `build.gradle` declares its loader plugins and applies the shared script; its `gradle.properties` supplies version-specific Minecraft/loader settings. Fabric's `fabric_gen` selects Yarn/Java 21/remapped output or Mojang mappings/Java 25 output. NeoForge uses `java_version` and optional Parchment mapping properties.
- `version.txt` is the single source of the mod version and is injected by `settings.gradle`; do not reintroduce `mod_version` in `gradle.properties`.
- When changing shaded dependencies, preserve the relocation rules in `WhitelistSyncCore/build.gradle`. In particular, SQLite JDBC native-loader classes must not be relocated.
- Core tests are under `WhitelistSyncCore/src/test`; run a focused test with Gradle's `--tests` filter as shown above.
