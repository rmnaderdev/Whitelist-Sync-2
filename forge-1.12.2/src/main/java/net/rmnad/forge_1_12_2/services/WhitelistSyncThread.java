package net.rmnad.forge_1_12_2.services;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.UserListWhitelistEntry;
import net.rmnad.forge_1_12_2.Config;
import net.rmnad.forge_1_12_2.WhitelistSync2;
import net.rmnad.core.json.OppedPlayersFileReader;
import net.rmnad.core.json.WhitelistedPlayersFileReader;
import net.rmnad.core.Log;
import net.rmnad.core.services.BaseService;

public class WhitelistSyncThread extends Thread {
    
    private final MinecraftServer server;
    private final BaseService service;
    private final boolean syncOpLists;
    private final boolean errorOnSetup;

    public WhitelistSyncThread(MinecraftServer server, BaseService service, boolean syncOpLists, boolean errorOnSetup) {
        this.server = server;
        this.service = service;
        this.syncOpLists = syncOpLists;
        this.errorOnSetup = errorOnSetup;
    }
    
    @Override
    public void run() {
        Log.info("Sync thread starting...");

        if(this.errorOnSetup) {
            Log.error("Error initializing whitelist sync database. Disabling mod functionality. Please correct errors and restart.");
            return;
        }

        try {
            while (server.isServerRunning()) {
                service.pullDatabaseWhitelistToLocal(
                        WhitelistedPlayersFileReader.getWhitelistedPlayers(WhitelistSync2.SERVER_FILEPATH),
                        (uuid, name)->{
                            // Called when user added to whitelist
                            server.getPlayerList().getWhitelistedPlayers().addEntry(new UserListWhitelistEntry(new GameProfile(uuid, name)));
                        },
                        (uuid, name) -> {
                            // Called when user removed from whitelist
                            server.getPlayerList().getWhitelistedPlayers().removeEntry(new GameProfile(uuid, name));
                        }
                );

                if (syncOpLists) {
                    service.pullDatabaseOpsToLocal(
                            OppedPlayersFileReader.getOppedPlayers(WhitelistSync2.SERVER_FILEPATH),
                            (uuid, name)->{
                                // Called when user added to op list
                                // TODO: Add level and bypassesPlayerLimit
                                server.getPlayerList().addOp(new GameProfile(uuid, name));
                            },
                            (uuid, name) -> {
                                // Called when user removed from op list
                                server.getPlayerList().removeOp(new GameProfile(uuid, name));
                            }
                    );
                }

                try {
                    Thread.sleep(Config.SYNC_TIMER * 1000L);
                } catch (InterruptedException ignored) {}
            }
        } catch (Exception e) {
            Log.error("Error in the whitelist sync thread! Syncing will stop until the server is restarted.", e);
        }
    }

}
