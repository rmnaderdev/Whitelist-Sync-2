package net.rmnad.fabric_1_21;

import io.wispforest.owo.config.annotation.Config;

@Config(name = "whitelistsync2", wrapperName = "WhitelistSync2Config")
public class ConfigModel {

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
}
