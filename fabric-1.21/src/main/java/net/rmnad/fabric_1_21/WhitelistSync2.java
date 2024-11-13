package net.rmnad.fabric_1_21;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.rmnad.Log;
import net.rmnad.callbacks.IServerControl;
import net.rmnad.logging.LogMessages;
import net.rmnad.services.*;

public class WhitelistSync2 implements ModInitializer {
	public static final String MODID = "whitelistsync2";

	// Database Service
	public static BaseService whitelistService;
	public static IServerControl serverControl;
	public static boolean errorOnSetup = false;

	public static WhitelistPollingThread pollingThread;
	public static WhitelistSocketThread socketThread;

	@Override
	public void onInitialize() {
		// Register config
		//Config.register(ModLoadingContext.get());

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
		//Log.verbose = Config.COMMON.VERBOSE_LOGGING.get();

		serverControl = new ServerControl(server);

		LogMessages.ShowModStartupHeaderMessage();

//		try {
//			// If this fails, let the server continue to start up.
//			VersionChecker versionChecker = new VersionChecker(Config.COMMON.WEB_API_HOST.get());
//			var modInfo = ModList.get().getModContainerById("whitelistsync2");
//			var minecraftInfo = ModList.get().getModContainerById("minecraft");
//
//			if (modInfo.isPresent() && minecraftInfo.isPresent()) {
//				versionChecker.checkVersion(modInfo.get().getModInfo().getVersion(), minecraftInfo.get().getModInfo().getVersion());
//			}
//		} catch (Exception ignore) {}
//
//		switch (Config.COMMON.DATABASE_MODE.get()) {
//			case SQLITE:
//				whitelistService = new SqLiteService(
//						Config.COMMON.SQLITE_DATABASE_PATH.get(),
//						server.getServerDirectory().toFile().getAbsolutePath(),
//						Config.COMMON.SYNC_OP_LIST.get(),
//						serverControl
//				);
//				break;
//			case MYSQL:
//				whitelistService = new MySqlService(
//						Config.COMMON.MYSQL_DB_NAME.get(),
//						Config.COMMON.MYSQL_IP.get(),
//						Config.COMMON.MYSQL_PORT.get(),
//						Config.COMMON.MYSQL_USERNAME.get(),
//						Config.COMMON.MYSQL_PASSWORD.get(),
//						server.getServerDirectory().toFile().getAbsolutePath(),
//						Config.COMMON.SYNC_OP_LIST.get(),
//						serverControl
//				);
//				break;
//			case WEB:
//				whitelistService = new WebService(
//						server.getServerDirectory().toFile().getAbsolutePath(),
//						Config.COMMON.WEB_API_HOST.get(),
//						Config.COMMON.WEB_API_KEY.get(),
//						Config.COMMON.SYNC_OP_LIST.get(),
//						Config.COMMON.WEB_SYNC_BANNED_PLAYERS.get(),
//						Config.COMMON.WEB_SYNC_BANNED_IPS.get(),
//						serverControl
//				);
//				break;
//			default:
//				Log.error(LogMessages.ERROR_WHITELIST_MODE);
//				errorOnSetup = true;
//				break;
//		}
//
//		if (!errorOnSetup) {
//			if (whitelistService.initializeDatabase()) {
//				// Database is setup!
//				// Check if whitelisting is enabled.
//				if (!server.getPlayerManager().isWhitelistEnabled()) {
//					Log.info(LogMessages.WARN_WHITELIST_NOT_ENABLED);
//					server.getPlayerManager().setWhitelistEnabled(true);
//				}
//			} else {
//				errorOnSetup = true;
//			}
//		}

		StartSyncThread();

		LogMessages.ShowModStartupFooterMessage();
	}

	public static void StartSyncThread() {
		if (whitelistService instanceof WebService) {
			socketThread = new WhitelistSocketThread((WebService) whitelistService, errorOnSetup, serverControl);

			socketThread.start();
		} else {
//			pollingThread = new WhitelistPollingThread(
//					whitelistService,
//					Config.COMMON.SYNC_OP_LIST.get(),
//					errorOnSetup,
//					Config.COMMON.SYNC_TIMER.get()
//			);
//			pollingThread.start();
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