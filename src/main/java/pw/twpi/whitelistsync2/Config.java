package pw.twpi.whitelistsync2;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

public class Config {

    public static class Common {
        public enum DatabaseMode {
            MYSQL,
            SQLITE
//        POSTGRESQL
        }

        public final String CATEGORY_GENERAL = "general";
        public final String CATEGORY_MYSQL = "mySQL";
        public final String CATEGORY_SQLITE = "sqlite";
//    public static final String CATEGORY_POSTGRESQL = "postgresql";

        // General Settings
        public ForgeConfigSpec.EnumValue<DatabaseMode> DATABASE_MODE;
        public ForgeConfigSpec.BooleanValue SYNC_OP_LIST;
        public ForgeConfigSpec.IntValue SYNC_TIMER;

        // MYSQL Settings
        public ForgeConfigSpec.ConfigValue<String> MYSQL_DB_NAME;
        public ForgeConfigSpec.ConfigValue<String> MYSQL_IP;
        public ForgeConfigSpec.IntValue MYSQL_PORT;
        public ForgeConfigSpec.ConfigValue<String> MYSQL_USERNAME;
        public ForgeConfigSpec.ConfigValue<String> MYSQL_PASSWORD;

        // SQLITE Settings
        public ForgeConfigSpec.ConfigValue<String> SQLITE_DATABASE_PATH;

        // POSTGRESQL Settings
        //    public static ForgeConfigSpec.ConfigValue<String> POSTGRESQL_DB_NAME;
        //    public static ForgeConfigSpec.ConfigValue<String> POSTGRESQL_IP;
        //    public static ForgeConfigSpec.IntValue POSTGRESQL_PORT;
        //    public static ForgeConfigSpec.ConfigValue<String> POSTGRESQL_USERNAME;
        //    public static ForgeConfigSpec.ConfigValue<String> POSTGRESQL_PASSWORD;

        Common(final ForgeConfigSpec.Builder builder) {
            // General Settings
            builder.comment("General configuration").push(CATEGORY_GENERAL);
            DATABASE_MODE = builder.comment("Mode for the database.")
                    .worldRestart()
                    .defineEnum("databaseMode", DatabaseMode.SQLITE);
            SYNC_OP_LIST = builder.comment("Option on whether to sync the server's op list to the database.")
                    .worldRestart()
                    .define("syncOpList", false);
            SYNC_TIMER = builder.comment("Time Interval in seconds for when the server " +
                    "polls the whitelist changes from the database. " +
                    "(Warning! Time lower than 5 sec may affect performace.)")
                    .worldRestart()
                    .defineInRange("syncTimer", 60, 1, Integer.MAX_VALUE);
            builder.pop();


            // MYSQL Config
            builder.comment(
                    "MySQL configuration (To enable " +
                            "mySQL, refer to the mode setting in the general configuration)."
            ).push(CATEGORY_MYSQL);
            MYSQL_DB_NAME = builder.comment("Name for your mySQL database (No spaces!).")
                    .worldRestart()
                    .define("mysqlDbName", "WhitelistSync");
            MYSQL_IP = builder.comment("IP for your mySQL server.")
                    .worldRestart()
                    .define("mysqlIp", "localhost");
            MYSQL_PORT = builder.comment("Port for your mySQL server.")
                    .worldRestart()
                    .defineInRange("mysqlPort", 3306, 1, 65535);
            MYSQL_USERNAME = builder.comment("Username for your mySQL server.")
                    .worldRestart()
                    .define("mysqlUsername", "root");
            MYSQL_PASSWORD = builder.comment("Password for your mySQL server.")
                    .worldRestart()
                    .define("mysqlPassword", "password");
            builder.pop();


            // SQLITE Config
            builder.comment(
                    "Sqlite configuration (To enable Sqlite, " +
                            "refer to the mode setting in the general configuration)."
            ).push(CATEGORY_SQLITE);
            SQLITE_DATABASE_PATH = builder.comment("Insert System Path for your SQLite database file. " +
                    "This should be the same for all your servers you want to sync!")
                    .worldRestart()
                    .define("sqliteDatabasePath", "./whitelistSync.db");
            builder.pop();
        }
    }

    private static final ForgeConfigSpec commonSpec;
    public static final Common COMMON;

    static {
        final Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
        commonSpec = specPair.getRight();
        COMMON = specPair.getLeft();
    }

    public static void register(final ModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, commonSpec);
    }
}
