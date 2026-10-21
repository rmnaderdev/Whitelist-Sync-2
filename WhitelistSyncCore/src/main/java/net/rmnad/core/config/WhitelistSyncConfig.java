package net.rmnad.core.config;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.EnumGetMethod;
import com.electronwill.nightconfig.core.file.FileConfig;
import net.rmnad.core.Log;

import java.io.*;

public class WhitelistSyncConfig {

    private FileConfig config;

    public static final String DATABASE_MODE_KEY = "general.databaseMode";
    public static final String SYNC_OP_LIST_KEY = "general.syncOpList";
    public static final String SYNC_TIMER_KEY = "general.syncTimer";
    public static final String VERBOSE_LOGGING_KEY = "general.verboseLogging";
    public static final String SYNC_BANNED_PLAYERS_KEY = "general.syncBannedPlayers";
    public static final String SYNC_BANNED_IPS_KEY = "general.syncBannedIps";

    public static final String MYSQL_DB_NAME_KEY = "mySQL.mysqlDbName";
    public static final String MYSQL_IP_KEY = "mySQL.mysqlIp";
    public static final String MYSQL_PORT_KEY = "mySQL.mysqlPort";
    public static final String MYSQL_USERNAME_KEY = "mySQL.mysqlUsername";
    public static final String MYSQL_PASSWORD_KEY = "mySQL.mysqlPassword";
    public static final String MYSQL_USE_SSL_KEY = "mySQL.mysqlUseSsl";

    public static final String POSTGRES_DB_NAME_KEY = "postgresql.postgresDbName";
    public static final String POSTGRES_SCHEMA_KEY = "postgresql.postgresSchema";
    public static final String POSTGRES_IP_KEY = "postgresql.postgresIp";
    public static final String POSTGRES_PORT_KEY = "postgresql.postgresPort";
    public static final String POSTGRES_USERNAME_KEY = "postgresql.postgresUsername";
    public static final String POSTGRES_PASSWORD_KEY = "postgresql.postgresPassword";
    public static final String POSTGRES_USE_SSL_KEY = "postgresql.postgresUseSsl";

    public static final String SQLITE_DATABASE_PATH_KEY = "sqlite.sqliteDatabasePath";

    public static final String WEB_API_HOST_KEY = "web.webApiHost";
    public static final String WEB_API_KEY_KEY = "web.webApiKey";
    public static final String WEB_SYNC_BANNED_PLAYERS_KEY = "web.webSyncBannedPlayers";
    public static final String WEB_SYNC_BANNED_IPS_KEY = "web.webSyncBannedIps";


    public enum DatabaseMode {
        MYSQL,
        POSTGRESQL,
        SQLITE,
        WEB
    }

    // General Settings
    public DatabaseMode databaseMode = DatabaseMode.SQLITE;
    public boolean syncOpList = false;
    public int syncTimer = 60;
    public boolean verboseLogging = false;
    // Banned player / IP syncing for MYSQL and SQLITE modes. (WEB mode uses its
    // own web.webSyncBannedPlayers / web.webSyncBannedIps settings.)
    public boolean syncBannedPlayers = false;
    public boolean syncBannedIps = false;

    // MYSQL Settings
    public String mysqlDbName = "WhitelistSync";
    public String mysqlIp = "localhost";
    public int mysqlPort = 3306;
    public String mysqlUsername = "root";
    public String mysqlPassword = "password";
    public boolean mysqlUseSsl = false;

    // POSTGRESQL Settings
    public String postgresDbName = "WhitelistSync";
    public String postgresSchema = "public";
    public String postgresIp = "localhost";
    public int postgresPort = 5432;
    public String postgresUsername = "postgres";
    public String postgresPassword = "password";
    public boolean postgresUseSsl = false;

    // SQLITE Settings
    public String sqliteDatabasePath = "./whitelistSync.db";

    // WEB Settings
    public String webApiHost = "https://whitelistsync.com/";
    public String webApiKey = "";
    public boolean webSyncBannedPlayers = false;
    public boolean webSyncBannedIps = false;

