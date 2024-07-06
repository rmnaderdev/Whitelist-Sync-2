package net.rmnad.forge_1_19_2.services;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.UserWhiteListEntry;
import net.rmnad.forge_1_19_2.Config;
import net.rmnad.forge_1_19_2.WhitelistSync2;
import net.rmnad.forge_1_19_2.json.OppedPlayersFileUtilities;
import net.rmnad.forge_1_19_2.json.WhitelistedPlayersFileUtilities;
import net.rmnad.whitelistsync2.Log;
import net.rmnad.whitelistsync2.services.BaseService;

/**
 * 

 */
public class WhitelistSyncThread extends Thread {
    
    private final MinecraftServer server;
    private final BaseService service;
    private final boolean syncOpLists;
    private boolean errorOnSetup;

    public WhitelistSyncThread(MinecraftServer server, BaseService service, boolean syncOpLists, boolean errorOnSetup) {
        this.server = server;
        this.service = service;
        this.syncOpLists = syncOpLists;
        this.errorOnSetup = errorOnSetup;
    }
    
    @Override
    public void run() {
        // Delay thread start for 5 sec
        Log.info("Delay start of sync thread, waiting 5 sec...");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ignored) {
        }
        Log.info("Sync thread starting...");

        if(!this.service.initializeDatabase() || this.errorOnSetup) {
            Log.error("Error initializing whitelist sync database. Disabling mod functionality. Please correct errors and restart.");
            this.errorOnSetup = true;
        } else {
            // Database is setup!

            // Check if whitelisting is enabled.
            if (!server.getPlayerList().isUsingWhitelist()) {
                Log.info("Oh no! I see whitelisting isn't enabled in the server properties. "
                        + "I assume this is not intentional, I'll enable it for you!");
                server.getPlayerList().setUsingWhiteList(true);
            }
        }

        if (this.errorOnSetup) {
            return;
        }

        try {
            while (server.isRunning()) {
                service.copyDatabaseWhitelistedPlayersToLocal(
                        WhitelistedPlayersFileUtilities.getWhitelistedPlayers(),
                        (uuid, name)->{
                            // Called when user added to whitelist
                            server.getPlayerList().getWhiteList().add(new UserWhiteListEntry(new GameProfile(uuid, name)));
                        },
                        (uuid, name) -> {
                            // Called when user removed from whitelist
                            server.getPlayerList().getWhiteList().remove(new UserWhiteListEntry(new GameProfile(uuid, name)));
                        }
                );

                if (syncOpLists) {
                    service.copyDatabaseOppedPlayersToLocal(
                            OppedPlayersFileUtilities.getOppedPlayers(),
                            (uuid, name)->{
                                // Called when user added to op list
                                server.getPlayerList().op(new GameProfile(uuid, name));
                            },
                            (uuid, name) -> {
                                // Called when user removed from op list
                                server.getPlayerList().deop(new GameProfile(uuid, name));
                            }
                    );
                }

                try {
                    Thread.sleep(Config.COMMON.SYNC_TIMER.get() * 1000);
                } catch (InterruptedException ignored) {
                }
            }
        } catch (Exception e) {
            Log.error("Error in the whitelist sync thread! Syncing will stop until the server is restarted.", e);
        }
    }

}
