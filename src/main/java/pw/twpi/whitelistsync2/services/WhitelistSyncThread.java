package pw.twpi.whitelistsync2.services;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.WhitelistEntry;
import net.rmnad.minecraft.forge.whitelistsynclib.services.BaseService;
import pw.twpi.whitelistsync2.Config;
import pw.twpi.whitelistsync2.WhitelistSync2;
import pw.twpi.whitelistsync2.json.OppedPlayersFileUtilities;
import pw.twpi.whitelistsync2.json.WhitelistedPlayersFileUtilities;

/**
 * 
 * @author Richard Nader, Jr. <rmnader@svsu.edu>
 */
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
        // Delay thread start for 5 sec
        WhitelistSync2.LOGGER.info("Delay start of sync thread, waiting 5 sec...");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ignored) {
        }
        WhitelistSync2.LOGGER.info("Sync thread starting...");

        if(!this.service.initializeDatabase() || this.errorOnSetup) {
            WhitelistSync2.LOGGER.error("Error initializing whitelist sync database. Disabling mod functionality. Please correct errors and restart.");
        } else {
            // Database is setup!

            // Check if whitelisting is enabled.
            if (!server.getPlayerList().isUsingWhitelist()) {
                WhitelistSync2.LOGGER.info("Oh no! I see whitelisting isn't enabled in the server properties. "
                        + "I assume this is not intentional, I'll enable it for you!");
                server.getPlayerList().setUsingWhiteList(true);
            }
        }

        try {
            while (server.isRunning()) {
                service.copyDatabaseWhitelistedPlayersToLocal(
                        WhitelistedPlayersFileUtilities.getWhitelistedPlayers(),
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
        } catch (Exception e) {
            WhitelistSync2.LOGGER.error("Error in the whitelist sync thread! Syncing will stop until the server is restarted.", e);
        }
    }

}
