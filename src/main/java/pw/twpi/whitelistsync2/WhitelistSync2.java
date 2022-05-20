package pw.twpi.whitelistsync2;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.rmnad.minecraft.forge.whitelistsynclib.services.BaseService;
import net.rmnad.minecraft.forge.whitelistsynclib.services.MySqlService;
import net.rmnad.minecraft.forge.whitelistsynclib.services.SqLiteService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pw.twpi.whitelistsync2.services.*;

@Mod(WhitelistSync2.MODID)
public class WhitelistSync2
{
    public static final String MODID = "whitelistsync2";
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    public static String SERVER_FILEPATH;

    // Database Service
    public static BaseService whitelistService;

    public WhitelistSync2() {
        // Register config
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SERVER_CONFIG);
        MinecraftForge.EVENT_BUS.register(new EventListener());
        LOGGER.info("Hello from Whitelist Sync 2!");
    }

    public static void SetupWhitelistSync(MinecraftServer server) {
        boolean errorOnSetup = false;

        // Server filepath
        SERVER_FILEPATH = server.getServerDirectory().getPath();

        LOGGER.info("----------------------------------------------");
        LOGGER.info("---------------WHITELIST SYNC 2---------------");
        LOGGER.info("----------------------------------------------");

        switch (Config.DATABASE_MODE.get()) {
            case SQLITE:
                whitelistService = new SqLiteService(Config.SQLITE_DATABASE_PATH.get(), Config.SYNC_OP_LIST.get());
                break;
            case MYSQL:
                whitelistService = new MySqlService(
                        Config.MYSQL_DB_NAME.get(),
                        Config.MYSQL_IP.get(),
                        Config.MYSQL_PORT.get(),
                        Config.MYSQL_USERNAME.get(),
                        Config.MYSQL_PASSWORD.get(),
                        Config.SYNC_OP_LIST.get()
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
                LOGGER.error("Please check what WHITELIST_MODE is set in the config and make sure it is set to a supported mode.");
                errorOnSetup = true;
                break;
        }

        StartWhitelistSyncThread(server, whitelistService, errorOnSetup);

        LOGGER.info("----------------------------------------------");
        LOGGER.info("----------------------------------------------");
        LOGGER.info("----------------------------------------------");
    }

    public static void StartWhitelistSyncThread(MinecraftServer server, BaseService service, boolean errorOnSetup) {
        WhitelistSyncThread syncThread = new WhitelistSyncThread(server, service, Config.SYNC_OP_LIST.get(), errorOnSetup);
        syncThread.start();
        LOGGER.info("WhitelistSync Thread Started!");
    }
}
