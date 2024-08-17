package net.rmnad.forge_1_20_6;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.rmnad.forge_1_20_6.commands.op.OpCommands;
import net.rmnad.forge_1_20_6.commands.whitelist.WhitelistCommands;
import net.rmnad.forge_1_20_6.services.WhitelistSyncThread;
import net.rmnad.Log;
import net.rmnad.logging.LogMessages;
import net.rmnad.services.BaseService;
import net.rmnad.services.MySqlService;
import net.rmnad.services.SqLiteService;
import net.rmnad.services.WebService;

@Mod(WhitelistSync2.MODID)
public class WhitelistSync2
{
    public static final String MODID = "whitelistsync2";
    public static String SERVER_FILEPATH;

    // Database Service
    public static BaseService whitelistService;

    public WhitelistSync2() {
        // Register config
        Config.register(ModLoadingContext.get());
        MinecraftForge.EVENT_BUS.register(this);
        Log.setLogger(new ForgeLogger());
        Log.info(LogMessages.HELLO_MESSAGE);
    }

    // Command Registration
    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event){
        new WhitelistCommands(event.getDispatcher());

        if(Config.COMMON.SYNC_OP_LIST.get()) {
            Log.info(LogMessages.OP_SYNC_ENABLED);
            new OpCommands(event.getDispatcher());
        } else {
            Log.info(LogMessages.OP_SYNC_DISABLED);
        }
    }

    // Server Started Event
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        WhitelistSync2.SetupWhitelistSync(event.getServer());
    }

    public static void SetupWhitelistSync(MinecraftServer server) {
        Log.verbose = Config.COMMON.VERBOSE_LOGGING.get();

        boolean errorOnSetup = false;

        // Server filepath
        SERVER_FILEPATH = server.getServerDirectory().getPath();

        LogMessages.ShowModStartupHeaderMessage();

        switch (Config.COMMON.DATABASE_MODE.get()) {
            case SQLITE:
                whitelistService = new SqLiteService(Config.COMMON.SQLITE_DATABASE_PATH.get(), Config.COMMON.SYNC_OP_LIST.get());
                break;
            case MYSQL:
                whitelistService = new MySqlService(
                    Config.COMMON.MYSQL_DB_NAME.get(),
                    Config.COMMON.MYSQL_IP.get(),
                    Config.COMMON.MYSQL_PORT.get(),
                    Config.COMMON.MYSQL_USERNAME.get(),
                    Config.COMMON.MYSQL_PASSWORD.get(),
                    Config.COMMON.SYNC_OP_LIST.get()
                );
                break;
            case WEB:
                whitelistService = new WebService(Config.COMMON.WEB_API_KEY.get(), Config.COMMON.SYNC_OP_LIST.get());
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

        StartWhitelistSyncThread(server, whitelistService, errorOnSetup);

        LogMessages.ShowModStartupFooterMessage();
    }

    public static void StartWhitelistSyncThread(MinecraftServer server, BaseService service, boolean errorOnSetup) {
        WhitelistSyncThread syncThread = new WhitelistSyncThread(server, service, Config.COMMON.SYNC_OP_LIST.get(), errorOnSetup);
        syncThread.start();
    }
}
