---
name: add-minecraft-version
description: Add support for a new Minecraft version to Whitelist Sync 2 for Fabric and/or NeoForge. Use when asked to add, port, or bump to a new Minecraft version or mod SDK version.
---

# Add a new Minecraft version

Follow these steps. Check upstream sources online for current values; don't guess versions.

## 1. Gather upstream info (online)
- **Fabric**: https://fabricmc.net/develop (Minecraft, Yarn if applicable, loader, Fabric API versions) and the template https://github.com/FabricMC/fabric-example-mod (branch matching the MC version, e.g. `1.21`; compare `gradle.properties`, `build.gradle`, `fabric.mod.json`). Fabric API versions: https://modrinth.com/mod/fabric-api/versions
  Newer unobfuscated releases (26.x+) use Mojang mappings (`fabric_gen=mojmap`, Java 25, plugin `net.fabricmc.fabric-loom`); older ones use Yarn (`fabric_gen=yarn`, Java 21, `net.fabricmc.fabric-loom-remap`). Check which applies; the Yarn mapping may not exist for new versions.
- **NeoForge**: https://projects.neoforged.net/neoforged/neoforge and https://maven.neoforged.net/releases/net/neoforged/neoforge/ (pick the matching `neo_version`), template https://github.com/NeoForgeMDKs/MDK-<mcversion>-ModDevGradle (or https://github.com/neoforged/MDK). Parchment mappings: https://parchmentmc.org/docs/getting-started (omit `parchment_*` properties if unavailable).
- Migration notes for API breaks (e.g. https://docs.neoforged.net/primer/docs/ and Fabric blog posts for the target version).
Use web_fetch/web_search for these.

## 2. Scaffold
Pick the closest existing module (same era / same `fabric_gen` / `java_version`) as `-PfromMc`. Quote args in PowerShell:
```
./gradlew addMinecraftVersion "-PnewLoader=fabric" "-PnewMc=<new>" "-PfromMc=<closest>"
```
This copies the module, rewrites `minecraft_version*`, and appends to `versions.properties` (last entry = latest, keep ordering oldest -> newest).

## 3. Update `<loader>-<new>/gradle.properties`
- Fabric: `yarn_mappings` (yarn gen only), `loader_version`, `fabric_version`, `fabric_gen`. Update `fabric.mod.json` (`java`, `minecraft` constraints) if needed.
- NeoForge: `neo_version`, `neo_version_range`, `minecraft_version_range`, `loader_version_range`, `java_version` (21 or 25), `parchment_*` (remove if unavailable).
- If the Java generation changes, check `gradle/fabric.gradle` / `gradle/neoforge.gradle` and the plugin versions in `settings.gradle` still apply. Keep each module's `build.gradle` as plugins + `apply from`.

## 4. Fix per-version sources
Shared code lives in `fabric-common/` and `neoforge-common/` (edit once, never copy). Only these differ per version:
- Fabric: `ServerControl`, `WhitelistSyncCommands`, `mixin/MinecraftCommandMixins`, `whitelistsync2.mixins.json`, `fabric.mod.json`
- NeoForge: `ServerControl`, `WhitelistSyncCommands`, `CommandsListener`
Build, then fix compile errors by comparing with the nearest older module (`git diff --no-index <older>/src <new>/src`) and upstream API changes. If a shared-source change is required, ensure other versions still compile.

## 5. Verify
```
./gradlew ":<loader>-<new>:build" "-Pmc=<loader>-<new>"
```
Fix until it passes. The jar appears in `target/WhitelistSync-<version>-<loader>-<mc>.jar`.

## 6. Finish
- Add the version to the README "Supported ... Versions" list (newest first).
- Don't edit `version.txt` (release-please manages it) or workflows (they read `versions.properties`).
- Summarize the versions chosen (with sources) and any unresolved API issues.
