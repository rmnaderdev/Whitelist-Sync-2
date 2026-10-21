# Contributing / local development

## Fast setup (IntelliJ IDEA)

Only the modules you select are loaded, so import doesn't decompile every Minecraft version.
By default **only the latest** Fabric and NeoForge modules are included.

1. Open the repo root as a Gradle project (JDK 21 for Gradle is fine; toolchains for Java 21/25 are auto-provisioned).
2. To work on another version, create `local.properties` (gitignored) in the repo root:
   ```properties
   mc=1.21.8
   ```
3. In IntelliJ, click **Reload All Gradle Projects** (Gradle tool window). Only the selected modules appear.

`mc` values (command line `-Pmc=...` overrides `local.properties`):

| Value | Modules loaded |
|-------|----------------|
| `latest` (default) | newest version of each loader |
| `1.21.8` or `1.21.8,26.2` | those versions, for every loader that has them |
| `fabric-1.21.8,neoforge-26.2` | exact modules |
| `all` | everything in `versions.properties` (slow; only needed to build everything) |

Tip: IntelliJ's Gradle run configurations accept `-Pmc=...` in "Run" arguments; PowerShell users must quote args containing commas/dots, e.g. `"-Pmc=1.21.8,26.2"`.

## Common commands

```sh
./gradlew :fabric-1.21.8:build -Pmc=fabric-1.21.8
./gradlew :neoforge-26.2:runServer          # with mc=latest
./gradlew :WhitelistSyncCore:test
./gradlew copyConfigToRun
```

A project must be selected via `mc` before you can run tasks on it (`:fabric-1.21.8:build` with the default `latest` fails with "project not found").

## Supported versions list

`versions.properties` is the single list of supported versions per loader (oldest -> newest; the last is "latest").
CI and release workflows build/publish one module per entry using `-Pmc=<loader>-<version>`.

## Adding a new Minecraft version

```sh
./gradlew addMinecraftVersion -PnewLoader=fabric -PnewMc=1.21.12 -PfromMc=1.21.11
```

This copies the closest module, updates `minecraft_version*` and registers it in `versions.properties`.
You then still need to update loader/mappings/API versions in `gradle.properties`, fix per-version sources
for API changes, and update the README. In Copilot CLI/Chat, the `add-minecraft-version` skill
(`.github/skills/add-minecraft-version/SKILL.md`) automates this using the upstream Fabric/NeoForge templates.
