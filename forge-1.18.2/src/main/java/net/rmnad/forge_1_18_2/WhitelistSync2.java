package net.rmnad.forge_1_18_2;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.rmnad.Log;
import net.rmnad.callbacks.IServerControl;
import net.rmnad.logging.LogMessages;
import net.rmnad.services.*;

@Mod(WhitelistSync2.MODID)
public class WhitelistSync2
{
    public static final String MODID = "whitelistsync2";

    // Database Service
    public static BaseService whitelistService;

    public static WhitelistPollingThread pollingThread;
    public static WhitelistSocketThread socketThread;

    public WhitelistSync2() {
        // Register config
        Config.register(ModLoadingContext.get());
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

        IServerControl serverControl = new ServerControl(server);

        boolean errorOnSetup = false;

        LogMessages.ShowModStartupHeaderMessage();

        switch (Config.COMMON.DATABASE_MODE.get()) {
            case SQLITE:
                whitelistService = new SqLiteService(
                        Config.COMMON.SQLITE_DATABASE_PATH.get(),
                        server.getServerDirectory().getPath(),
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
                        server.getServerDirectory().getPath(),
                        Config.COMMON.SYNC_OP_LIST.get(),
                        serverControl
                );
                break;
            case WEB:
                whitelistService = new WebService(
                        server.getServerDirectory().getPath(),
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

        LogMessages.ShowModStartupFooterMessage();
    }

    public static void ShutdownWhitelistSync() {
        if(pollingThread != null) {
            pollingThread.interrupt();
        }

        if(socketThread != null) {
            socketThread.interrupt();
        }
    }
}
