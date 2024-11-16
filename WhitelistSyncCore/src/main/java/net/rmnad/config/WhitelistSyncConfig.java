package net.rmnad.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.moandjiezana.toml.Toml;
import net.rmnad.Log;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class WhitelistSyncConfig {

    public static WhitelistSyncModel Config;

    private static Path RuntimeDir = null;


    private static Path getConfigPath() {
        return RuntimeDir.resolve("config").resolve("whitelistsync2.json");
    }

    private static Path getLegacyConfigPath() {
        return RuntimeDir.resolve("config").resolve("whitelistsync2-common.toml");
    }

    public static void loadConfig() {
        loadConfig(null);
    }

    public static void loadConfig(Path runtimeDir) {
        if (runtimeDir == null) {
            RuntimeDir = Paths.get(".");
        } else {
            RuntimeDir = runtimeDir;
        }

        // Does config file exist?
        if (getConfigPath().toFile().exists()) {
            Config = readConfig();
        } else if (getLegacyConfigPath().toFile().exists()) {
            // Read legacy config file
            Config = readLegacyConfig();
            saveConfig();

            try {
                if (!getLegacyConfigPath().toFile().renameTo(RuntimeDir.resolve("config").resolve("whitelistsync2.toml.old").toFile())) {
                    Log.error("Error renaming legacy whitelistsync2 config file.");
                }
            } catch (Exception e) {
                Log.error("Error renaming legacy whitelistsync2 config file: " + e.getMessage());
            }

            Log.info("Converted legacy whitelistsync2 config file to new format. Old file has been renamed to whitelistsync2.toml.old");

        } else {
            // Create new config file
            Config = new WhitelistSyncModel();
            saveConfig();
            Log.info("Created new whitelistsync2 config file.");
        }
    }

    public static void saveConfig() {
        // Save config to file
        if (Config != null) {
            try (Writer writer = new FileWriter(getConfigPath().toFile())) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(Config, writer);
            } catch (IOException e) {
                Log.error("Error saving whitelistsync2 config file: " + e.getMessage());
            }
        }
    }

    private static WhitelistSyncModel readConfig() {
        try (Reader reader = new FileReader(getConfigPath().toFile())) {
            Gson gson = new Gson();
            return gson.fromJson(reader, WhitelistSyncModel.class);
        } catch (IOException e) {
            Log.error("Error reading whitelistsync2 config file: " + e.getMessage());
        }

        return null;
    }

    private static WhitelistSyncModel readLegacyConfig() {
        try (Reader reader = new FileReader(getLegacyConfigPath().toFile())) {
            Toml toml = new Toml().read(reader);

            WhitelistSyncModel config = new WhitelistSyncModel();

            config.syncTimer = toml.getLong("general.syncTimer").intValue();
            config.syncOpList = toml.getBoolean("general.syncOpList");
            config.databaseMode = WhitelistSyncModel.DatabaseMode.valueOf(toml.getString("general.databaseMode"));
            config.verboseLogging = toml.getBoolean("general.verboseLogging");

            config.mysqlDbName = toml.getString("mySQL.mysqlDbName");
            config.mysqlIp = toml.getString("mySQL.mysqlIp");
            config.mysqlUsername = toml.getString("mySQL.mysqlUsername");
            config.mysqlPassword = toml.getString("mySQL.mysqlPassword");
            config.mysqlPort = toml.getLong("mySQL.mysqlPort").intValue();

            config.sqliteDatabasePath = toml.getString("sqlite.sqliteDatabasePath");

            config.webApiHost = toml.getString("web.webApiHost");
            config.webApiKey = toml.getString("web.webApiKey");
            config.webSyncBannedPlayers = toml.getBoolean("web.webSyncBannedPlayers");
            config.webSyncBannedIps = toml.getBoolean("web.webSyncBannedIps");

            return config;
        } catch (IOException e) {
            Log.error("Error reading whitelistsync2 legacy config file: " + e.getMessage());
        }

        return null;
    }
}
