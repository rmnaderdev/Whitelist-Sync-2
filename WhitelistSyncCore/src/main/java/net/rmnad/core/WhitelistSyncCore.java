package net.rmnad.core;

import net.rmnad.core.callbacks.IServerControl;
import net.rmnad.core.config.WhitelistSyncConfig;
import net.rmnad.core.logging.LogMessages;
import net.rmnad.core.services.*;

public class WhitelistSyncCore {

    public static BaseService whitelistService;
    public static IServerControl currentServerControl;
    public static WhitelistPollingThread pollingThread;
    public static WhitelistSocketThread socketThread;

    public static final WhitelistSyncConfig CONFIG = new WhitelistSyncConfig();

    public static boolean errorOnSetup = false;

    public static void LoadConfig() {
        CONFIG.load();
    }

    public static void SetupWhitelistSync(IServerControl serverControl) {
        currentServerControl = serverControl;
        Log.verbose = CONFIG.verboseLogging;

        LogMessages.ShowModStartupHeaderMessage();

        currentServerControl.versionCheck();

        switch (CONFIG.databaseMode) {
            case SQLITE:
                whitelistService = new SqLiteService(currentServerControl);
                break;
            case MYSQL:
                whitelistService = new MySqlService(currentServerControl);
                break;
            case WEB:
                whitelistService = new WebService(currentServerControl);
                break;
            default:
                Log.error(LogMessages.ERROR_WHITELIST_MODE);
                errorOnSetup = true;
                break;
        }

        if (!errorOnSetup) {
            if (!whitelistService.initializeDatabase()) {
                errorOnSetup = true;
            } else {
                currentServerControl.checkWhitelistEnabled();
            }
        }

        StartSyncThread();

        LogMessages.ShowModStartupFooterMessage();
    }

    public static void StartSyncThread() {
        if (whitelistService instanceof WebService) {
            socketThread = new WhitelistSocketThread((WebService) whitelistService, errorOnSetup, currentServerControl);

            socketThread.start();
        } else {
            pollingThread = new WhitelistPollingThread(
                    whitelistService,
                    errorOnSetup
            );
            pollingThread.start();
        }
    }

    public static void RestartSyncThread() {
        if (errorOnSetup)
            return;

        ShutdownWhitelistSync();
        StartSyncThread();
    }

    public static void ShutdownWhitelistSync() {
        if(pollingThread != null) {
            pollingThread.interrupt();

            try {
                pollingThread.join();
            } catch (InterruptedException ignored) {}

        }

        if(socketThread != null) {
            socketThread.interrupt();

            try {
                socketThread.join();
            } catch (InterruptedException ignored) {}
        }
    }

}
