# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Whitelist Sync 2 is a Minecraft mod that synchronizes whitelists, ops, and bans across multiple servers via MySQL, SQLite, or a proprietary cloud web service (Whitelist Sync Web).

**Supported loaders:** Fabric, NeoForge (Forge support was removed — it could not run on Gradle 9)
**MC versions:** 1.20.1–1.21.x and 26.x, one subproject per version
**Current version:** 2.8.4

## Build Commands

```bash
./gradlew build                       # Build everything
./gradlew :fabric-1.21.8:build        # Build one subproject
./gradlew clean                       # Delete /target
./gradlew copyConfigToRun             # Copy sample config into each run dir
./gradlew publishUnified              # Publish to Modrinth + CurseForge (needs MODRINTH_TOKEN, CF_TOKEN)
```

No test suite exists — verification is manual via in-game testing.

**Toolchains:** the build declares a Java toolchain per generation (1.21.x → Java 21, 26.x → Java 25) and auto-provisions the JDK via the foojay resolver, so you do **not** need to launch Gradle on a matching JDK. Produced jars land in `/target/WhitelistSync-<version>-<loader>-<mcver>.jar`.

## Architecture

### Core / Platform Split

`WhitelistSyncCore` is a platform-agnostic shadow JAR (Java 17) embedded into each mod JAR. Its dependencies are relocated under `net.rmnad.core.shade.*` to avoid conflicts (see `WhitelistSyncCore/build.gradle`). Platform modules implement the `IServerControl` callback interface and wire up Minecraft events.

```
WhitelistSyncCore/src/main/java/net/rmnad/core/
  services/      — DB and sync logic (MySqlService, SqLiteService, WebService)
                   + threads (WhitelistPollingThread, WhitelistSocketThread)
  config/        — TOML config parsing
  models/        — DTOs: WhitelistedPlayer, OppedPlayer, BannedPlayer, BannedIp
  json/          — Vanilla JSON file readers for the server's .json save files
  callbacks/     — IServerControl interface (platform abstraction boundary)
  WhitelistSyncCore.java  — Orchestrator; initializes service and starts threads
```

### Sync Mechanisms

Two strategies, selected by config:
1. **WebService** → `WhitelistSocketThread` — real-time via SignalR WebSocket (cloud service)
2. **MySqlService / SqLiteService** → `WhitelistPollingThread` — periodic DB polling

### Multi-version layout (IMPORTANT)

Each `fabric-<mcver>` / `neoforge-<mcver>` is a separate Gradle subproject. Because Mojang changes APIs almost every point release, the setup is deliberately split into **shared** vs **per-version** parts. Know which bucket a file is in before editing:

**Shared build logic** — `gradle/fabric.gradle`, `gradle/neoforge.gradle`.
Each subproject `build.gradle` is only a `plugins {}` block + `apply from: "$rootDir/gradle/<loader>.gradle"`. The shared script reads per-version values and a generation flag from that subproject's `gradle.properties`:
- Fabric: `fabric_gen=yarn` (Java 21, `net.fabricmc.fabric-loom-remap`, yarn mappings, publishes `remapJar`) or `fabric_gen=mojmap` (Java 25, `net.fabricmc.fabric-loom`, no mappings, publishes `shadowJar`).
- NeoForge: `java_version=21|25`; `parchment{}` is applied only when `parchment_mappings_version` is set.

**Shared sources** (byte-identical across all versions) — edit these ONCE:
- `fabric-common/` — `WhitelistSync2`, `FabricLogger`, `logo.webp`
- `neoforge-common/` — `WhitelistSync2`, `ForgeLogger`, `neoforge.mods.toml`, `pack.mcmeta`

**Per-version sources** (genuinely differ per MC version — kept in each subproject's own `src/`) — a fix here may need repeating across the affected versions:
- Fabric: `ServerControl`, `WhitelistSyncCommands`, `mixin/MinecraftCommandMixins`, `fabric.mod.json`, `whitelistsync2.mixins.json`
- NeoForge: `ServerControl`, `WhitelistSyncCommands`, `CommandsListener`

### Adding a new MC version

Copy the closest existing subproject dir, update its `gradle.properties` (mappings/loader/version values + `fabric_gen` or `java_version`), add an `include` line in `settings.gradle`, and reconcile only the per-version source files against the new MC API. The shared scripts and shared sources need no changes.

## CI

- **PR builds:** `.github/workflows/build.yml` runs `./gradlew build` on `self-hosted`
- **Releases:** `.github/workflows/publish.yml` triggers on `v*` tags, runs `./gradlew publishUnified`
