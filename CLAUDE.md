# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Whitelist Sync 2 is a Minecraft mod that synchronizes whitelists, ops, and bans across multiple servers via MySQL, SQLite, or a proprietary cloud web service (Whitelist Sync Web).

**Supported loaders:** Fabric, NeoForge (Forge deprecated as of v2.8.2)  
**MC versions:** 1.16.5–1.21.11 across multiple subprojects  
**Current version:** 2.8.4

## Build Commands

```bash
./gradlew build              # Full build all subprojects
./gradlew clean              # Delete /target
./gradlew copyConfigToRun    # Copy sample config to run directories
./gradlew publishUnified     # Publish to Modrinth + CurseForge (requires MODRINTH_TOKEN, CF_TOKEN)
```

No test suite exists — verification is manual via in-game testing.

## Architecture

### Core / Platform Split

`WhitelistSyncCore` is a platform-agnostic shadow JAR embedded into each mod JAR via Gradle shadow plugin. Platform modules (`fabric-*`, `neoforge-*`) implement the `IServerControl` callback interface and wire up Minecraft events.

```
WhitelistSyncCore/src/main/java/net/rmnad/core/
  services/      — DB and sync logic (MySqlService, SqLiteService, WebService)
  services/      — Thread classes (WhitelistPollingThread, WhitelistSocketThread)
  config/        — TOML config parsing
  models/        — DTOs: WhitelistedPlayer, OppedPlayer, BannedPlayer, BannedIp
  json/          — Vanilla JSON file readers for server's .json save files
  callbacks/     — IServerControl interface (platform abstraction boundary)
  WhitelistSyncCore.java  — Orchestrator; initializes service and starts threads
```

Each platform module contains:
- `WhitelistSync2.java` — entry point, event registration
- `WhitelistSyncCommands.java` — command registration
- `ServerControl.java` — IServerControl implementation
- `FabricLogger.java` / equivalent — logger adapter

### Sync Mechanisms

Two sync strategies, selected by config:

1. **WebService** → `WhitelistSocketThread` — real-time via SignalR WebSocket (cloud service)
2. **MySqlService / SqLiteService** → `WhitelistPollingThread` — periodic DB polling

### Multi-subproject Structure

Each `fabric-<mcver>` and `neoforge-<mcver>` directory is a separate Gradle subproject targeting that specific Minecraft version. Changes to sync logic belong in `WhitelistSyncCore`; MC-version-specific changes belong in the relevant subproject.

### Output JARs

`/target/WhitelistSync-<version>-<loader>-<mcver>.jar` — shaded fat JARs ready for deployment.

## CI

- **PR builds:** `.github/workflows/build.yml` runs `./gradlew build` on `whitelist-sync-2-arc-runner`
- **Releases:** `.github/workflows/publish.yml` triggers on `v*` tags, runs `./gradlew publishUnified`
