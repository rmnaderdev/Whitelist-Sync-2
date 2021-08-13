package pw.twpi.whitelistsync2.services;

import java.nio.file.FileSystem;
import java.nio.file.WatchService;
import net.minecraft.server.MinecraftServer;
import pw.twpi.whitelistsync2.Config;
import pw.twpi.whitelistsync2.WhitelistSync2;

/**
 * 
 * @author Richard Nader, Jr. <rmnader@svsu.edu>
 */
public class SyncThread implements Runnable {
    
    private final MinecraftServer server;
    private final BaseService service;

    // Watch Listener
    private FileSystem fileSystem;
    private WatchService watcher;

    public SyncThread(MinecraftServer server, BaseService service) {
        this.server = server;
        this.service = service;
    }
    
    @Override
    public void run() {
        try {
            while (server.isRunning()) {
                service.copyDatabaseWhitelistedPlayersToLocal(server);

                if (Config.SYNC_OP_LIST.get()) {
                    service.copyDatabaseOppedPlayersToLocal(server);
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
