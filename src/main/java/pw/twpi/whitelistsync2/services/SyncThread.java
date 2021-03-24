package pw.twpi.whitelistsync2.services;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
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
        if (service.getClass().equals(MySqlService.class)) {
            while (server.isRunning()) {
                service.copyDatabaseWhitelistedPlayersToLocal(server);

                if (Config.SYNC_OP_LIST.get()) {
                    service.copyDatabaseOppedPlayersToLocal(server);
                }

                try {
                    Thread.sleep(Config.MYSQL_SYNC_TIMER.get() * 1000);
                } catch (InterruptedException ignored) { }
            }
        } else if (service.getClass().equals(SqLiteService.class)) {
            while (server.isRunning()) {
                service.copyDatabaseWhitelistedPlayersToLocal(server);

                if (Config.SYNC_OP_LIST.get()) {
                    service.copyDatabaseOppedPlayersToLocal(server);
                }

                try {
                    Thread.sleep(Config.SQLITE_SERVER_SYNC_TIMER.get() * 1000);
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
