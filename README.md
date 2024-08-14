_Are you a server owner who is sick of adding individual players to each of your 100 forge servers? Wait you only have 2 forge servers? Well this mod can help you still!_

Introducing Whitelist Sync!
===========================


### A mod that allows you to sync the whitelists and ops list from multiple Minecraft servers together using one MySQL or SQLite database!
This mod allows you to run a single /wl add &lt;player&gt; or /wlop op &lt;player&gt; on one of your servers and it will update all of your other forge servers running this mod connected to the proper database!

## New in v2.5.0
- No more shadowed JDBC dependencies! **MANUAL STEP REQUIRED for v2.5.0.** See Installation section below.
- `copyServerToDatabase` commands have been changed to `push`.

## Download
All releases can be found on the [Modrinth](https://modrinth.com/mod/whitelistsync2) page or [Curse Forge](https://www.curseforge.com/minecraft/mc-mods/whitelistsync2).

## Support the project
If you like this project and want to support it, consider donating to my [PayPal](https://www.paypal.com/paypalme/PSVFX)! Any amount is appreciated!

## Supported Versions
- 1.21
- 1.20.6
- 1.20.2
- 1.20.1
- 1.19.2
- 1.19
- 1.18.2
- 1.16.5
- ~~1.12.2~~ (no longer supported)

*Versions not listed above **and after 1.16.5** can be requested as an issue. I will try my best to accommodate.*

## Installation

#### For SQLite database config:
- Download this mod AND the [SQLite DB connector](https://modrinth.com/plugin/sqlite-jdbc) mod and add both jar files to your mods folder.
- Run your server once and set up the config (whitelistsync.cfg/whitelistsync-common.toml located in the config folder) and configure your database settings and timers.
- NOTE: Be sure to make the database path the same for all the servers you want to link together!
- When the server runs it is going to make a new database unless one already exists. If you want to push your current whitelist to the new database use "/wl copyServerToDatabase" then you can start all of your other servers and they will sync to the database.

#### For MySQL server database setup:
- Download this mod AND the [MySQL DB connector](https://modrinth.com/plugin/mysql-jdbc) mod and add both jar files to your mods folder.
- Run your server once and set up the config (whitelistsync.cfg/whitelistsync-common.toml located in the config folder) and configure your database settings and timers.
- Set up your mySQL database IP, port, and authentication as well as the Sync Timer.
- Run server and make sure the database connects. **Note:** Mod will make its own database and table for you or connect to an existing one.
- Push your existing whitelist to the server (/wl copyServerToDatabase) or pull changes from your database (/wl sync).
- Enjoy!

## Commands

#### Whitelist Control
- /wl add &lt;player&gt; | Adds a specified player to whitelist. (**Use this instead of /whitelist add**)
- /wl remove &lt;player&gt; | Removes a specified player from the whitelist. (**Use this instead of /whitelist remove**)
- /wl list | Lists users whitelisted across all servers.
- /wl sync | Pulls whitelist from the database and updates the local server whitelist.
- /wl push | Pushes local server whitelist to the database and merges them in. (Versions before v1.5.0 use /wl copyServerToDatabase)

#### Op Control
- /wlop op &lt;player&gt; | Ops a specified player. (**Use this instead of /op**)
- /wlop deop &lt;player&gt; | De-ops a specified player. (**Use this instead of /deop**)
- /wlop list | Lists users who are ops across all servers.
- /wlop sync | Pulls list of ops from the database and updates local server ops.
- /wlop push | Pushes local server ops to database and merges them in. (Versions before v1.5.0 use /wlop copyServerToDatabase)

The mod uses polling to check for changes in the database and will update the local server whitelist and ops list accordingly. The polling interval can be set in the config file.

## To-Do
- [x] Get the damn gradle project structure to work
- [ ] Add support for Fabric
- [ ] Add support for level and bypassPlayerLimit for op lists
- [ ] Add support for syncing ban lists
- [ ] Add support for BungeeCord
- [ ] Add support for Spigot
- [ ] Add support for Paper

Please post your bugs to [GitHub](https://github.com/rmnaderdev/Whitelist-Sync-2/issues "GitHub") or better yet make a pull request!
