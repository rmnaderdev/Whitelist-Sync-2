package net.rmnad.config;

public class WhitelistSyncConfig {

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

    public DatabaseMode getDatabaseMode() {
        return databaseMode;
    }

    public boolean isSyncOpList() {
        return syncOpList;
    }

    public int getSyncTimer() {
        return syncTimer;
    }

    public boolean isVerboseLogging() {
        return verboseLogging;
    }

    public String getMysqlDbName() {
        return mysqlDbName;
    }

    public String getMysqlIp() {
        return mysqlIp;
    }

    public int getMysqlPort() {
        return mysqlPort;
    }

    public String getMysqlUsername() {
        return mysqlUsername;
    }

    public String getMysqlPassword() {
        return mysqlPassword;
    }

    public String getSqliteDatabasePath() {
        return sqliteDatabasePath;
    }

    public String getWebApiHost() {
        return webApiHost;
    }

    public String getWebApiKey() {
        return webApiKey;
    }
}
