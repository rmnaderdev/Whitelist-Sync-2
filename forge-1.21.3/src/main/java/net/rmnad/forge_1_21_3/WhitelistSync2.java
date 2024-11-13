package net.rmnad.forge_1_21_3;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.rmnad.callbacks.IServerControl;
import net.rmnad.Log;
import net.rmnad.logging.LogMessages;
import net.rmnad.services.*;

@Mod(WhitelistSync2.MODID)
public class WhitelistSync2
{
    public static final String MODID = "whitelistsync2";

    // Database Service
    public static BaseService whitelistService;
    public static IServerControl serverControl;
    public static boolean errorOnSetup = false;

    public static WhitelistPollingThread pollingThread;
    public static WhitelistSocketThread socketThread;

    public WhitelistSync2(FMLJavaModLoadingContext context) {
        // Register config
        Config.register(context);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(CommandsListener.class);
        Log.setLogger(new ForgeLogger());
        Log.info(LogMessages.HELLO_MESSAGE);
    }

    // Command Registration
    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event){
        new WhitelistSyncCommands(event.getDispatcher());

        if(Config.COMMON.SYNC_OP_LIST.get()) {
            Log.info(LogMessages.OP_SYNC_ENABLED);
        } else {
            Log.info(LogMessages.OP_SYNC_DISABLED);
        }
    }

    // Server Started Event
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        WhitelistSync2.SetupWhitelistSync(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        WhitelistSync2.ShutdownWhitelistSync();
    }

    public static void SetupWhitelistSync(MinecraftServer server) {
        Log.verbose = Config.COMMON.VERBOSE_LOGGING.get();

        serverControl = new ServerControl(server);

        LogMessages.ShowModStartupHeaderMessage();

        try {
            // If this fails, let the server continue to start up.
            VersionChecker versionChecker = new VersionChecker(Config.COMMON.WEB_API_HOST.get());
            var modInfo = ModList.get().getModContainerById("whitelistsync2");
            var minecraftInfo = ModList.get().getModContainerById("minecraft");

            if (modInfo.isPresent() && minecraftInfo.isPresent()) {
                versionChecker.checkVersion(modInfo.get().getModInfo().getVersion(), minecraftInfo.get().getModInfo().getVersion());
            }
        } catch (Exception ignore) {}

        switch (Config.COMMON.DATABASE_MODE.get()) {
            case SQLITE:
                whitelistService = new SqLiteService(
                        Config.COMMON.SQLITE_DATABASE_PATH.get(),
                        server.getServerDirectory().toFile().getAbsolutePath(),
                        Config.COMMON.SYNC_OP_LIST.get(),
                        serverControl
                );
                break;
            case MYSQL:
                whitelistService = new MySqlService(
                        Config.COMMON.MYSQL_DB_NAME.get(),
                        Config.COMMON.MYSQL_IP.get(),
                        Config.COMMON.MYSQL_PORT.get(),
                        Config.COMMON.MYSQL_USERNAME.get(),
                        Config.COMMON.MYSQL_PASSWORD.get(),
                        server.getServerDirectory().toFile().getAbsolutePath(),
                        Config.COMMON.SYNC_OP_LIST.get(),
                        serverControl
                );
                break;
            case WEB:
                whitelistService = new WebService(
                        server.getServerDirectory().toFile().getAbsolutePath(),
                        Config.COMMON.WEB_API_HOST.get(),
                        Config.COMMON.WEB_API_KEY.get(),
                        Config.COMMON.SYNC_OP_LIST.get(),
                        Config.COMMON.WEB_SYNC_BANNED_PLAYERS.get(),
                        Config.COMMON.WEB_SYNC_BANNED_IPS.get(),
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
                if (!server.getPlayerList().isUsingWhitelist()) {
                    Log.info(LogMessages.WARN_WHITELIST_NOT_ENABLED);
                    server.getPlayerList().setUsingWhiteList(true);
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
                    Config.COMMON.SYNC_OP_LIST.get(),
                    errorOnSetup,
                    Config.COMMON.SYNC_TIMER.get()
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
