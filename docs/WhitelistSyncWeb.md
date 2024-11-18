# Introducing Whitelist Sync Web!

A service that allows you to seamlessly sync your whitelisted players, opped players, banned players, and banned IPs across multiple Minecraft forge servers in real-time.

## Features

- Sync your whitelisted players, opped players, banned players, and banned IPs across multiple servers in real-time
- Manage your servers and accounts with ease using a simple web interface
- Give access to your entire team and allow any team member to manage server whitelists and op lists
- Securely manage your server connections with API keys that can be revoked at any time
- No database setup or hosting needed
- Need custom functionality for managing your players? Use the available API endpoints to manage accounts with custom code (Discord bots, application services, etc). *More documentation to come in the future...*
- Support for syncing banned players and banned IPs
- Currently supporting Minecraft Forge and Fabric. Spigot versions are planned depending on demand

### Video Demo

<video src='./assets/whitelisting.mp4' width=600></video>

<video src='./assets/opping-banning.mp4' width=600></video>

## Pricing and Beta Access

**This platform is currently in testing and may contain bugs and downtime.** Access is only allowed to pre-authorized users. If you would like to apply to access the platform while it is in beta, you may apply by joining the Whitelist Sync Discord server and filling out the application here: [Apply to the beta/trial here!](https://discord.gg/CrqvEVRmWS)

### Free Tier
The free/trial tier gives a user access to Whitelist Sync Web with up-to 25 player syncing (under 1 account), 1 user to manage the account (you), and real-time syncing (connections) with up to 2 running Minecraft servers. Ideal for a small group of friends who play on up-to two different Minecraft servers.
* 25 player syncing (whitelist and ops only)
* 1 user (you)
* Real-time syncing with up to 2 running Minecraft servers. For example, if there are 2 separate Minecraft server instances running,  then connecting a 3rd Minecraft server will be over the limit and this server will not receive sync updates or be able to pull/push updates.

### Tier 1 Plan ($3/month)
Gives a user access to Whitelist Sync Web with unlimited player syncing (under 1 account), up-to 1 user (you) + 4 other users with manage access for the account (server group), and real-time syncing (connections) with up to 10 running Minecraft servers.
* Unlimited player syncing (whitelist, ops, banned players, banned IPs)
* 1 user (you) + 4 other users with manage players for the account (these 4 extra users can log in and manage the player lists using a separate login)
* Real-time syncing with up to 10 running Minecraft servers. For example, if there are 10 separate Minecraft server instances running, then connecting a 11th Minecraft server will be over the limit and this server fail to establish a connection and will not receive sync updates or be able to pull/push updates.

### Tier 2 Plan ($8/month)
Gives a user access to Whitelist Sync Web with unlimited player syncing (under 1 account), up-to 1 user (you) + 24 other users with manage access for the account (server group), and real-time syncing (connections) with up to 25 running Minecraft servers.
* Unlimited player syncing (whitelist, ops, banned players, banned IPs)
* 1 user (you) + 24 other users with manage players for the account (these 24 extra users can log in and manage the player lists using a separate login)
* Real-time syncing with up to 25 running Minecraft servers. For example, if there are 10 separate Minecraft server instances running, then connecting a 26th Minecraft server will be over the limit and this server fail to establish a connection and will not receive sync updates or be able to pull/push updates.

## Getting Started

1. To get started, you will need to install the Whitelist Sync Forge mod on your Minecraft servers. You can download the mod from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/whitelistsync2) OR [Modrinth](https://modrinth.com/mod/whitelistsync2).  
   *Mod version v2.6.X+ is required to work with this service*
2. Once you have installed the mod on your servers, you will need to create an account using the accounts link at the top of this page. An account can be thought of as a group of servers that will have their whitelisted players and opped players synced.
3. After creating an account, you can access the different player access lists by clicking the buttons in the account row. You will see 4 buttons: **Whitelist**, **Ops**, **Banned Players**, and **Banned IPs**. Clicking on these buttons will take you to a page where you can manage the players on the list.
4. Account admins will see an additional **Manage** button. This manage button will take you to an additional page where account admins can manage the Api Keys and Users for the account. Users are added by their email and will need to have an account on the site before they can be added to an account.
5. To configure the Whitelist Sync Forge mod, you will need to generate an API key on the accounts page. It is recommended to make an API key for each server you will be adding to the account, naming it accordingly. This will make it easier to revoke access to a specific server if needed. Once you create an API key, copy the code it gives you (you will not be able to see this code again). Add the code to your `whitelistsync2-common.toml` config file on the forge server:  
   Make sure the `databaseMode` is set to `"WEB"` and the `webApiKey` setting under `[web]` is set to the API key you copied.

```toml
[general]
  databaseMode = "WEB"

...

[web]
  #API Key for the web service. You can generate one by logging into the web service and adding a new API key to your account.
  webApiKey = "[API KEY HERE]"
  #Host for the web service. This should be the URL of the web service. You should never need to change this.
  webApiHost = "https://whitelistsync.com/"
  #Option to enable banned players sync.
  webSyncBannedPlayers = true
  #Option to enable banned IPs sync.
  webSyncBannedIps = true

...

```

6. Once your forge server is running and no errors are shown on startup, you can push your existing whitelisted players, ops, banned players, and banned ips using the /wl push command. All access lists are managed on the servers using the vanilla Minecraft server commands. Whitelist sync will automatically sync the lists across all servers in the account in real time. See the documentation for the forge mod on the Curseforge or Modrinth pages linked above.

## Feedback

If you have any feedback, feature requests, or questions, please create a discussion on the Discord server [Discord Server](https://discord.gg/7FMJN4kurr) page.

