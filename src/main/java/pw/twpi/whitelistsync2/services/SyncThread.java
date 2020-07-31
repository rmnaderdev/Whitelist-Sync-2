package pw.twpi.whitelistsync2.services;

import net.minecraft.server.MinecraftServer;
import pw.twpi.whitelistsync2.WhitelistSync2;
import pw.twpi.whitelistsync2.config.ConfigHandler;

import java.nio.file.FileSystem;
import java.nio.file.WatchService;


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
        if (service.getClass().equals(MySqlService.class)) {
            while (server.isServerRunning()) {
                service.copyDatabaseWhitelistedPlayersToLocal(server);

                if (ConfigHandler.SYNC_OP_LIST) {
                    service.copyDatabaseOppedPlayersToLocal(server);
                }

                try {
                    Thread.sleep(ConfigHandler.mysqlServerSyncTimer * 1000);
                } catch (InterruptedException ignored) { }
            }
        } else if (service.getClass().equals(SqLiteService.class)) {
            while (server.isServerRunning()) {
                service.copyDatabaseWhitelistedPlayersToLocal(server);

                if (ConfigHandler.SYNC_OP_LIST) {
                    service.copyDatabaseOppedPlayersToLocal(server);
                }

                try {
                    Thread.sleep(ConfigHandler.sqliteServerSyncTimer * 1000);
                } catch (InterruptedException e) {}

            }
        } else {
            WhitelistSync2.LOGGER.error("Error in the Sync Thread! "
                    + "Nothing will be synced! Please report to author!");
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
            }
        }

    }

}
