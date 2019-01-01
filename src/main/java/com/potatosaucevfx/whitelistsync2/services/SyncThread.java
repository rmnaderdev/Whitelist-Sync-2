package com.potatosaucevfx.whitelistsync2.services;

import com.potatosaucevfx.whitelistsync2.WhitelistSync2;
import com.potatosaucevfx.whitelistsync2.config.ConfigHandler;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import net.minecraft.server.MinecraftServer;


/**
 * 
 * @author Richard Nader, Jr. <nader1rm@cmich.edu>
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
                service.updateLocalWhitelistFromDatabase(server);

                if (ConfigHandler.SYNC_OP_LIST) {
                    service.updateLocalOpListFromDatabase(server);
                }

                try {
                    Thread.sleep(ConfigHandler.mysqlServerSyncTimer * 1000);
                } catch (InterruptedException e) {
                }
            }
        } else if (service.getClass().equals(SqLiteService.class)) {

            if (ConfigHandler.sqliteMode.equalsIgnoreCase("INTERVAL")) {
                while (server.isServerRunning()) {
                    service.updateLocalWhitelistFromDatabase(server);

                    if (ConfigHandler.SYNC_OP_LIST) {
                        service.updateLocalOpListFromDatabase(server);
                    }

                    try {
                        Thread.sleep(ConfigHandler.sqliteServerSyncTimer * 1000);
                    } catch (InterruptedException e) {}
                }
            } else if (ConfigHandler.sqliteMode.equalsIgnoreCase("LISTENER")) {
                checkSQliteDB();
            }

        } else {
            WhitelistSync2.logger.error("Error in the Sync Thread! "
                    + "Nothing will be synced! Please report to author!");
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
            }
        }

    }

    private void checkSQliteDB() {
        try {
            this.fileSystem = FileSystems.getDefault();
            this.watcher = fileSystem.newWatchService();

            Path dataBasePath = fileSystem.getPath(ConfigHandler.sqliteDatabasePath.replace("whitelist.db", ""));
            dataBasePath.register(watcher, ENTRY_MODIFY);

        } catch (IOException e) {
            WhitelistSync2.logger.error("Error finding whitelist database file. "
                    + "This should not happen, please report.\n" + e.getMessage());
        }

        while (server.isServerRunning()) {
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                // Test if whitelist is changed
                if (event.context().toString().equalsIgnoreCase("whitelist.db")) {
                    WhitelistSync2.logger.debug("Remote Database Updated... Syncing...");
                    service.updateLocalWhitelistFromDatabase(server);

                    if (ConfigHandler.SYNC_OP_LIST) {
                        service.updateLocalOpListFromDatabase(server);
                    }
                }
            }

            // Reset the key -- this step is critical if you want to
            // receive further watch events.  If the key is no longer valid,
            // the directory is inaccessible so exit the loop.
            boolean valid = key.reset();
            if (!valid) {
                break;
            }

            try {
                Thread.sleep(ConfigHandler.sqliteServerListenerTimer * 1000);
            } catch (InterruptedException e) {}

        }
    }

}
