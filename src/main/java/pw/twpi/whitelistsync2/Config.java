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

    public static final String CATEGORY_GENERAL = "general";
    public static final String CATEGORY_MYSQL = "mySQL";
    public static final String CATEGORY_SQLITE = "sqlite";

    private static final ForgeConfigSpec.Builder SERVER_BUILDER
            = new ForgeConfigSpec.Builder();

    // Only one common config
    public static ForgeConfigSpec SERVER_CONFIG;

    // General Settings
    public static ForgeConfigSpec.EnumValue<DatabaseMode> DATABASE_MODE;
    public static ForgeConfigSpec.BooleanValue SYNC_OP_LIST;

    // MYSQL Settings
    public static ForgeConfigSpec.IntValue MYSQL_SYNC_TIMER;
    public static ForgeConfigSpec.ConfigValue<String> MYSQL_DB_NAME;
    public static ForgeConfigSpec.ConfigValue<String> MYSQL_IP;
    public static ForgeConfigSpec.IntValue MYSQL_PORT;
    public static ForgeConfigSpec.ConfigValue<String> MYSQL_USERNAME;
    public static ForgeConfigSpec.ConfigValue<String> MYSQL_PASSWORD;

    // SQLITE Settings
    public static ForgeConfigSpec.ConfigValue<String> SQLITE_DATABASE_PATH;
    public static ForgeConfigSpec.IntValue SQLITE_SERVER_SYNC_TIMER;


    static {
        // General Settings
        SERVER_BUILDER.comment("General configuration").push(CATEGORY_GENERAL);
        setupGeneralConfig();
        SERVER_BUILDER.pop();


        // MYSQL Config
        SERVER_BUILDER.comment(
                "MySQL configuration (To enable " +
                        "mySQL, refer to the mode setting in the general configuration)."
        ).push(CATEGORY_MYSQL);
        setupMySQLConfig();
        SERVER_BUILDER.pop();


        // SQLITE Config
        SERVER_BUILDER.comment(
                "Sqlite configuration (To enable Sqlite, " +
                        "refer to the mode setting in the general configuration)."
        ).push(CATEGORY_SQLITE);
        setupSqliteConfig();
        SERVER_BUILDER.pop();

        SERVER_CONFIG = SERVER_BUILDER.build();
    }

    private static void setupGeneralConfig() {
        DATABASE_MODE = SERVER_BUILDER.comment("Mode for the database.")
                .defineEnum("databaseMode", DatabaseMode.SQLITE);
        SYNC_OP_LIST = SERVER_BUILDER.comment("Option on whether to sync the server's op list to the database.")
                .define("syncOpList", false);
    }

    private static void setupMySQLConfig() {
        MYSQL_SYNC_TIMER = SERVER_BUILDER.comment("Time Interval in seconds for when the server " +
                "polls the whitelist changes from the mySQL database. " +
                "(Warning! Time lower than 5 sec may effect performace.)")
                .defineInRange("mysqlSyncTimer", 60, 1, Integer.MAX_VALUE);
        MYSQL_DB_NAME = SERVER_BUILDER.comment("Name for your mySQL database (No spaces!).")
                .define("mysqlDbName", "WhitelistSync");
        MYSQL_IP = SERVER_BUILDER.comment("IP for your mySQL server.")
                .define("mysqlIp", "localhost");
        MYSQL_PORT = SERVER_BUILDER.comment("Port for your mySQL server.")
                .defineInRange("mysqlPort", 3306, 1, 65535);
        MYSQL_USERNAME = SERVER_BUILDER.comment("Username for your mySQL server.")
                .define("mysqlUsername", "root");
        MYSQL_PASSWORD = SERVER_BUILDER.comment("Password for your mySQL server.")
                .define("mysqlPassword", "password");
    }

    private static void setupSqliteConfig() {
        SQLITE_DATABASE_PATH = SERVER_BUILDER.comment("Insert System Path for your SQLite database file. " +
                "This should be the same for all your servers you want to sync!")
                .define("sqliteDatabasePath", "./whitelistSync.db");
        SQLITE_SERVER_SYNC_TIMER = SERVER_BUILDER.comment("Time Interval in seconds for when the server " +
                "polls the whitelist changes from the database. (Only used in INTERVAL Sqlite Mode!)")
                .defineInRange("sqliteServerSyncTimer", 60, 1, Integer.MAX_VALUE);
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
    public static void onReload(final ModConfig.ModConfigEvent configEvent) {
    }

}
