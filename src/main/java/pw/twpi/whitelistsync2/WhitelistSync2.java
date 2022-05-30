package pw.twpi.whitelistsync2;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.rmnad.minecraft.forge.whitelistsynclib.services.BaseService;
import net.rmnad.minecraft.forge.whitelistsynclib.services.MySqlService;
import net.rmnad.minecraft.forge.whitelistsynclib.services.SqLiteService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pw.twpi.whitelistsync2.commands.CommandOp;
import pw.twpi.whitelistsync2.commands.CommandWhitelist;
import pw.twpi.whitelistsync2.config.Config;
import pw.twpi.whitelistsync2.services.WhitelistSyncThread;

import java.io.File;

/**
 * Main mod class
 *
 * @author Richard Nader, Jr. <rmnader@svsu.edu>
 */
@Mod(modid = WhitelistSync2.MODID, acceptableRemoteVersions = "*", serverSideOnly = true)
public class WhitelistSync2 {

    public static final String MODID = "whitelistsync2";
    public static String SERVER_FILEPATH;
    public static Configuration config;

    // Database Service
    public static BaseService whitelistService;

    public static final Logger LOGGER = LogManager.getLogger(MODID);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        UpdateConfig(e);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info("Hello from Whitelist Sync 2!");
    }

    @SideOnly(Side.SERVER)
    @Mod.EventHandler
    public void serverLoad(FMLServerStartingEvent event) {
        boolean errorOnSetup = false;

        // Server filepath
        SERVER_FILEPATH = event.getServer().getDataDirectory().getPath();

        LOGGER.info("----------------------------------------------");
        LOGGER.info("---------------WHITELIST SYNC 2---------------");
        LOGGER.info("----------------------------------------------");

        if (Config.WHITELIST_MODE.equalsIgnoreCase(Config.MODE_SQLITE)) {
            whitelistService = new SqLiteService(Config.sqliteDatabasePath, Config.SYNC_OP_LIST);
        } else if (Config.WHITELIST_MODE.equalsIgnoreCase(Config.MODE_MYSQL)) {
            whitelistService = new MySqlService(
                    Config.mySQL_DBname,
                    Config.mySQL_IP,
                    Config.mySQL_PORT,
                    Config.mySQL_Username,
                    Config.mySQL_Password,
                    Config.SYNC_OP_LIST
            );
        } else {
            LOGGER.error("Please check what WHITELIST_MODE is set in the config and make sure it is set to a supported mode.");
            errorOnSetup = true;
        }

        event.registerServerCommand(new CommandWhitelist(whitelistService));
        if(Config.SYNC_OP_LIST) {
            LOGGER.info("Opped Player Sync is enabled");
            event.registerServerCommand(new CommandOp(whitelistService));
        } else {
            LOGGER.info("Opped Player Sync is disabled");
        }

        StartWhitelistSyncThread(event.getServer(), whitelistService, errorOnSetup);

        LOGGER.info("----------------------------------------------");
        LOGGER.info("----------------------------------------------");
        LOGGER.info("----------------------------------------------");
    }

    // Method for loading config.
    public void UpdateConfig(FMLPreInitializationEvent e) {
        File directory = e.getModConfigurationDirectory();
        config = new Configuration(new File(directory.getPath(), "whitelistsync.cfg"));
        Config.readConfig();
    }

    public static void StartWhitelistSyncThread(MinecraftServer server, BaseService service, boolean errorOnSetup) {
        WhitelistSyncThread sync = new WhitelistSyncThread(server, service, Config.SYNC_OP_LIST, errorOnSetup);
        sync.start();
        LOGGER.info("Sync Thread Started!");
    }
}