    public void load() {
        File configFile = new File("config/whitelistsync2-common.toml");
        configFile.getParentFile().mkdirs();

        config = FileConfig.builder(configFile)
                .defaultResource("/whitelistsync2-common.toml")
                .autosave()
                .build();

        config.load();

        ConfigSpec spec = getConfigSpec();
        if (!spec.isCorrect(config)) {
            ConfigSpec.CorrectionListener listener = (action, path, incorrectValue, correctedValue) -> {
                String pathString = String.join(",", path);
                Log.warning("Corrected " + pathString + ": was " + incorrectValue + ", is now " + correctedValue);
            };
            spec.correct(config, listener);
            config.save();

        }

        // getOrElse guards against a missing key returning null and NPE-ing on
        // unboxing into the primitive fields below.
        databaseMode = config.getEnumOrElse(DATABASE_MODE_KEY, databaseMode, EnumGetMethod.NAME);
        syncOpList = config.getOrElse(SYNC_OP_LIST_KEY, syncOpList);
        syncTimer = config.getOrElse(SYNC_TIMER_KEY, syncTimer);
        verboseLogging = config.getOrElse(VERBOSE_LOGGING_KEY, verboseLogging);
        syncBannedPlayers = config.getOrElse(SYNC_BANNED_PLAYERS_KEY, syncBannedPlayers);
        syncBannedIps = config.getOrElse(SYNC_BANNED_IPS_KEY, syncBannedIps);

        mysqlDbName = config.getOrElse(MYSQL_DB_NAME_KEY, mysqlDbName);
        mysqlIp = config.getOrElse(MYSQL_IP_KEY, mysqlIp);
        mysqlPort = config.getOrElse(MYSQL_PORT_KEY, mysqlPort);
        mysqlUsername = config.getOrElse(MYSQL_USERNAME_KEY, mysqlUsername);
        mysqlPassword = config.getOrElse(MYSQL_PASSWORD_KEY, mysqlPassword);
        mysqlUseSsl = config.getOrElse(MYSQL_USE_SSL_KEY, mysqlUseSsl);

        postgresDbName = config.getOrElse(POSTGRES_DB_NAME_KEY, postgresDbName);
        postgresSchema = config.getOrElse(POSTGRES_SCHEMA_KEY, postgresSchema);
        postgresIp = config.getOrElse(POSTGRES_IP_KEY, postgresIp);
        postgresPort = config.getOrElse(POSTGRES_PORT_KEY, postgresPort);
        postgresUsername = config.getOrElse(POSTGRES_USERNAME_KEY, postgresUsername);
        postgresPassword = config.getOrElse(POSTGRES_PASSWORD_KEY, postgresPassword);
        postgresUseSsl = config.getOrElse(POSTGRES_USE_SSL_KEY, postgresUseSsl);

        sqliteDatabasePath = config.getOrElse(SQLITE_DATABASE_PATH_KEY, sqliteDatabasePath);

        webApiHost = config.getOrElse(WEB_API_HOST_KEY, webApiHost);
        webApiKey = config.getOrElse(WEB_API_KEY_KEY, webApiKey);
        webSyncBannedPlayers = config.getOrElse(WEB_SYNC_BANNED_PLAYERS_KEY, webSyncBannedPlayers);
        webSyncBannedIps = config.getOrElse(WEB_SYNC_BANNED_IPS_KEY, webSyncBannedIps);
    }

    public static ConfigSpec getConfigSpec() {
        ConfigSpec spec = new ConfigSpec();

        spec.defineEnum(DATABASE_MODE_KEY, DatabaseMode.SQLITE, EnumGetMethod.NAME);
        spec.define(SYNC_OP_LIST_KEY, false);
        spec.define(SYNC_TIMER_KEY, 60);
        spec.define(VERBOSE_LOGGING_KEY, false);
        spec.define(SYNC_BANNED_PLAYERS_KEY, false);
        spec.define(SYNC_BANNED_IPS_KEY, false);
        spec.define(MYSQL_DB_NAME_KEY, "WhitelistSync");
        spec.define(MYSQL_IP_KEY, "localhost");
        spec.define(MYSQL_PORT_KEY, 3306);
        spec.define(MYSQL_USERNAME_KEY, "root");
        spec.define(MYSQL_PASSWORD_KEY, "password");
        spec.define(MYSQL_USE_SSL_KEY, false);
        spec.define(POSTGRES_DB_NAME_KEY, "WhitelistSync");
        spec.define(POSTGRES_SCHEMA_KEY, "public");
        spec.define(POSTGRES_IP_KEY, "localhost");
        spec.define(POSTGRES_PORT_KEY, 5432);
        spec.define(POSTGRES_USERNAME_KEY, "postgres");
        spec.define(POSTGRES_PASSWORD_KEY, "password");
        spec.define(POSTGRES_USE_SSL_KEY, false);
        spec.define(SQLITE_DATABASE_PATH_KEY, "./whitelistSync.db");
        spec.define(WEB_API_HOST_KEY, "https://whitelistsync.com/");
        spec.define(WEB_API_KEY_KEY, "");
        spec.define(WEB_SYNC_BANNED_PLAYERS_KEY, false);
        spec.define(WEB_SYNC_BANNED_IPS_KEY, false);

        return spec;
    }
}
