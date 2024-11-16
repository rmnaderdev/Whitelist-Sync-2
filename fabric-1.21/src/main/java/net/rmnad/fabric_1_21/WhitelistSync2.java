package net.rmnad.fabric_1_21;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.rmnad.Log;
import net.rmnad.callbacks.IServerControl;
import net.rmnad.logging.LogMessages;
import net.rmnad.services.*;
import net.rmnad.fabric_1_21.WhitelistSync2Config;

public class WhitelistSync2 implements ModInitializer {
	public static final String MODID = "whitelistsync2";

	public static final WhitelistSync2Config CONFIG = WhitelistSync2Config.createAndLoad();

	// Database Service
	public static BaseService whitelistService;
	public static IServerControl serverControl;
	public static boolean errorOnSetup = false;

	public static WhitelistPollingThread pollingThread;
	public static WhitelistSocketThread socketThread;

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> {
			new WhitelistSyncCommands(dispatcher);
		}));

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			SetupWhitelistSync(server);
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			ShutdownWhitelistSync();
		});

		// Register commands
		Log.setLogger(new FabricLogger());
		Log.info(LogMessages.HELLO_MESSAGE);
	}

	public static void SetupWhitelistSync(MinecraftServer server) {
		Log.verbose = CONFIG.verboseLogging();

		serverControl = new ServerControl(server);

		LogMessages.ShowModStartupHeaderMessage();

//		try {
//			// If this fails, let the server continue to start up.
//			VersionChecker versionChecker = new VersionChecker(CONFIG.webApiHost());
//			var modInfo = ModList.get().getModContainerById("whitelistsync2");
//			var minecraftInfo = ModList.get().getModContainerById("minecraft");
//
//			if (modInfo.isPresent() && minecraftInfo.isPresent()) {
//				versionChecker.checkVersion(modInfo.get().getModInfo().getVersion(), minecraftInfo.get().getModInfo().getVersion());
//			}
//		} catch (Exception ignore) {}

		switch (CONFIG.databaseMode()) {
			case SQLITE:
				whitelistService = new SqLiteService(
						CONFIG.sqliteDatabasePath(),
						server.getRunDirectory().toFile().getAbsolutePath(),
						CONFIG.syncOpList(),
						serverControl
				);
				break;
			case MYSQL:
				whitelistService = new MySqlService(
						CONFIG.mysqlDbName(),
						CONFIG.mysqlIp(),
						CONFIG.mysqlPort(),
						CONFIG.mysqlUsername(),
						CONFIG.mysqlPassword(),
						server.getRunDirectory().toFile().getAbsolutePath(),
						CONFIG.syncOpList(),
						serverControl
				);
				break;
			case WEB:
				whitelistService = new WebService(
						server.getRunDirectory().toFile().getAbsolutePath(),
						CONFIG.webApiHost(),
						CONFIG.webApiKey(),
						CONFIG.syncOpList(),
						CONFIG.webSyncBannedPlayers(),
						CONFIG.webSyncBannedIps(),
						serverControl
				);
				break;
			default:
				Log.error(LogMessages.ERROR_WHITELIST_MODE);
				errorOnSetup = true;
				break;
		}

		if (!errorOnSetup) {
			if (whitelistService.initializeDatabase()) {
				// Database is setup!
				// Check if whitelisting is enabled.
				if (!server.getPlayerManager().isWhitelistEnabled()) {
					Log.info(LogMessages.WARN_WHITELIST_NOT_ENABLED);
					server.getPlayerManager().setWhitelistEnabled(true);
				}
			} else {
				errorOnSetup = true;
			}
		}

		StartSyncThread();

		LogMessages.ShowModStartupFooterMessage();
	}

	public static void StartSyncThread() {
		if (whitelistService instanceof WebService) {
			socketThread = new WhitelistSocketThread((WebService) whitelistService, errorOnSetup, serverControl);

			socketThread.start();
		} else {
			pollingThread = new WhitelistPollingThread(
					whitelistService,
					CONFIG.syncOpList(),
					errorOnSetup,
					CONFIG.syncTimer()
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