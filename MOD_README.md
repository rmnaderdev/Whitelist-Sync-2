_Are you a server owner who is sick of adding individual players to each of your 100 forge servers? Wait you only have 2 forge servers? Well this mod can help you still!_

Introducing Whitelist Sync!
===========================


### A mod that allows you to sync the whitelists, ops list, banned players (Web only), and banned IPs (Web only) from multiple Minecraft servers together using one MySQL db, SQLite db, or the NEW [Whitelist Sync Web](https://info.whitelistsync.com/docs/WhitelistSyncWeb.html) service!
This mod listens for whitelist, op, ban, and ban-ip changes (using the normal Minecraft commands) on all of your servers and will update all other forge servers connected to the same database.

## Whitelist Sync Web
Whitelist Sync Web is a new service that allows you to sync your whitelist, ops, banned players, and banned IPs across all of your servers in real-time. This service is a paid service, but it does have a free tier for you to try out. This service is the easiest way to sync your servers and requires no setup on your end other than installing the mod and setting the API key. Interested in learning more? [Click here](https://info.whitelistsync.com/docs/WhitelistSyncWeb.html).

## Download
All releases can be found on the [Modrinth](https://modrinth.com/mod/wlsync2) page or on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/whitelistsync2).

## Support/Questions/Feedback
For issues with the mod please open an issue on [GitHub](https://github.com/rmnaderdev/Whitelist-Sync-2/issues). For other support requests, feature requests, or other general discussion. Please join the [Discord Server](https://discord.gg/7FMJN4kurr).

## Support the project
If you like this project and want to support it, consider donating to my [PayPal](https://www.paypal.com/paypalme/PSVFX), [buymeacoffee](https://buymeacoffee.com/potatodotjar) or by using the new [Whitelist Sync Web](https://info.whitelistsync.com/docs/WhitelistSyncWeb.html) service! Any amount is appreciated!

## Supported Forge Versions
- 1.21.3
- 1.21.1
- 1.21
- 1.20.6
- 1.20.2
- 1.20.1
- 1.19.2
- 1.19
- 1.18.2
- 1.16.5
- ~~1.12.2~~ (no longer supported, may come back in the future)

## Supported Fabric Versions
- 1.21.4
- 1.21.3
- 1.21.2
- 1.21.1
- 1.21

## Supported NeoForge Versions
- 1.21.4
- 1.21.3
- 1.21.1
- 1.21

*Versions not listed above **and before 1.16.5** can be requested as an issue. I will try my best to accommodate. Whitelist Sync Web subscribers take priority.*

## Installation

#### For Web Sync (Recommended, easiest setup):
- See the Whitelist Sync Web setup on the [dedicated page](https://info.whitelistsync.com/docs/WhitelistSyncWeb.html)

#### For SQLite database config (Self-hosted):
- Download this mod AND the [SQLite DB connector](https://modrinth.com/plugin/sqlite-jdbc) mod and add both jar files to your mods folder.
- Run your server once and set up the config (whitelistsync.cfg/whitelistsync-common.toml located in the config folder) and configure your database settings and timers.
- NOTE: Be sure to make the database path the same for all the servers you want to link together!
- When the server runs it is going to make a new database unless one already exists. If you want to push your current whitelist to the new database use "/wl copyServerToDatabase" then you can start all of your other servers and they will sync to the database.

#### For MySQL database (Self-hosted):
- Download this mod AND the [MySQL DB connector](https://modrinth.com/plugin/mysql-jdbc) mod and add both jar files to your mods folder.
- Run your server once and set up the config (whitelistsync.cfg/whitelistsync-common.toml located in the config folder) and configure your database settings and timers.
- Set up your mySQL database IP, port, and authentication as well as the Sync Timer.
- Run server and make sure the database connects. **Note:** Mod will make its own database and table for you or connect to an existing one.
- Push your existing whitelist to the server (/wl copyServerToDatabase) or pull changes from your database (/wl sync).
- Enjoy!

## Commands
- /wl sync [whitelist|ops|banned-players|banned-ips] | Refreshes local synced data from database.
- /wl push [whitelist|ops|banned-players|banned-ips] | Pushes local server data to the database. Use this if you want to update the database with the current server data.
- /wl restart | Restarts the sync thread. Useful if the sync thread stops or has issues.

## Legacy Commands (For v2.6.0 and below ONLY!)

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
- [x] Allow user to sync/push only whitelist, ops, or bans

## Future Plans
- [x] Add support for syncing ban lists
- [x] Add support for Fabric
- [ ] Add support for BungeeCord
- [ ] Add support for Spigot
- [ ] Add support for Paper

Please post your bugs to [GitHub](https://github.com/rmnaderdev/Whitelist-Sync-2/issues "GitHub") or better yet make a pull request!
