package com.potatosaucevfx.whitelistsync2;

import com.potatosaucevfx.whitelistsync2.commands.CommandEasterEgg;
import com.potatosaucevfx.whitelistsync2.commands.CommandOp;
import com.potatosaucevfx.whitelistsync2.commands.CommandWhitelist;
import com.potatosaucevfx.whitelistsync2.config.ConfigHandler;
import com.potatosaucevfx.whitelistsync2.exceptions.ConfigErrorException;
import com.potatosaucevfx.whitelistsync2.services.BaseService;
import com.potatosaucevfx.whitelistsync2.services.MySqlService;
import com.potatosaucevfx.whitelistsync2.services.SqLiteService;
import java.io.File;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Main mod class
 * @author Richard Nader, Jr. <nader1rm@cmich.edu>
 */
@Mod(modid = WhitelistSync2.MODID, version = WhitelistSync2.VERSION, acceptableRemoteVersions = "*", serverSideOnly = true)
public class WhitelistSync2 {
    
    public static final String MODID = "whitelistsync2";
    public static final String VERSION = "2.0-1.12.2"; // Change gradle build config too!
    public static String SERVER_FILEPATH;
    public static Configuration config;
    
    // Database Service
    public static BaseService whitelistService;

    public static final Logger logger = LogManager.getLogger(MODID);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        logger.info("\n\nHello Minecraft!\n");
        UpdateConfig(e);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        logger.info("Hello again Minecraft");
        logger.info("Setting up databases...");
        
        
        if (ConfigHandler.WHITELIST_MODE.equalsIgnoreCase(ConfigHandler.MODE_SQLITE)) {
            whitelistService = new SqLiteService();
        } else if (ConfigHandler.WHITELIST_MODE.equalsIgnoreCase(ConfigHandler.MODE_MYSQL)) {
            whitelistService = new MySqlService();
        } else {
            throw new ConfigErrorException("Please check what WHITELIST_MODE is set in the config"
                    + "and make sure it is set to a supported mode.");
        }
        
        
        logger.info("Database setup!");
    }

    @SideOnly(Side.SERVER)
    @Mod.EventHandler
    public void serverLoad(FMLServerStartingEvent event) {
        //SERVER_FILEPATH = event.getServer().getDataDirectory().getAbsolutePath();

        File serverDir = event.getServer().getDataDirectory();

        SERVER_FILEPATH = serverDir.getPath();
        logger.info("----------------------------------------------");
        logger.info("---------------WHITELIST SYNC 2---------------");
        logger.info("----------------------------------------------");
        
        // Check if whitelisting is enabled.
        if (!event.getServer().getPlayerList().isWhiteListEnabled()) {
            logger.info("Oh no! I see whitelisting isn't enabled in the server properties. "
                    + "I assume this is not intentional, I'll enable it for you!");
            event.getServer().getPlayerList().setWhiteListEnabled(true);
        }
        
        logger.info("Loading Commands");
        event.registerServerCommand(new CommandWhitelist(whitelistService));
        event.registerServerCommand(new CommandOp(whitelistService));
       

        logger.info("Starting Sync Thread...");
        Utilities.StartSyncThread(event.getServer(), whitelistService);

        

        logger.info("----------------------------------------------");
        logger.info("----------------------------------------------");
        logger.info("----------------------------------------------");
    }
    
    // Method for loading config.
    public void UpdateConfig(FMLPreInitializationEvent e) {
        File directory = e.getModConfigurationDirectory();
        config = new Configuration(new File(directory.getPath(), "whitelistsync.cfg"));
        ConfigHandler.readConfig();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent e) {
        if (config.hasChanged()) {
            config.save();
        }
    }
}
