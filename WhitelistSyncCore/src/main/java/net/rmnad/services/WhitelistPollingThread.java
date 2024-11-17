package net.rmnad.services;

import net.rmnad.Log;
import net.rmnad.WhitelistSyncCore;
import net.rmnad.logging.LogMessages;

public class WhitelistPollingThread extends Thread {

    private final BaseService service;
    private final boolean errorOnSetup;

    public WhitelistPollingThread(
            BaseService service,
            boolean errorOnSetup) {

        this.setName("WhitelistPollingThread");
        this.setDaemon(true);
        this.service = service;
        this.errorOnSetup = errorOnSetup;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
            Log.debug("WhitelistPollingThread interrupted. Exiting.");
            return;
        }

        Log.info(LogMessages.SYNC_THREAD_STARTING);

        if (this.errorOnSetup) {
            Log.error(LogMessages.ERROR_INITIALIZING_WHITELIST_SYNC_DATABASE);
            return;
        }

        while (true) {
            try {
                service.pullDatabaseWhitelistToLocal();

                if (WhitelistSyncCore.CONFIG.syncOpList) {
                    service.pullDatabaseOpsToLocal();
                }
            } catch (Exception e) {
                Log.error(LogMessages.ERROR_WHITELIST_SYNC_THREAD, e);
            }

            try {
                Thread.sleep(WhitelistSyncCore.CONFIG.syncTimer * 1000L);
            } catch (InterruptedException ignored) {
                Log.debug("WhitelistPollingThread interrupted. Exiting.");
                return;
            }
        }
    }
}
