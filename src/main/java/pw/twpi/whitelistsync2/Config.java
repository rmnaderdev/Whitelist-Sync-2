package pw.twpi.whitelistsync2;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

import java.nio.file.Path;

@Mod.EventBusSubscriber
public class Config {

    public enum DatabaseMode {
        MYSQL,
        SQLITE
    }

    public enum SyncMode {
        INTERVAL,
        LISTENER
    }

    public static final String CATEGORY_GENERAL = "general";
    public static final String CATEGORY_MYSQL = "mySQL";
    public static final String CATEGORY_SQLITE = "sqlite";

    private static final ForgeConfigSpec.Builder COMMON_BUILDER
            = new ForgeConfigSpec.Builder();

    // Only one common config
    public static ForgeConfigSpec COMMON_CONFIG;

    // General Settings
    public static ForgeConfigSpec.EnumValue<DatabaseMode> DATABASE_MODE;
    public static ForgeConfigSpec.BooleanValue SYNC_OP_LIST;
    public static ForgeConfigSpec.BooleanValue ENABLE_DEBUG_LOGGING;

    // MYSQL Settings
    public static ForgeConfigSpec.IntValue MYSQL_SYNC_TIMER;
    public static ForgeConfigSpec.ConfigValue<String> MYSQL_DB_NAME;
    public static ForgeConfigSpec.ConfigValue<String> MYSQL_IP;
    public static ForgeConfigSpec.IntValue MYSQL_PORT;
    public static ForgeConfigSpec.ConfigValue<String> MYSQL_USERNAME;
    public static ForgeConfigSpec.ConfigValue<String> MYSQL_PASSWORD;

    // SQLITE Settings
    public static ForgeConfigSpec.ConfigValue<String> SQLITE_DATABASE_PATH;
    public static ForgeConfigSpec.EnumValue<SyncMode> SQLITE_SYNC_MODE;
    public static ForgeConfigSpec.IntValue SQLITE_SERVER_SYNC_TIMER;
    public static ForgeConfigSpec.IntValue SQLITE_SERVER_LISTENER_TIMER;


    static {
        // General Settings
        COMMON_BUILDER.comment("General configuration").push(CATEGORY_GENERAL);
        setupGeneralConfig();
        COMMON_BUILDER.pop();


        // MYSQL Config
        COMMON_BUILDER.comment(
                "MySQL configuration (To enable " +
                        "mySQL, refer to the mode setting in the general configuration)."
        ).push(CATEGORY_MYSQL);
        setupMySQLConfig();
        COMMON_BUILDER.pop();


        // SQLITE Config
        COMMON_BUILDER.comment(
                "Sqlite configuration (To enable Sqlite, " +
                        "refer to the mode setting in the general configuration)."
        ).push(CATEGORY_SQLITE);
        setupSqliteConfig();
        COMMON_BUILDER.pop();

        COMMON_CONFIG = COMMON_BUILDER.build();
    }

    private static void setupGeneralConfig() {
        DATABASE_MODE = COMMON_BUILDER.comment("Mode for the database.")
                .defineEnum("databaseMode", DatabaseMode.SQLITE);
        SYNC_OP_LIST = COMMON_BUILDER.comment("Option on wheather to sync the server's op list to the database.")
                .define("syncOpList", false);
        ENABLE_DEBUG_LOGGING = COMMON_BUILDER.comment("Option on wheather to output whitelist/op changes triggered by other servers. " +
                "This will show when a new user is opped or whitelisted on anther connected sever.")
                .define("enableDebugLogging", false);
    }

    private static void setupMySQLConfig() {
        MYSQL_SYNC_TIMER = COMMON_BUILDER.comment("Time Interval in seconds for when the server " +
                "polls the whitelist changes from the mySQL database. " +
                "(Warning! Time lower than 5 sec may effect performace.)")
                .defineInRange("mysqlSyncTimer", 60, 1, Integer.MAX_VALUE);
        MYSQL_DB_NAME = COMMON_BUILDER.comment("Name for your mySQL database (No spaces!).")
                .define("mysqlDbName", "WhitelistSync");
        MYSQL_IP = COMMON_BUILDER.comment("IP for your mySQL server.")
                .define("mysqlIp", "localhost");
        MYSQL_PORT = COMMON_BUILDER.comment("Port for your mySQL server.")
                .defineInRange("mysqlPort", 3306, 1, 65535);
        MYSQL_USERNAME = COMMON_BUILDER.comment("Username for your mySQL server.")
                .define("mysqlUsername", "root");
        MYSQL_PASSWORD = COMMON_BUILDER.comment("Password for your mySQL server.")
                .define("mysqlPassword", "password");
    }

    private static void setupSqliteConfig() {
        SQLITE_SYNC_MODE = COMMON_BUILDER.comment("Mode for how the database updates.")
                .defineEnum("sqliteSyncMode", SyncMode.INTERVAL);
        SQLITE_DATABASE_PATH = COMMON_BUILDER.comment("Insert System Path for your SQLite database file. " +
                "This should be the same for all your servers you want to sync!")
                .define("sqliteDatabasePath", "./whitelistSync.db");
        SQLITE_SERVER_SYNC_TIMER = COMMON_BUILDER.comment("Time Interval in seconds for when the server " +
                "polls the whitelist changes from the database. (Only used in INTERVAL Sqlite Mode!)")
                .defineInRange("sqliteServerSyncTimer", 60, 1, Integer.MAX_VALUE);
        SQLITE_SERVER_LISTENER_TIMER = COMMON_BUILDER.comment("Time Interval in seconds for when the server checks " +
                "for database changes (Only used in Database Update Sqlite Mode!)")
                .defineInRange("sqliteServerListenerTimer", 10, 1, Integer.MAX_VALUE);

    }


    public static void loadConfig(ForgeConfigSpec spec, Path path) {

        final CommentedFileConfig configData = CommentedFileConfig.builder(path)
                .sync()
                .autosave()
                .writingMode(WritingMode.REPLACE)
                .build();

        configData.load();
        spec.setConfig(configData);
    }

    @SubscribeEvent
    public static void onLoad(final ModConfig.Loading configEvent) {

    }

    @SubscribeEvent
    public static void onReload(final ModConfig.ConfigReloading configEvent) {
    }

}
