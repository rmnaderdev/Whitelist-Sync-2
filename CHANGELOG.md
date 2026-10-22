# Changelog

## [2.10.0](https://github.com/rmnaderdev/Whitelist-Sync-2/compare/v2.9.0...v2.10.0) (2026-10-22)


### Features

* add ban sync for mysql and sqlite ([a045ada](https://github.com/rmnaderdev/Whitelist-Sync-2/commit/a045ada35090bc8bfda495bd540f35dd4580d045))
* Add Fabric 26.3 module ([8a3784a](https://github.com/rmnaderdev/Whitelist-Sync-2/commit/8a3784a1a6cc54b43f8002c234e26f9a888fd456))
* add Gradle task to scaffold new MC versions ([7457db8](https://github.com/rmnaderdev/Whitelist-Sync-2/commit/7457db875e819be341d3a402089e7bb1327b557c))
* Add NeoForge 26.3 module ([c80e22d](https://github.com/rmnaderdev/Whitelist-Sync-2/commit/c80e22d63d3100ec59ead1c8af2e0e9661234b48))
* added postgres db support ([5147ea1](https://github.com/rmnaderdev/Whitelist-Sync-2/commit/5147ea1174c18bf529813a54e5f2f490e08f6707))
* centralize supported versions and dynamic module selection ([473a85f](https://github.com/rmnaderdev/Whitelist-Sync-2/commit/473a85f9fb889ea3b1744f81383c184d8712c8fb))


### Bug Fixes

* Concurrency ([bfb44d2](https://github.com/rmnaderdev/Whitelist-Sync-2/commit/bfb44d260b50ec67a4ca33fe5b0c610c37136634))
* Correct version string conversion in NeoForge ServerControl ([8a772fc](https://github.com/rmnaderdev/Whitelist-Sync-2/commit/8a772fc10b5909b0ca78926c3fab7c1fe423da98))
* Enable and fix version checking for existing Fabric modules ([9e9ce98](https://github.com/rmnaderdev/Whitelist-Sync-2/commit/9e9ce983c879015098b39ea76509fa1d38f190c3))
* gate bans by active mode not web only ([bb1f403](https://github.com/rmnaderdev/Whitelist-Sync-2/commit/bb1f4033f3d98495654380497f785c8a9e551b15))
* Incorrect logging ([a3d7dd9](https://github.com/rmnaderdev/Whitelist-Sync-2/commit/a3d7dd925e32aed93707c586ecaa7a75f44d46c3))
* json reader crash on shaded gson 2.8.5 ([7a4310f](https://github.com/rmnaderdev/Whitelist-Sync-2/commit/7a4310f5c3c5468c9a8ada0cb1b2cdaf7ed80714))
* **postgres:** need to use rs.getBoolean instead of rs.getInt for binary column ([74870fb](https://github.com/rmnaderdev/Whitelist-Sync-2/commit/74870fb29dc5c18b1a709d9071325d40e942c9c4))
* Removed hardcoded useSSL url parameter from mysql connection string. Make it configurable. ([0da98ef](https://github.com/rmnaderdev/Whitelist-Sync-2/commit/0da98eff4b913d2792f717b7bf1ac0ff8df4e3c2))
* Unperforming O(n2) sync logic. Use hash set instead of scanning entire list each loop ([e2a0206](https://github.com/rmnaderdev/Whitelist-Sync-2/commit/e2a0206467a74d15824354568dc4347af54d266f))

## [2.9.0] - 2026-07-06
- Added support for new Minecraft versions for NeoForge and Fabric:
  - fabric-26.1.2
  - fabric-26.2
  - neoforge-26.1.2
  - neoforge-26.2
- Removed Forge subprojects — ForgeGradle is not compatible with Gradle 9.
- Deduplicated multiversion sources and build scripts into shared Fabric/NeoForge build logic.
- Reworked CI into a per-subproject matrix build on Java 21 + 25 to fix OOM and support MC 26.x.
- Fixed NeoForge 26.2 coremod conflict by relocating the bundled Gson.

## [2.8.3] - 2025-12-14
- Added support for new Minecraft versions for NeoForge and Fabric:
  - fabric-1.21.11
  - neoforge-1.21.11
- Added SQLite and MySQL database drivers back into the mod jar. **External connector mod jars are no longer required.**
- Refactored package names to be consistent across all versions and loaders.
- Fixed bug with command mixin not casting to correct object type causing error on command execution.
- Fixed version checker not working.
- Fixed issue reading banned player and ip json when it contains a record with no reason.

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
