_Are you a server owner who is sick of adding individual players to each of your 100 forge servers? Wait you only have 2 forge servers? Well this mod can help you still!_

Introducing Whitelist Sync!
===========================


### A mod that allows you to sync the whitelists and ops list from multiple Forge and Spigot servers together using one MySQL or SQlite database!
This mod allows you to run a single /wl add &lt;player&gt; or /wlop op &lt;player&gt; on one of your servers and it will update all of your other forge servers running this mod connected to the proper database!

Looking for a spigot plugin? Find it on the [WhitelistSync2-Spigot GitHub](https://github.com/PotatoSauceVFX/Whitelist-Sync-2-Spigot/releases "WhitelistSync2-Spigot GitHub") page.

## News
**[7/31/20]** Version 2.2.1 has been released and includes some bug fixes. **PLEASE NOTE: Once you initially run this version on a MySQL database when using the op sync feature, the older versions of the mod will no longer work and will give SQL errors due to changes in the data structure. To still use older versions of this mod, disable the op sync feature of the older versions or contact me for specific changes.**

~~**[9/29/21]** Version 2.2.5+ now supports postgreSQL. If you need postgreSQL for an older version of MC (&lt; 1.16.5), let me know and I can add it to my list of things to do.~~

**[5/11/22]** Version 2.3.1+ will no longer support postgreSQL due to instability. postgreSQL support may return in the future, but I will not be maintaining the implementation for this feature.

**[5/24/22]** Whitelist Sync version 2.4.0+ for Minecraft versions >1.16.5 will now have it's config file located in the normal config folder in the server root folder. The file name is whitelistsync-common.toml.

## Supported Versions
- 1.18.X
- 1.16.5
- 1.12.2

*Versions not listed above can be requested, but I will decide whether or not it is worth my time.*

## Installation

#### For SQLite database config:
- Download this mod and add it to your mods folder.
- Run your server and setup the config (whitelistsync.cfg/whitelistsync-common.toml) and configure your update settings and timers.
- NOTE: Be sure to make the database path the same for all of the servers you want to link together!
- When the server runs it is going to make a new database. If you want to push your current whitelist to the database use "/wl copyServerToDatabase" then you can start all of your other servers and they will sync to the database.

#### For MySQL server database setup:
- Download this mod and add it to your mods folder.
- Run your server and setup the config (whitelistsync.cfg/whitelistsync-common.toml).
- Setup your mySQL database IP, port, and authentication as well as the Sync Timer.
- Run server and make sure the database connects. **Note:** Mod will make it's own database and table for you or connect to an existing one.
- Push your existing whitelist to the server (/wl copyServerToDatabase) or pull changes from your database (/wl sync).
- Enjoy!

## Commands

#### Whitelist Control
- /wl add &lt;player&gt; | Adds a specified player to whitelist. (**Use this instead of /whitelist add**)
- /wl remove &lt;player&gt; | Removes a specified player from the whitelist. (**Use this instead of /whitelist remove**)
- /wl list | Lists users whitelisted across all servers.
- /wl sync | Pulls whitelist from the database and updates the local server whitelist.
- /wl copyServerToDatabase | Pushes local server whitelist to the database and merges them in.

#### Op Control
- /wlop op &lt;player&gt; | Ops a specified player. (**Use this instead of /op**)
- /wlop deop &lt;player&gt; | De-ops a specified player. (**Use this instead of /deop**)
- /wlop list | Lists users who are ops across all servers.
- /wlop sync | Pulls list of ops from the database and updates local server ops.
- /wlop copyServerToDatabase | Pushes local server ops to database and merges them in.

Please post your bugs to [GitHub](https://github.com/PotatoSauceVFX/Whitelist-Sync-2/issues "GitHub") or better yet make a pull request!
