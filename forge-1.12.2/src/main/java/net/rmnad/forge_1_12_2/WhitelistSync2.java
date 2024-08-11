package net.rmnad.forge_1_12_2;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.rmnad.Log;
import net.rmnad.forge_1_12_2.commands.CommandOp;
import net.rmnad.forge_1_12_2.commands.CommandWhitelist;
import net.rmnad.services.BaseService;
import net.rmnad.services.MySqlService;
import net.rmnad.services.SqLiteService;
import net.rmnad.forge_1_12_2.services.*;

import java.io.File;

@Mod(modid = WhitelistSync2.MODID, acceptableRemoteVersions = "*", serverSideOnly = true)
public class WhitelistSync2
{
    public static final String MODID = "whitelistsync2";
    public static String SERVER_FILEPATH;
    public static Configuration config;

    // Database Service
    public static BaseService whitelistService;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        UpdateConfig(e);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        Log.setLogger(new ForgeLogger());
        Log.info("Hello from Whitelist Sync 2!");
    }

    @SideOnly(Side.SERVER)
    @Mod.EventHandler
    public void serverLoad(FMLServerStartingEvent event) {
        WhitelistSync2.SetupWhitelistSync(event);
    }

    public static void SetupWhitelistSync(FMLServerStartingEvent event) {
        Log.verbose = Config.VERBOSE_LOGGING;

        boolean errorOnSetup = false;

        // Server filepath
        SERVER_FILEPATH = event.getServer().getDataDirectory().getPath();

        Log.info("----------------------------------------------");
        Log.info("---------------WHITELIST SYNC 2---------------");
        Log.info("----------------------------------------------");

        if (Config.WHITELIST_MODE.equalsIgnoreCase(Config.MODE_MYSQL)) {
            whitelistService = new MySqlService(
                Config.mySQL_DBname,
                Config.mySQL_IP,
                Config.mySQL_PORT,
                Config.mySQL_Username,
                Config.mySQL_Password,
                Config.SYNC_OP_LIST
            );
        } else if (Config.WHITELIST_MODE.equalsIgnoreCase(Config.MODE_SQLITE)) {
            whitelistService = new SqLiteService(Config.sqliteDatabasePath, Config.SYNC_OP_LIST);
        } else {
            Log.error("Please check what WHITELIST_MODE is set in the config and make sure it is set to a supported mode.");
            errorOnSetup = true;
        }

        event.registerServerCommand(new CommandWhitelist(whitelistService));
        if(Config.SYNC_OP_LIST) {
            Log.info("Opped Player Sync is enabled");
            event.registerServerCommand(new CommandOp(whitelistService));
        } else {
            Log.info("Opped Player Sync is disabled");
        }

        if (!errorOnSetup) {
            if (whitelistService.initializeDatabase()) {
                // Database is setup!
                // Check if whitelisting is enabled.
                if (!event.getServer().getPlayerList().isWhiteListEnabled()) {
                    Log.info("Oh no! I see whitelisting isn't enabled in the server properties. "
                            + "I assume this is not intentional, I'll enable it for you!");
                    event.getServer().getPlayerList().setWhiteListEnabled(true);
                }
            } else {
                errorOnSetup = true;
            }
        }

        StartWhitelistSyncThread(event.getServer(), whitelistService, errorOnSetup);

        Log.info("----------------------------------------------");
        Log.info("----------------------------------------------");
        Log.info("----------------------------------------------");
    }

    // Method for loading config.
    public void UpdateConfig(FMLPreInitializationEvent e) {
        File directory = e.getModConfigurationDirectory();
        config = new Configuration(new File(directory.getPath(), "whitelistsync.cfg"));
        Config.readConfig();
    }

    public static void StartWhitelistSyncThread(MinecraftServer server, BaseService service, boolean errorOnSetup) {
        WhitelistSyncThread syncThread = new WhitelistSyncThread(server, service, Config.SYNC_OP_LIST, errorOnSetup);
        syncThread.start();
        Log.info("WhitelistSync Thread Started!");
    }
}
