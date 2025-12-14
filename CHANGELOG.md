# Changelog

## [2.8.3] - 2025-12-14
- Added support for new Minecraft versions for NeoForge and Fabric:
  - fabric-1.21.11
  - neoforge-1.21.11

- Added SQLite and MySQL database drivers back into the mod jar. **External connector mod jars are no longer required.**
- Refactored package names to be consistent across all versions and loaders.

## [2.8.2] - 2025-10-17
Added support for new Minecraft versions for NeoForge and Fabric:
- fabric-1.21.6
- fabric-1.21.7
- fabric-1.21.8
- fabric-1.21.10
- neoforge-1.21.8
- neoforge-1.21.9
- neoforge-1.21.10

Bug fixes:
- Fixes [#33](https://github.com/rmnaderdev/Whitelist-Sync-2/issues/33) [MySQL] Unable to create database when database name contains '-' character

*Forge is no longer going to be actively supported due to ForgeGradle not being compatible with Gradle v9.*