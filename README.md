_Are you a server owner who is sick of adding individual players to each of your 100 forge servers? Wait you only have 2 forge servers? Well this mod can help you still!_

Introducing Whitelist Sync!
===========================


### A mod that allows you to sync the whitelists and ops list from multiple Forge and Spigot servers together using one MySQL or SQlite database!
This mod allows you to run a single /wl add &lt;player&gt; or /wlop op &lt;player&gt; on one of your servers and it will update all of your other forge servers running this mod connected to the proper database!

Looking for a spigot plugin? Find it on the [WhitelistSync2-Spigot GitHub](https://github.com/PotatoSauceVFX/Whitelist-Sync-2-Spigot/releases "WhitelistSync2-Spigot GitHub") page.

## News
**[7/31/20]** Version 2.2.1 has been released and includes some bug fixes. **PLEASE NOTE: Once you initially run this version on a MySQL database when using the op sync feature, the older versions of the mod will no longer work and will give SQL errors due to changes in the data structure. To still use older versions of this mod, disable the op sync feature of the older versions or contact me for specific changes.**

**[5/11/22]** Version 2.3.1+ will no longer support postgreSQL due to instability. postgreSQL support may return in the future, but I will not be maintaining the implementation for this feature.

**[5/24/22]** Whitelist Sync version 2.4.0+ for Minecraft versions >1.16.5 will now have it's config file located in the normal config folder in the server root folder. The file name is whitelistsync-common.toml.

**[8/16/22]** This project is primarily for my use as I don't like to put my free-time into it. It is a tool I use and I am happy to keep it open source for others to use as well, but I no longer maintain this project for other people to use. This is part of the reason I no longer maintain it on CurseForge (among other strong feelings I have about the CurseForge platform). I would love to maintain this project for others to use, but this project has ate up so much of my free-time fighting with annoying Forge issues and it lacks any financial incentive to make up for that time. I will still accept issue reports, I just might not get to them in a timely manner. 

## Supported Versions
- 1.19.2
- 1.19
- 1.18.2
- 1.16.5
- 1.12.2

*Versions not listed above can be requested, but I will decide whether it is worth my time. Feel free to make a pull request!*

## Installation

#### For SQLite database config:
- Download this mod and add it to your mods folder.
- Run your server once and setup the config (whitelistsync.cfg/whitelistsync-common.toml located in the config folder) and configure your database settings and timers.
- NOTE: Be sure to make the database path the same for all of the servers you want to link together!
- When the server runs it is going to make a new database unless one already exists. If you want to push your current whitelist to the new database use "/wl copyServerToDatabase" then you can start all of your other servers and they will sync to the database.

#### For MySQL server database setup:
- Download this mod and add it to your mods folder.
- Run your server once and setup the config (whitelistsync.cfg/whitelistsync-common.toml located in the config folder) and configure your database settings and timers.
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
