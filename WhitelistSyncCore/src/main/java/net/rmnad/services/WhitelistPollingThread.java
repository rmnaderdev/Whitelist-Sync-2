package net.rmnad.services;

import net.rmnad.Log;
import net.rmnad.callbacks.IOnUserOpAdd;
import net.rmnad.callbacks.IOnUserOpRemove;
import net.rmnad.callbacks.IOnUserWhitelistAdd;
import net.rmnad.callbacks.IOnUserWhitelistRemove;
import net.rmnad.json.OppedPlayersFileReader;
import net.rmnad.json.WhitelistedPlayersFileReader;
import net.rmnad.logging.LogMessages;

public class WhitelistPollingThread extends Thread {

    private final BaseService service;
    private final boolean syncOpLists;
    private final boolean errorOnSetup;

    private final String serverFilePath;
    private final int syncTimer;

    private final IOnUserWhitelistAdd onUserAdd;
    private final IOnUserWhitelistRemove onUserRemove;
    private final IOnUserOpAdd onUserOpAdd;
    private final IOnUserOpRemove onUserOpRemove;

    public WhitelistPollingThread(
            BaseService service,
            boolean syncOpLists,
            boolean errorOnSetup,
            String serverFilePath,
            int syncTimer,
            IOnUserWhitelistAdd onUserAdd,
            IOnUserWhitelistRemove onUserRemove,
            IOnUserOpAdd onUserOpAdd,
            IOnUserOpRemove onUserOpRemove) {

        this.setName("WhitelistPollingThread");
        this.setDaemon(true);
        this.service = service;
        this.syncOpLists = syncOpLists;
        this.errorOnSetup = errorOnSetup;

        this.serverFilePath = serverFilePath;
        this.syncTimer = syncTimer;

        this.onUserAdd = onUserAdd;
        this.onUserRemove = onUserRemove;
        this.onUserOpAdd = onUserOpAdd;
        this.onUserOpRemove = onUserOpRemove;
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
                service.pullDatabaseWhitelistToLocal(
                        WhitelistedPlayersFileReader.getWhitelistedPlayers(serverFilePath),
                        this.onUserAdd,
                        this.onUserRemove
                );

                if (syncOpLists) {
                    service.pullDatabaseOpsToLocal(
                            OppedPlayersFileReader.getOppedPlayers(serverFilePath),
                            this.onUserOpAdd,
                            this.onUserOpRemove
                    );
                }
            } catch (Exception e) {
                Log.error(LogMessages.ERROR_WHITELIST_SYNC_THREAD, e);
            }

            try {
                Thread.sleep(this.syncTimer * 1000L);
            } catch (InterruptedException ignored) {
                Log.debug("WhitelistPollingThread interrupted. Exiting.");
                return;
            }
        }
    }
}
