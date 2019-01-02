package com.potatosaucevfx.whitelistsync2.config;

import com.potatosaucevfx.whitelistsync2.WhitelistSync2;
import net.minecraftforge.common.config.Configuration;
import static net.minecraftforge.common.config.Configuration.CATEGORY_GENERAL;


/**
 * Setup the mod configuration file
 * @author Richard Nader, Jr. <nader1rm@cmich.edu>
 */
public class ConfigHandler {
    // Custom Categories
    private final static String MYSQL_CATEGORY = "mySQL";
    private final static String SQLITE_CATEGORY = "sqlite";

    // Modes
    public static final String MODE_MYSQL = "MYSQL";
    public static final String MODE_SQLITE = "SQLITE";

    // General settings
    public static String WHITELIST_MODE = "SQLITE"; // SQLITE or MYSQL, use mode finals above.
    public static boolean SYNC_OP_LIST = false; // Sync ops list.
    public static boolean ENABLE_CONSOLE_OUTPUT = false; // Logs remote sync changes to console.

    // sqlite config
    public static String sqliteDatabasePath = "./whitelist.db";
    public static String sqliteMode = "INTERVAL";
    public static int sqliteServerSyncTimer = 60;

    public static int sqliteServerListenerTimer = 10;

    // mySQL config
    public static int mysqlServerSyncTimer = 60;
    public static String mySQL_DBname = "WhitelistSync";
    public static String mySQL_IP = "localhost";
    public static String mySQL_PORT = "3306";
    public static String mySQL_Username = "root";
    public static String mySQL_Password = "password";

    public static void readConfig() {
        Configuration cfg = WhitelistSync2.config;
        try {
            
            cfg.load();
            initGeneralConfig(cfg);
            
        } catch (Exception e1) {
            
            WhitelistSync2.logger.error("Problem loading config file!\n" + e1.getMessage());
            
        } finally {
            if (cfg.hasChanged()) {
                cfg.save();
            }
        }
    }

    private static void initGeneralConfig(Configuration cfg) {
        // General Settings
        cfg.addCustomCategoryComment(CATEGORY_GENERAL, "General configuration");
        WHITELIST_MODE = cfg.getString("Whitelist Sync Mode", CATEGORY_GENERAL, WHITELIST_MODE,
                "Mode for the database. Options are [MYSQL or SQLITE].");

        SYNC_OP_LIST = cfg.getBoolean("Sync OPs list to database", CATEGORY_GENERAL, SYNC_OP_LIST, "Option on wheather to sync the server's op list to the database.");

        ENABLE_CONSOLE_OUTPUT = cfg.getBoolean("Log Remote Updates", CATEGORY_GENERAL, ENABLE_CONSOLE_OUTPUT, 
                "Option on wheather to output whitelist/op changes triggered by other servers. This will show when a new user is opped or whitelisted on anther connected sever.");
        
        // Sqlite settings
        cfg.addCustomCategoryComment(SQLITE_CATEGORY, "Sqlite configuration (To enable "
                + "Sqlitee, refer to the mode setting in the general configuration).");
        sqliteDatabasePath = cfg.getString("Database Path", SQLITE_CATEGORY,
                sqliteDatabasePath, "Insert System Path for your Sqlite database file. "
                + "This will be the same for all your servers you want to sync!");

        sqliteMode = cfg.getString("Sqlite Sync Mode", SQLITE_CATEGORY, sqliteMode,
                "Mode for how the database updates."
                + " INTERVAL = Update Time Interval, LISTENER = Database Update Listener (Please let me know if there are problems).");

        sqliteServerSyncTimer = cfg.getInt("Sync Timer", SQLITE_CATEGORY,
                sqliteServerSyncTimer, 5, 1000, "Time Interval in seconds for when the server polls "
                + "the whitelist changes from the database. (Only used in Interval Sqlite Mode!)");

        sqliteServerListenerTimer = cfg.getInt("Server Listener Sync Time",
                SQLITE_CATEGORY, sqliteServerListenerTimer, 1, 1000,
                "Time Interval in seconds for when the server checks for"
                + " database changes (Only used in Database Update "
                + "Sqlite Mode!)");

        // MY_SQL settings
        cfg.addCustomCategoryComment(MYSQL_CATEGORY, "mySQL configuration (To enable "
                + "mySQL, refer to the mode setting in the general configuration).");

        mysqlServerSyncTimer = cfg.getInt("mySQL Sync Timer", MYSQL_CATEGORY,
                mysqlServerSyncTimer, 1, 1000, "Time Interval in seconds for when the server polls "
                + "the whitelist changes from the mySQL database. (Warning! Time lower than 5 sec may effect performace.)");

        mySQL_DBname = cfg.getString("mySQL Database Name", MYSQL_CATEGORY, mySQL_DBname, "Name for your mySQL database (No spaces!).");

        mySQL_IP = cfg.getString("mySQL IP", MYSQL_CATEGORY, mySQL_IP,
                "IP for your mySQL server (Example: localhost) Note: Do not add schema.");

        mySQL_PORT = cfg.getString("mySQL Port", MYSQL_CATEGORY, mySQL_PORT, "Port for your mySQL server.");

        mySQL_Username = cfg.getString("mySQL Username", MYSQL_CATEGORY, mySQL_Username, "Username for your mySQL server.");

        mySQL_Password = cfg.getString("mySQL Password", MYSQL_CATEGORY, mySQL_Password, "Password for your mySQL server.");
    }
}
