package net.rmnad.config;

public class WhitelistSyncModel {

    public enum DatabaseMode {
        MYSQL,
        SQLITE,
        WEB
    }

    // General Settings
    protected DatabaseMode databaseMode = DatabaseMode.SQLITE;
    protected boolean syncOpList = false;
    protected int syncTimer = 60;
    protected boolean verboseLogging = false;

    // MYSQL Settings
    protected String mysqlDbName = "WhitelistSync";
    protected String mysqlIp = "localhost";
    protected int mysqlPort = 3306;
    protected String mysqlUsername = "root";
    protected String mysqlPassword = "password";

    // SQLITE Settings
    protected String sqliteDatabasePath = "./whitelistSync.db";

    // WEB Settings
    protected String webApiHost = "https://whitelistsync.com/";
    protected String webApiKey = "";
    protected boolean webSyncBannedPlayers = false;
    protected boolean webSyncBannedIps = false;

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

    public boolean isWebSyncBannedPlayers() {
        return webSyncBannedPlayers;
    }

    public boolean isWebSyncBannedIps() {
        return webSyncBannedIps;
    }
}
