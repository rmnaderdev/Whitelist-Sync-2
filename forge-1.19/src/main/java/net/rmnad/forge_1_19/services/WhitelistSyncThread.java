package net.rmnad.forge_1_19.services;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.UserWhiteListEntry;
import net.rmnad.forge_1_19.Config;
import net.rmnad.forge_1_19.WhitelistSync2;
import net.rmnad.json.OppedPlayersFileReader;
import net.rmnad.json.WhitelistedPlayersFileReader;
import net.rmnad.Log;
import net.rmnad.logging.LogMessages;
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
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {}

        Log.info(LogMessages.SYNC_THREAD_STARTING);

        if(this.errorOnSetup) {
            Log.error(LogMessages.ERROR_INITIALIZING_WHITELIST_SYNC_DATABASE);
            return;
        }

        try {
            while (server.isRunning()) {
                service.pullDatabaseWhitelistToLocal(
                        WhitelistedPlayersFileReader.getWhitelistedPlayers(WhitelistSync2.SERVER_FILEPATH),
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
            Log.error(LogMessages.ERROR_WHITELIST_SYNC_THREAD, e);
        }
    }

}
