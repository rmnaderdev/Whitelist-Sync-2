_Are you a server owner who is sick of adding individual players to each of your 100 forge servers? Wait you only have 2 forge servers? Well this mod can help you still!_

Introducing Whitelist Sync!
===========================


### A mod that allows you to sync the whitelists and ops list from multiple Minecraft servers together using one MySQL or SQlite database!
This mod allows you to run a single /wl add &lt;player&gt; or /wlop op &lt;player&gt; on one of your servers and it will update all of your other forge servers running this mod connected to the proper database!

## Supported Versions
- 1.19.2
- 1.19
- 1.18.2
- 1.16.5
- 1.12.2

*Versions not listed above can be requested as an issue. I will try my best to accommodate.*

## Installation

#### For SQLite database config:
- Download this mod AND the [SQLite DB connector](https://modrinth.com/plugin/sqlite-jdbc) mod and add both jar files to your mods folder.
- Run your server once and setup the config (whitelistsync.cfg/whitelistsync-common.toml located in the config folder) and configure your database settings and timers.
- NOTE: Be sure to make the database path the same for all of the servers you want to link together!
- When the server runs it is going to make a new database unless one already exists. If you want to push your current whitelist to the new database use "/wl copyServerToDatabase" then you can start all of your other servers and they will sync to the database.

#### For MySQL server database setup:
- Download this mod AND the [MySQL DB connector](https://modrinth.com/plugin/mysql-jdbc) mod and add both jar files to your mods folder.
- Run your server once and setup the config (whitelistsync.cfg/whitelistsync-common.toml located in the config folder) and configure your database settings and timers.
- Setup your mySQL database IP, port, and authentication as well as the Sync Timer.
- Run server and make sure the database connects. **Note:** Mod will make its own database and table for you or connect to an existing one.
- Push your existing whitelist to the server (/wl copyServerToDatabase) or pull changes from your database (/wl sync).
- Enjoy!

## Commands

#### Whitelist Control
- /wl add &lt;player&gt; | Adds a specified player to whitelist. (**Use this instead of /whitelist add**)
- /wl remove &lt;player&gt; | Removes a specified player from the whitelist. (**Use this instead of /whitelist remove**)
- /wl list | Lists users whitelisted across all servers.
- /wl sync | Pulls whitelist from the database and updates the local server whitelist.
- /wl push | Pushes local server whitelist to the database and merges them in. (Versions less than v1.5.0 use /wl copyServerToDatabase)

#### Op Control
- /wlop op &lt;player&gt; | Ops a specified player. (**Use this instead of /op**)
- /wlop deop &lt;player&gt; | De-ops a specified player. (**Use this instead of /deop**)
- /wlop list | Lists users who are ops across all servers.
- /wlop sync | Pulls list of ops from the database and updates local server ops.
- /wlop push | Pushes local server ops to database and merges them in. (Versions less than v1.5.0 use /wlop copyServerToDatabase)

The mod uses polling to check for changes in the database and will update the local server whitelist and ops list accordingly. The polling interval can be set in the config file.

## To-Do
- [ ] Add support for Fabric
- [ ] Add support for level and bypassPlayerLimit for op lists
- [ ] Add support for syncing ban lists
- [ ] Add support for BungeeCord
- [ ] Add support for Spigot
- [ ] Add support for Paper

Please post your bugs to [GitHub](https://github.com/rmnaderdev/Whitelist-Sync-2/issues "GitHub") or better yet make a pull request!



## Appending forge README.md
Source installation information for modders
-------------------------------------------
This code follows the Minecraft Forge installation methodology. It will apply
some small patches to the vanilla MCP source code, giving you and it access
to some of the data and functions you need to build a successful mod.

Note also that the patches are built against "un-renamed" MCP source code (aka
SRG Names) - this means that you will not be able to read them directly against
normal code.

Setup Process:
==============================

Step 1: Open your command-line and browse to the folder where you extracted the zip file.

Step 2: You're left with a choice.
If you prefer to use Eclipse:
1. Run the following command: `./gradlew genEclipseRuns`
2. Open Eclipse, Import > Existing Gradle Project > Select Folder
   or run `gradlew eclipse` to generate the project.

If you prefer to use IntelliJ:
1. Open IDEA, and import project.
2. Select your build.gradle file and have it import.
3. Run the following command: `./gradlew genIntellijRuns`
4. Refresh the Gradle Project in IDEA if required.

If at any point you are missing libraries in your IDE, or you've run into problems you can
run `gradlew --refresh-dependencies` to refresh the local cache. `gradlew clean` to reset everything
{this does not affect your code} and then start the process again.

Mapping Names:
=============================
By default, the MDK is configured to use the official mapping names from Mojang for methods and fields
in the Minecraft codebase. These names are covered by a specific license. All modders should be aware of this
license, if you do not agree with it you can change your mapping names to other crowdsourced names in your
build.gradle. For the latest license text, refer to the mapping file itself, or the reference copy here:
https://github.com/MinecraftForge/MCPConfig/blob/master/Mojang.md

Additional Resources:
=========================
Community Documentation: https://docs.minecraftforge.net/en/1.19.2/gettingstarted/
LexManos' Install Video: https://youtu.be/8VEdtQLuLO0
Forge Forums: https://forums.minecraftforge.net/
Forge Discord: https://discord.minecraftforge.net/
