package pw.twpi.whitelistsync2;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pw.twpi.whitelistsync2.commands.CommandOp;
import pw.twpi.whitelistsync2.commands.CommandWhitelist;
import pw.twpi.whitelistsync2.config.ConfigHandler;
import pw.twpi.whitelistsync2.services.BaseService;
import pw.twpi.whitelistsync2.services.MySqlService;
import pw.twpi.whitelistsync2.services.SqLiteService;
import pw.twpi.whitelistsync2.services.SyncThread;

import java.io.File;

/**
 * Main mod class
 *
 * @author Richard Nader, Jr. <rmnader@svsu.edu>
 */
@Mod(modid = WhitelistSync2.MODID, version = WhitelistSync2.VERSION, acceptableRemoteVersions = "*", serverSideOnly = true)
public class WhitelistSync2 {

    public static final String MODID = "whitelistsync2";
    public static final String VERSION = "1.12.2-2.2.1"; // Change gradle build config too!
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
        boolean setupSuccessful = true;

        // Server filepath
        SERVER_FILEPATH = event.getServer().getDataDirectory().getPath();

        LOGGER.info("----------------------------------------------");
        LOGGER.info("---------------WHITELIST SYNC 2---------------");
        LOGGER.info("----------------------------------------------");

        if (ConfigHandler.WHITELIST_MODE.equalsIgnoreCase(ConfigHandler.MODE_SQLITE)) {
            whitelistService = new SqLiteService();
        } else if (ConfigHandler.WHITELIST_MODE.equalsIgnoreCase(ConfigHandler.MODE_MYSQL)) {
            whitelistService = new MySqlService();
        } else {
            LOGGER.error("Please check what WHITELIST_MODE is set in the config and make sure it is set to a supported mode.");
            setupSuccessful = false;
        }

        if(!whitelistService.initializeDatabase() || !setupSuccessful) {
            LOGGER.error("Error initializing whitelist sync database. Disabling mod functionality. Please correct errors and restart.");
        } else {
            // Database is setup!

            // Check if whitelisting is enabled.
            if (!event.getServer().getPlayerList().isWhiteListEnabled()) {
                LOGGER.info("Oh no! I see whitelisting isn't enabled in the server properties. "
                        + "I assume this is not intentional, I'll enable it for you!");
                event.getServer().getPlayerList().setWhiteListEnabled(true);
            }

            StartSyncThread(event.getServer(), whitelistService);

            event.registerServerCommand(new CommandWhitelist(whitelistService));

            if(ConfigHandler.SYNC_OP_LIST) {
                LOGGER.info("Opped Player Sync is enabled");
                event.registerServerCommand(new CommandOp(whitelistService));
            } else {
                LOGGER.info("Opped Player Sync is disabled");
            }
        }

        LOGGER.info("----------------------------------------------");
        LOGGER.info("----------------------------------------------");
        LOGGER.info("----------------------------------------------");
    }

    // Method for loading config.
    public void UpdateConfig(FMLPreInitializationEvent e) {
        File directory = e.getModConfigurationDirectory();
        config = new Configuration(new File(directory.getPath(), "whitelistsync.cfg"));
        ConfigHandler.readConfig();
    }

    public void StartSyncThread(MinecraftServer server, BaseService service) {
        Thread sync = new Thread(new SyncThread(server, service));
        sync.start();
        LOGGER.info("Sync Thread Started!");
    }
}
