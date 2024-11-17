package net.rmnad.config;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.EnumGetMethod;
import com.electronwill.nightconfig.core.file.FileConfig;
import net.rmnad.Log;

import java.io.*;

public class WhitelistSyncConfig {

    public static FileConfig config;

    public static final String DATABASE_MODE_KEY = "general.databaseMode";
    public static final String SYNC_OP_LIST_KEY = "general.syncOpList";
    public static final String SYNC_TIMER_KEY = "general.syncTimer";
    public static final String VERBOSE_LOGGING_KEY = "general.verboseLogging";

    public static final String MYSQL_DB_NAME_KEY = "mySQL.mysqlDbName";
    public static final String MYSQL_IP_KEY = "mySQL.mysqlIp";
    public static final String MYSQL_PORT_KEY = "mySQL.mysqlPort";
    public static final String MYSQL_USERNAME_KEY = "mySQL.mysqlUsername";
    public static final String MYSQL_PASSWORD_KEY = "mySQL.mysqlPassword";

    public static final String SQLITE_DATABASE_PATH_KEY = "sqlite.sqliteDatabasePath";

    public static final String WEB_API_HOST_KEY = "web.webApiHost";
    public static final String WEB_API_KEY_KEY = "web.webApiKey";
    public static final String WEB_SYNC_BANNED_PLAYERS_KEY = "web.webSyncBannedPlayers";
    public static final String WEB_SYNC_BANNED_IPS_KEY = "web.webSyncBannedIps";


    public enum DatabaseMode {
        MYSQL,
        SQLITE,
        WEB
    }

    // General Settings
    public DatabaseMode databaseMode = DatabaseMode.SQLITE;
    public boolean syncOpList = false;
    public int syncTimer = 60;
    public boolean verboseLogging = false;

    // MYSQL Settings
    public String mysqlDbName = "WhitelistSync";
    public String mysqlIp = "localhost";
    public int mysqlPort = 3306;
    public String mysqlUsername = "root";
    public String mysqlPassword = "password";

    // SQLITE Settings
    public String sqliteDatabasePath = "./whitelistSync.db";

    // WEB Settings
    public String webApiHost = "https://whitelistsync.com/";
    public String webApiKey = "";
    public boolean webSyncBannedPlayers = false;
    public boolean webSyncBannedIps = false;

    public void load() {
        File configFile = new File("config/whitelistSync2-common.toml");
        configFile.getParentFile().mkdirs();

        config = FileConfig.builder(configFile)
                .defaultResource("/whitelistSync2-common.toml")
                .autosave()
                .build();

        config.load();

        ConfigSpec spec = getConfigSpec();
        if (!spec.isCorrect(config)) {
            ConfigSpec.CorrectionListener listener = (action, path, incorrectValue, correctedValue) -> {
                String pathString = String.join(",", path);
                Log.warning("Corrected " + pathString + ": was " + incorrectValue + ", is now " + correctedValue);
            };
            int numberOfCorrections = spec.correct(config, listener);
            config.save();

        }

        databaseMode = config.getEnum(DATABASE_MODE_KEY, DatabaseMode.class);
        syncOpList = config.get(SYNC_OP_LIST_KEY);
        syncTimer = config.get(SYNC_TIMER_KEY);
        verboseLogging = config.get(VERBOSE_LOGGING_KEY);

        mysqlDbName = config.get(MYSQL_DB_NAME_KEY);
        mysqlIp = config.get(MYSQL_IP_KEY);
        mysqlPort = config.get(MYSQL_PORT_KEY);
        mysqlUsername = config.get(MYSQL_USERNAME_KEY);
        mysqlPassword = config.get(MYSQL_PASSWORD_KEY);

        sqliteDatabasePath = config.get(SQLITE_DATABASE_PATH_KEY);

        webApiHost = config.get(WEB_API_HOST_KEY);
        webApiKey = config.get(WEB_API_KEY_KEY);
        webSyncBannedPlayers = config.get(WEB_SYNC_BANNED_PLAYERS_KEY);
        webSyncBannedIps = config.get(WEB_SYNC_BANNED_IPS_KEY);
    }

    public static ConfigSpec getConfigSpec() {
        ConfigSpec spec = new ConfigSpec();

        spec.defineEnum(DATABASE_MODE_KEY, DatabaseMode.SQLITE, EnumGetMethod.NAME);
        spec.define(SYNC_OP_LIST_KEY, false);
        spec.define(SYNC_TIMER_KEY, 60);
        spec.define(VERBOSE_LOGGING_KEY, false);
        spec.define(MYSQL_DB_NAME_KEY, "WhitelistSync");
        spec.define(MYSQL_IP_KEY, "localhost");
        spec.define(MYSQL_PORT_KEY, 3306);
        spec.define(MYSQL_USERNAME_KEY, "root");
        spec.define(MYSQL_PASSWORD_KEY, "password");
        spec.define(SQLITE_DATABASE_PATH_KEY, "./whitelistSync.db");
        spec.define(WEB_API_HOST_KEY, "https://whitelistsync.com/");
        spec.define(WEB_API_KEY_KEY, "");
        spec.define(WEB_SYNC_BANNED_PLAYERS_KEY, false);
        spec.define(WEB_SYNC_BANNED_IPS_KEY, false);

        return spec;
    }
}
