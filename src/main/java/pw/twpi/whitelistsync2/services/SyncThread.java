package pw.twpi.whitelistsync2.services;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.UserWhiteListEntry;
import net.rmnad.minecraft.forge.whitelistsynclib.services.BaseService;
import pw.twpi.whitelistsync2.Config;
import pw.twpi.whitelistsync2.WhitelistSync2;
import pw.twpi.whitelistsync2.json.OppedPlayersFileUtilities;
import pw.twpi.whitelistsync2.json.WhitelistedPlayersFileUtilities;

/**
 * 
 * @author Richard Nader, Jr. <rmnader@svsu.edu>
 */
public class SyncThread implements Runnable {
    
    private final MinecraftServer server;
    private final BaseService service;

    public SyncThread(MinecraftServer server, BaseService service) {
        this.server = server;
        this.service = service;
    }
    
    @Override
    public void run() {
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

                if (Config.SYNC_OP_LIST.get()) {
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
                    Thread.sleep(Config.SYNC_TIMER.get() * 1000);
                } catch (InterruptedException ignored) {
                }
            }
        } catch (Throwable t) {
            WhitelistSync2.LOGGER.error("Error in the whitelist sync thread! Syncing will stop until the server is restarted.", t);
        }
    }

}
