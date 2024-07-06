package net.rmnad.forge_1_19_2;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.rmnad.whitelistsync2.Log;
import net.rmnad.whitelistsync2.services.BaseService;
import net.rmnad.whitelistsync2.services.MySqlService;
import net.rmnad.whitelistsync2.services.SqLiteService;
import net.rmnad.forge_1_19_2.commands.op.OpCommands;
import net.rmnad.forge_1_19_2.commands.whitelist.WhitelistCommands;
import net.rmnad.forge_1_19_2.services.*;

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
        Log.info("Hello from Whitelist Sync 2!");
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event){
        new WhitelistCommands(event.getDispatcher());

        if(Config.COMMON.SYNC_OP_LIST.get()) {
            Log.info("Opped Player Sync is enabled");
            new OpCommands(event.getDispatcher());
        } else {
            Log.info("Opped Player Sync is disabled");
        }
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        WhitelistSync2.SetupWhitelistSync(event.getServer());
    }

    public static void SetupWhitelistSync(MinecraftServer server) {
        boolean errorOnSetup = false;

        // Server filepath
        SERVER_FILEPATH = server.getServerDirectory().getPath();

        Log.info("----------------------------------------------");
        Log.info("---------------WHITELIST SYNC 2---------------");
        Log.info("----------------------------------------------");

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
//            case POSTGRESQL:
//                whitelistService = new PostgreSqlService(
//                        Config.POSTGRESQL_DB_NAME.get(),
//                        Config.POSTGRESQL_IP.get(),
//                        Config.POSTGRESQL_PORT.get(),
//                        Config.POSTGRESQL_USERNAME.get(),
//                        Config.POSTGRESQL_PASSWORD.get(),
//                        Config.SYNC_OP_LIST.get()
//                );
//                break;
            default:
                Log.error("Please check what WHITELIST_MODE is set in the config and make sure it is set to a supported mode.");
                errorOnSetup = true;
                break;
        }

        StartWhitelistSyncThread(server, whitelistService, errorOnSetup);

        Log.info("----------------------------------------------");
        Log.info("----------------------------------------------");
        Log.info("----------------------------------------------");
    }

    public static void StartWhitelistSyncThread(MinecraftServer server, BaseService service, boolean errorOnSetup) {
        WhitelistSyncThread syncThread = new WhitelistSyncThread(server, service, Config.COMMON.SYNC_OP_LIST.get(), errorOnSetup);
        syncThread.start();
        Log.info("WhitelistSync Thread Started!");
    }
}
