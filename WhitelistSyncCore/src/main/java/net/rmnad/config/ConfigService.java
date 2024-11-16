package net.rmnad.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.moandjiezana.toml.Toml;
import net.rmnad.Log;

import java.io.*;
import java.nio.file.Path;

public class ConfigService {

    public WhitelistSyncConfig config;

    private Path runtimeDir;

    public ConfigService(Path runtimeDir) {
        this.runtimeDir = runtimeDir;
    }

    private Path getConfigPath() {
        return runtimeDir.resolve("config").resolve("whitelistsync2.json");
    }

    private Path getLegacyConfigPath() {
        return runtimeDir.resolve("config").resolve("whitelistsync2-common.toml");
    }

    public void loadConfig() {
        // Does config file exist?
        if (getConfigPath().toFile().exists()) {
            readConfig();
        } else if (getLegacyConfigPath().toFile().exists()) {
            // Read legacy config file
            config = readLegacyConfig();
            saveConfig();

            try {
                if (!getLegacyConfigPath().toFile().renameTo(runtimeDir.resolve("config").resolve("whitelistsync2.toml.old").toFile())) {
                    Log.error("Error renaming legacy whitelistsync2 config file.");
                }
            } catch (Exception e) {
                Log.error("Error renaming legacy whitelistsync2 config file: " + e.getMessage());
            }

            Log.info("Converted legacy whitelistsync2 config file to new format. Old file has been renamed to whitelistsync2.toml.old");

        } else {
            // Create new config file
            config = new WhitelistSyncConfig();
            saveConfig();
            Log.info("Created new whitelistsync2 config file.");
        }
    }

    public void saveConfig() {
        // Save config to file
        if (config != null) {
            try (Writer writer = new FileWriter(getConfigPath().toFile())) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(config, writer);
            } catch (IOException e) {
                Log.error("Error saving whitelistsync2 config file: " + e.getMessage());
            }
        }
    }

    private WhitelistSyncConfig readConfig() {
        try (Reader reader = new FileReader(getConfigPath().toFile())) {
            Gson gson = new Gson();
            return gson.fromJson(reader, WhitelistSyncConfig.class);
        } catch (IOException e) {
            Log.error("Error reading whitelistsync2 config file: " + e.getMessage());
        }

        return null;
    }

    private WhitelistSyncConfig readLegacyConfig() {
        try (Reader reader = new FileReader(getLegacyConfigPath().toFile())) {
            Toml toml = new Toml().read(reader);

            WhitelistSyncConfig config = new WhitelistSyncConfig();

            config.syncTimer = toml.getLong("syncTimer").intValue();
            config.syncOpList = toml.getBoolean("syncOpList");
            config.databaseMode = WhitelistSyncConfig.DatabaseMode.valueOf(toml.getString("databaseMode"));
            config.verboseLogging = toml.getBoolean("verboseLogging");

            config.mysqlDbName = toml.getString("mysqlDbName");
            config.mysqlIp = toml.getString("mysqlIp");
            config.mysqlUsername = toml.getString("mysqlUsername");
            config.mysqlPassword = toml.getString("mysqlPassword");
            config.mysqlPort = toml.getLong("mysqlPort").intValue();

            config.sqliteDatabasePath = toml.getString("sqliteDatabasePath");

            config.webApiHost = toml.getString("webApiHost");
            config.webApiKey = toml.getString("webApiKey");

            return config;
        } catch (IOException e) {
            Log.error("Error reading whitelistsync2 legacy config file: " + e.getMessage());
        }

        return null;
    }

    public WhitelistSyncConfig getConfig() {
        return config;
    }
}
