# Whitelist Sync 2
Server Side Mod to sync all of your forge server whitelists to a single database (Currently supports SQLite or mySQL servers).

SEE https://www.curseforge.com/minecraft/mc-mods/whitelistsync2

[3/11/19] I am planning on fixing the issues in the beta version and in the previous version as soon as I can. I am a college student and work a part time SW dev job so those take my priority since I need money to pay the bills. Donations are appreciated and allow me to spend more time on this project. Otherwise I will make changes when I can. Thank you for understanding. :)

Are you a server owner who is sick of adding individual players to each of your 100 forge servers? Wait you only have 2 forge servers? Well this mod can help you still!

 

Introducing Whitelist Sync!

 

A mod that allows you to sync the whitelists and op lists from multiple forge servers together!

 

This mod allows you to run a single /wl add <player> or /wlop add player on one of your servers and it will update all of your other forge servers running this mod and connected to the same database!

AS OF VERSION 1.3 THIS MOD NO LONGER REQUIRES ADDITIONAL DEPENDENCIES!
 

Installation:

Just download this mod and add it to your mods folder. :)
Run your server and setup the config (whitelistsync.cfg).
NOTE: Be sure to make the sqlite database path the same for all of the servers you want to link together!
When the server runs it is going to make a new database. If you want to push your current server whitelist to the database use "/wl copyWhiteListToDatabase" then you can start all of your other server and they will sync to the database.
 

Commands:

/wl add <player> | Adds a specified player to whitelist. (Use this instead of /whitelist add)
/wl remove <player> | Removes a specified player from the whitelist. (Use this instead of /whitelist remove)
/wl copyServerToDatabase | Pushes local server whitelist to the database.
