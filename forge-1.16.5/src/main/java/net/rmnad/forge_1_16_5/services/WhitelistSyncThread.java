package net.rmnad.forge_1_16_5.services;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.WhitelistEntry;
import net.rmnad.forge_1_16_5.Config;
import net.rmnad.forge_1_16_5.WhitelistSync2;
import net.rmnad.json.OppedPlayersFileReader;
import net.rmnad.json.WhitelistedPlayersFileReader;
import net.rmnad.Log;
import net.rmnad.services.BaseService;

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
            while (server.isRunning()) {
                service.pullDatabaseWhitelistToLocal(
                        WhitelistedPlayersFileReader.getWhitelistedPlayers(WhitelistSync2.SERVER_FILEPATH),
                        (uuid, name)->{
                            // Called when user added to whitelist
                            server.getPlayerList().getWhiteList().add(new WhitelistEntry(new GameProfile(uuid, name)));
                        },
                        (uuid, name) -> {
                            // Called when user removed from whitelist
                            server.getPlayerList().getWhiteList().remove(new WhitelistEntry(new GameProfile(uuid, name)));
                        }
                );

                if (syncOpLists) {
                    service.pullDatabaseOpsToLocal(
                            OppedPlayersFileReader.getOppedPlayers(WhitelistSync2.SERVER_FILEPATH),
                            (uuid, name)->{
                                // Called when user added to op list
                                // TODO: Add level and bypassesPlayerLimit
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
                } catch (InterruptedException ignored) {}
            }
        } catch (Exception e) {
            Log.error("Error in the whitelist sync thread! Syncing will stop until the server is restarted.", e);
        }
    }

}
