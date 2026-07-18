package net.rmnad.core.services;

import io.reactivex.rxjava3.annotations.Nullable;
import net.rmnad.core.Log;
import net.rmnad.core.WhitelistSyncCore;
import net.rmnad.core.callbacks.IServerControl;
import net.rmnad.core.json.BannedIpsFileReader;
import net.rmnad.core.json.BannedPlayersFileReader;
import net.rmnad.core.json.OppedPlayersFileReader;
import net.rmnad.core.json.WhitelistedPlayersFileReader;
import net.rmnad.core.logging.LogMessages;
import net.rmnad.core.models.BannedIp;
import net.rmnad.core.models.BannedPlayer;
import net.rmnad.core.models.OppedPlayer;
import net.rmnad.core.models.WhitelistedPlayer;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Service for MYSQL Databases
 */
public class MySqlService implements BaseService {

    // MySQL identifiers are interpolated into DDL/queries below (a prepared
    // statement parameter cannot stand in for a schema name), so the database
    // name is restricted to a safe character set to prevent SQL injection.
    private static final Pattern VALID_DB_NAME = Pattern.compile("^[A-Za-z0-9_]{1,64}$");

    private final String databaseName;
    private final String url;
    private final String username;
    private final String password;

    private final IServerControl serverControl;

    // Single persistent connection, reused across sync operations instead of
    // opening a fresh one per query. Guarded by the per-method synchronization
    // below because a JDBC Connection is not thread-safe and both the polling
    // thread and command thread reach these methods.
    private Connection connection;

    public MySqlService(IServerControl serverControl) {

        String ip = WhitelistSyncCore.CONFIG.mysqlIp;
        int port = WhitelistSyncCore.CONFIG.mysqlPort;
        this.databaseName = WhitelistSyncCore.CONFIG.mysqlDbName;
        this.url = "jdbc:mysql://" + ip + ":" + port + "/?allowPublicKeyRetrieval=true&useSSL="
                + WhitelistSyncCore.CONFIG.mysqlUseSsl + "&serverTimezone=UTC";
        this.username = WhitelistSyncCore.CONFIG.mysqlUsername;
        this.password = WhitelistSyncCore.CONFIG.mysqlPassword;

        this.serverControl = serverControl;
    }

    // Returns the shared connection, (re)opening it if absent or no longer valid.
    // The validity check lets a connection dropped by the server (idle timeout,
    // restart) heal on the next call.
    private Connection getConnection() throws SQLException {
        if (connection == null || !connection.isValid(2)) {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ignored) {}
            }
            connection = DriverManager.getConnection(url, username, password);
        }
        return connection;
    }

    @Override
    public synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {}
            connection = null;
        }
    }

    // Function used to initialize the database file
    @Override
    public synchronized boolean initializeDatabase() {
        Log.info("Setting up the MySQL service...");
        boolean isSuccess = true;

        if (!VALID_DB_NAME.matcher(databaseName).matches()) {
            Log.error("Invalid mysqlDbName '" + databaseName + "'. Only letters, digits and underscores are allowed (max 64 characters).");
            return false;
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            Log.error("Failed to init com.mysql.cj.jdbc.Driver. Please report this to the developer.");
            Log.error(e.getMessage(), e);
            isSuccess = false;
        }


        if (isSuccess) {
            try {
                Connection conn = getConnection();
                Log.info("Connected to " + url + " successfully!");

                // Create database
                String sql = "CREATE DATABASE IF NOT EXISTS `" + databaseName + "`;";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.execute();
                }

                // Create whitelist table
                sql = "CREATE TABLE IF NOT EXISTS `" + databaseName + "`.`whitelist` ("
                        + "`uuid` VARCHAR(60) NOT NULL,"
                        + "`name` VARCHAR(20) NOT NULL,"
                        + "`whitelisted` TINYINT NOT NULL DEFAULT 1,"
                        + "PRIMARY KEY (`uuid`)"
                        + ")";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.execute();
                }

                // Create opped players table if enabled
                if (WhitelistSyncCore.CONFIG.syncOpList) {
                    sql = "CREATE TABLE IF NOT EXISTS `" + databaseName + "`.`op` ("
                            + "`uuid` VARCHAR(60) NOT NULL,"
                            + "`name` VARCHAR(20) NOT NULL,"
                            + "`isOp` TINYINT NOT NULL DEFAULT 1,"
                            + "PRIMARY KEY (`uuid`)"
                            + ")";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.execute();
                    }
                }

                // Create banned players table if enabled
                if (WhitelistSyncCore.CONFIG.syncBannedPlayers) {
                    sql = "CREATE TABLE IF NOT EXISTS `" + databaseName + "`.`bannedPlayers` ("
                            + "`uuid` VARCHAR(60) NOT NULL,"
                            + "`name` VARCHAR(20) NOT NULL,"
                            + "`reason` TEXT,"
                            + "`banned` TINYINT NOT NULL DEFAULT 1,"
                            + "PRIMARY KEY (`uuid`)"
                            + ")";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.execute();
                    }
                }

                // Create banned ips table if enabled
                if (WhitelistSyncCore.CONFIG.syncBannedIps) {
                    sql = "CREATE TABLE IF NOT EXISTS `" + databaseName + "`.`bannedIps` ("
                            + "`ip` VARCHAR(45) NOT NULL,"
                            + "`reason` TEXT,"
                            + "`banned` TINYINT NOT NULL DEFAULT 1,"
                            + "PRIMARY KEY (`ip`)"
                            + ")";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.execute();
                    }
                }

                Log.info("Setup MySQL database!");
            } catch (SQLException e) {
                Log.error("Failed to connect to the mySQL database! Did you set one up in the config?");
                Log.error(e.getMessage(), e);
                isSuccess = false;
            }
        }


        return isSuccess;
    }

    @Override
    public synchronized ArrayList<WhitelistedPlayer> getWhitelistedPlayersFromDatabase() {
        // ArrayList for whitelisted players.
        ArrayList<WhitelistedPlayer> whitelistedPlayers = new ArrayList<>();

        // Keep track of records.
        int records = 0;

        String sql = "SELECT uuid, name FROM `" + databaseName + "`.`whitelist` WHERE whitelisted = true;";

        Connection conn;
        try {
            conn = getConnection();
        } catch (SQLException e) {
            Log.error("Error querying whitelisted players from database!");
            Log.error(e.getMessage(), e);
            return whitelistedPlayers;
        }

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            long startTime = System.currentTimeMillis();

            // Add queried results to arraylist.
            while (rs.next()) {
                whitelistedPlayers.add(new WhitelistedPlayer(rs.getString("uuid"), rs.getString("name")));
                records++;
            }

            // Time taken
            long timeTaken = System.currentTimeMillis() - startTime;

            Log.debug(LogMessages.SuccessGetWhitelistedPlayersFromDatabase(timeTaken, records));
        } catch (SQLException e) {
            // Something is wrong...
            Log.error("Error querying whitelisted players from database!");
            Log.error(e.getMessage(), e);
        }
        return whitelistedPlayers;
    }

    @Override
    public synchronized ArrayList<OppedPlayer> getOppedPlayersFromDatabase() {
        // ArrayList for opped players.
        ArrayList<OppedPlayer> oppedPlayers = new ArrayList<>();

        if (!WhitelistSyncCore.CONFIG.syncOpList) {
            Log.error(LogMessages.ALERT_OP_SYNC_DISABLED);
            return oppedPlayers;
        }

        // Keep track of records.
        int records = 0;

        String sql = "SELECT uuid, name FROM `" + databaseName + "`.`op` WHERE isOp = true;";

        Connection conn;
        try {
            conn = getConnection();
        } catch (SQLException e) {
            Log.error("Error querying opped players from database!");
            Log.error(e.getMessage(), e);
            return oppedPlayers;
        }

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            long startTime = System.currentTimeMillis();

            // Add queried results to arraylist.
            while (rs.next()) {
                OppedPlayer oppedPlayer = new OppedPlayer();
                oppedPlayer.setUuid(rs.getString("uuid"));
                oppedPlayer.setName(rs.getString("name"));

                oppedPlayers.add(oppedPlayer);
                records++;
            }

            // Time taken
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug(LogMessages.SuccessGetOppedPlayersFromDatabase(timeTaken, records));
        } catch (SQLException e) {
            Log.error("Error querying opped players from database!");
            Log.error(e.getMessage(), e);
        }

        return oppedPlayers;
    }

    @Override
    public synchronized ArrayList<BannedPlayer> getBannedPlayersFromDatabase() {
        ArrayList<BannedPlayer> bannedPlayers = new ArrayList<>();

        if (!WhitelistSyncCore.CONFIG.syncBannedPlayers) {
            Log.error(LogMessages.ALERT_BANNED_PLAYERS_SYNC_DISABLED);
            return bannedPlayers;
        }

        int records = 0;

        String sql = "SELECT uuid, name, reason FROM `" + databaseName + "`.`bannedPlayers` WHERE banned = true;";

        Connection conn;
        try {
            conn = getConnection();
        } catch (SQLException e) {
            Log.error("Error querying banned players from database!");
            Log.error(e.getMessage(), e);
            return bannedPlayers;
        }

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            long startTime = System.currentTimeMillis();

            while (rs.next()) {
                bannedPlayers.add(new BannedPlayer(rs.getString("uuid"), rs.getString("name"), rs.getString("reason")));
                records++;
            }

            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug(LogMessages.SuccessGetBannedPlayersFromDatabase(timeTaken, records));
        } catch (SQLException e) {
            Log.error("Error querying banned players from database!");
            Log.error(e.getMessage(), e);
        }

        return bannedPlayers;
    }

    @Override
    public synchronized ArrayList<String> getBannedIpsFromDatabase() {
        ArrayList<String> bannedIps = new ArrayList<>();

        if (!WhitelistSyncCore.CONFIG.syncBannedIps) {
            Log.error(LogMessages.ALERT_BANNED_IPS_SYNC_DISABLED);
            return bannedIps;
        }

        int records = 0;

        String sql = "SELECT ip FROM `" + databaseName + "`.`bannedIps` WHERE banned = true;";

        Connection conn;
        try {
            conn = getConnection();
        } catch (SQLException e) {
            Log.error("Error querying banned ips from database!");
            Log.error(e.getMessage(), e);
            return bannedIps;
        }

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            long startTime = System.currentTimeMillis();

            while (rs.next()) {
                bannedIps.add(rs.getString("ip"));
                records++;
            }

            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug(LogMessages.SuccessGetBannedIpsFromDatabase(timeTaken, records));
        } catch (SQLException e) {
            Log.error("Error querying banned ips from database!");
            Log.error(e.getMessage(), e);
        }

        return bannedIps;
    }

    @Override
    public synchronized boolean pushLocalWhitelistToDatabase() {
        // TODO: Start job on thread to avoid lag?
        // Keep track of records.
        int records = 0;
        long startTime = System.currentTimeMillis();

        ArrayList<WhitelistedPlayer> whitelistedPlayers
                = WhitelistedPlayersFileReader.getWhitelistedPlayers();

        String sql = "INSERT IGNORE INTO `" + databaseName + "`.`whitelist`(uuid, name, whitelisted) VALUES (?, ?, true)";

        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                // Loop through local whitelist and insert into database.
                for (WhitelistedPlayer player : whitelistedPlayers) {

                    if (player.getUuid() != null && player.getName() != null) {
                        stmt.setString(1, player.getUuid());
                        stmt.setString(2, player.getName());
                        stmt.executeUpdate();

                        records++;
                    }
                }
            }
            // Record time taken.
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug(LogMessages.SuccessPushLocalWhitelistToDatabase(timeTaken, records));

            return true;
        } catch (SQLException e) {
            Log.error(LogMessages.ERROR_PushLocalWhitelistToDatabase, e);
        }

        return false;
    }

    @Override
    public synchronized boolean pushLocalOpsToDatabase() {
        if (!WhitelistSyncCore.CONFIG.syncOpList) {
            Log.error(LogMessages.ALERT_OP_SYNC_DISABLED);
            return false;
        }

        // TODO: Start job on thread to avoid lag?
        // Keep track of records.
        int records = 0;
        long startTime = System.currentTimeMillis();

        ArrayList<OppedPlayer> oppedPlayers
                = OppedPlayersFileReader.getOppedPlayers();

        String sql = "INSERT IGNORE INTO `" + databaseName + "`.`op`(uuid, name, isOp) VALUES (?, ?, true)";

        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                // Loop through local whitelist and insert into database.
                for (OppedPlayer player : oppedPlayers) {

                    if (player.getUuid() != null && player.getName() != null) {
                        stmt.setString(1, player.getUuid());
                        stmt.setString(2, player.getName());
                        stmt.executeUpdate();

                        records++;
                    }
                }
            }
            // Record time taken.
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug(LogMessages.SuccessPushLocalOpsToDatabase(timeTaken, records));

            return true;
        } catch (SQLException e) {
            Log.error(LogMessages.ERROR_PushLocalOpsToDatabase, e);
        }

        return false;
    }

    @Override
    public synchronized boolean pushLocalBannedPlayersToDatabase() {
        if (!WhitelistSyncCore.CONFIG.syncBannedPlayers) {
            Log.error(LogMessages.ALERT_BANNED_PLAYERS_SYNC_DISABLED);
            return false;
        }

        int records = 0;
        long startTime = System.currentTimeMillis();

        ArrayList<BannedPlayer> bannedPlayers = BannedPlayersFileReader.getBannedPlayers();

        String sql = "INSERT IGNORE INTO `" + databaseName + "`.`bannedPlayers`(uuid, name, reason, banned) VALUES (?, ?, ?, true)";

        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (BannedPlayer player : bannedPlayers) {
                    if (player.getUuid() != null && player.getName() != null) {
                        stmt.setString(1, player.getUuid());
                        stmt.setString(2, player.getName());
                        stmt.setString(3, player.getReason());
                        stmt.executeUpdate();

                        records++;
                    }
                }
            }
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug(LogMessages.SuccessPushLocalBannedPlayersToDatabase(timeTaken, records));

            return true;
        } catch (SQLException e) {
            Log.error(LogMessages.ERROR_PushLocalBannedPlayersToDatabase, e);
        }

        return false;
    }

    @Override
    public synchronized boolean pushLocalBannedIpsToDatabase() {
        if (!WhitelistSyncCore.CONFIG.syncBannedIps) {
            Log.error(LogMessages.ALERT_BANNED_IPS_SYNC_DISABLED);
            return false;
        }

        int records = 0;
        long startTime = System.currentTimeMillis();

        ArrayList<BannedIp> bannedIps = BannedIpsFileReader.getBannedIps();

        String sql = "INSERT IGNORE INTO `" + databaseName + "`.`bannedIps`(ip, reason, banned) VALUES (?, ?, true)";

        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (BannedIp ip : bannedIps) {
                    if (ip.getIp() != null) {
                        stmt.setString(1, ip.getIp());
                        stmt.setString(2, ip.getReason());
                        stmt.executeUpdate();

                        records++;
                    }
                }
            }
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug(LogMessages.SuccessPushLocalBannedIpsToDatabase(timeTaken, records));

            return true;
        } catch (SQLException e) {
            Log.error(LogMessages.ERROR_PushLocalBannedIpsToDatabase, e);
        }

        return false;
    }

    @Override
    public synchronized boolean pullDatabaseWhitelistToLocal() {
        int records = 0;
        long startTime = System.currentTimeMillis();

        ArrayList<WhitelistedPlayer> localWhitelistedPlayers
                = WhitelistedPlayersFileReader.getWhitelistedPlayers();

        Set<String> localUuids = new HashSet<>();
        for (WhitelistedPlayer player : localWhitelistedPlayers) {
            if (player.getUuid() != null) {
                localUuids.add(player.getUuid());
            }
        }

        String sql = "SELECT name, uuid, whitelisted FROM `" + databaseName + "`.`whitelist`";

        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String name = rs.getString("name");
                    int whitelisted = rs.getInt("whitelisted");

                    if (whitelisted == 1) {
                        if (!localUuids.contains(uuid.toString())) {
                            try {
                                serverControl.addWhitelistPlayer(uuid, name);
                                Log.debug(LogMessages.AddedUserToWhitelist(name));
                                records++;
                            } catch (NullPointerException e) {
                                Log.error(e.getMessage(), e);
                            }
                        }
                    } else {
                        if (localUuids.contains(uuid.toString())) {
                            serverControl.removeWhitelistPlayer(uuid, name);
                            Log.debug(LogMessages.RemovedUserToWhitelist(name));
                            records++;
                        }
                    }

                }
            }
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug(LogMessages.SuccessPullDatabaseWhitelistToLocal( timeTaken, records));

            return true;
        } catch (SQLException e) {
            Log.error(LogMessages.ERROR_PullDatabaseWhitelistToLocal, e);
        }

        return false;
    }

    @Override
    public synchronized boolean pullDatabaseOpsToLocal() {
        if (!WhitelistSyncCore.CONFIG.syncOpList) {
            Log.error(LogMessages.ALERT_OP_SYNC_DISABLED);
            return false;
        }

        // TODO: Compare level and bypassesPlayerLimit, sync if needed
        int records = 0;
        long startTime = System.currentTimeMillis();

        ArrayList<OppedPlayer> localOppedPlayers
                = OppedPlayersFileReader.getOppedPlayers();

        Set<String> localUuids = new HashSet<>();
        for (OppedPlayer player : localOppedPlayers) {
            if (player.getUuid() != null) {
                localUuids.add(player.getUuid());
            }
        }

        String sql = "SELECT uuid, name, isOp FROM `" + databaseName + "`.`op`";

        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String name = rs.getString("name");
                    int opped = rs.getInt("isOp");

                    if (opped == 1) {
                        if (!localUuids.contains(uuid.toString())) {
                            try {
                                serverControl.addOpPlayer(uuid, name);
                                Log.debug(LogMessages.OppedUser(name));
                                records++;
                            } catch (NullPointerException e) {
                                Log.error(e.getMessage(), e);
                            }
                        }
                    } else {
                        if (localUuids.contains(uuid.toString())) {
                            serverControl.removeOpPlayer(uuid, name);
                            Log.debug(LogMessages.DeopUser(name));
                            records++;
                        }
                    }

                }
            }
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug(LogMessages.SuccessPullDatabaseOpsToLocal(timeTaken, records));

            return true;
        } catch (SQLException e) {
            Log.error("Error querying opped players from database!");
            Log.error(e.getMessage(), e);
        }

        return false;
    }

    @Override
    public synchronized boolean pullDatabaseBannedPlayersToLocal() {
        if (!WhitelistSyncCore.CONFIG.syncBannedPlayers) {
            Log.error(LogMessages.ALERT_BANNED_PLAYERS_SYNC_DISABLED);
            return false;
        }

        int records = 0;
        long startTime = System.currentTimeMillis();

        ArrayList<BannedPlayer> localBannedPlayers = BannedPlayersFileReader.getBannedPlayers();

        Set<String> localUuids = new HashSet<>();
        for (BannedPlayer player : localBannedPlayers) {
            if (player.getUuid() != null) {
                localUuids.add(player.getUuid());
            }
        }

        String sql = "SELECT uuid, name, reason, banned FROM `" + databaseName + "`.`bannedPlayers`";

        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String name = rs.getString("name");
                    String reason = rs.getString("reason");
                    int banned = rs.getInt("banned");

                    if (banned == 1) {
                        if (!localUuids.contains(uuid.toString())) {
                            try {
                                serverControl.addBannedPlayer(uuid, name, reason);
                                Log.debug(LogMessages.BannedPlayer(name));
                                records++;
                            } catch (NullPointerException e) {
                                Log.error(e.getMessage(), e);
                            }
                        }
                    } else {
                        if (localUuids.contains(uuid.toString())) {
                            serverControl.removeBannedPlayer(uuid, name);
                            Log.debug("Unbanned player " + name + ".");
                            records++;
                        }
                    }
                }
            }
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug(LogMessages.SuccessPullDatabaseBannedPlayersToLocal(timeTaken, records));

            return true;
        } catch (SQLException e) {
            Log.error("Error querying banned players from database!");
            Log.error(e.getMessage(), e);
        }

        return false;
    }

    @Override
    public synchronized boolean pullDatabaseBannedIpsToLocal() {
        if (!WhitelistSyncCore.CONFIG.syncBannedIps) {
            Log.error(LogMessages.ALERT_BANNED_IPS_SYNC_DISABLED);
            return false;
        }

        int records = 0;
        long startTime = System.currentTimeMillis();

        ArrayList<BannedIp> localBannedIps = BannedIpsFileReader.getBannedIps();

        Set<String> localIps = new HashSet<>();
        for (BannedIp bannedIp : localBannedIps) {
            if (bannedIp.getIp() != null) {
                localIps.add(bannedIp.getIp());
            }
        }

        String sql = "SELECT ip, reason, banned FROM `" + databaseName + "`.`bannedIps`";

        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    String ip = rs.getString("ip");
                    String reason = rs.getString("reason");
                    int banned = rs.getInt("banned");

                    if (banned == 1) {
                        if (!localIps.contains(ip)) {
                            try {
                                serverControl.addBannedIp(ip, reason);
                                Log.debug(LogMessages.BannedIp(ip));
                                records++;
                            } catch (NullPointerException e) {
                                Log.error(e.getMessage(), e);
                            }
                        }
                    } else {
                        if (localIps.contains(ip)) {
                            serverControl.removeBannedIp(ip);
                            Log.debug("Unbanned ip " + ip + ".");
                            records++;
                        }
                    }
                }
            }
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug(LogMessages.SuccessPullDatabaseBannedIpsToLocal(timeTaken, records));

            return true;
        } catch (SQLException e) {
            Log.error("Error querying banned ips from database!");
            Log.error(e.getMessage(), e);
        }

        return false;
    }

    @Override
    public synchronized boolean addWhitelistPlayer(UUID uuid, String name) {
        long startTime = System.currentTimeMillis();

        String sql = "REPLACE INTO `" + databaseName + "`.`whitelist`(uuid, name, whitelisted) VALUES (?, ?, true)";

        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, name);
                stmt.executeUpdate();
            }

            // Time taken.
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug("Added " + name + " to whitelist | Took " + timeTaken + "ms");
            return true;

        } catch (SQLException e) {
            Log.error("Error adding " + name + " to whitelist database!");
            Log.error(e.getMessage(), e);
        }

        return false;
    }

    @Override
    public synchronized boolean addOppedPlayer(UUID uuid, String name) {
        if (!WhitelistSyncCore.CONFIG.syncOpList) {
            Log.error(LogMessages.ALERT_OP_SYNC_DISABLED);
            return false;
        }

        long startTime = System.currentTimeMillis();

        String sql = "REPLACE INTO `" + databaseName + "`.`op`(uuid, name, isOp) VALUES (?, ?, true)";

        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, name);
                stmt.executeUpdate();
            }

            // Time taken.
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug("Database opped " + name + " | Took " + timeTaken + "ms");
            return true;

        } catch (SQLException e) {
            Log.error("Error opping " + name + " !");
            Log.error(e.getMessage(), e);
        }

        return false;
    }

    @Override
    public synchronized boolean addBannedPlayer(UUID uuid, String name, @Nullable String reason) {
        if (!WhitelistSyncCore.CONFIG.syncBannedPlayers) {
            Log.error(LogMessages.ALERT_BANNED_PLAYERS_SYNC_DISABLED);
            return false;
        }

        long startTime = System.currentTimeMillis();

        String sql = "REPLACE INTO `" + databaseName + "`.`bannedPlayers`(uuid, name, reason, banned) VALUES (?, ?, ?, true)";

        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, name);
                stmt.setString(3, reason);
                stmt.executeUpdate();
            }

            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug("Banned " + name + " | Took " + timeTaken + "ms");
            return true;

        } catch (SQLException e) {
            Log.error("Error banning " + name + " !");
            Log.error(e.getMessage(), e);
        }

        return false;
    }

    @Override
    public synchronized boolean addBannedIp(String ip, @Nullable String reason) {
        if (!WhitelistSyncCore.CONFIG.syncBannedIps) {
            Log.error(LogMessages.ALERT_BANNED_IPS_SYNC_DISABLED);
            return false;
        }

        long startTime = System.currentTimeMillis();

        String sql = "REPLACE INTO `" + databaseName + "`.`bannedIps`(ip, reason, banned) VALUES (?, ?, true)";

        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, ip);
                stmt.setString(2, reason);
                stmt.executeUpdate();
            }

            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug("Banned " + ip + " | Took " + timeTaken + "ms");
            return true;

        } catch (SQLException e) {
            Log.error("Error banning " + ip + " !");
            Log.error(e.getMessage(), e);
        }

        return false;
    }

    @Override
    public synchronized boolean removeWhitelistPlayer(UUID uuid, String name) {
        long startTime = System.currentTimeMillis();

        String sql = "REPLACE INTO `" + databaseName + "`.`whitelist`(uuid, name, whitelisted) VALUES (?, ?, false)";

        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, name);
                stmt.executeUpdate();
            }

            // Time taken.
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug("Removed " + name + " from whitelist | Took " + timeTaken + "ms");
            return true;

        } catch (SQLException e) {
            Log.error("Error removing " + name + " from whitelist database!");
            Log.error(e.getMessage(), e);
        }

        return false;
    }

    @Override
    public synchronized boolean removeOppedPlayer(UUID uuid, String name) {
        if (!WhitelistSyncCore.CONFIG.syncOpList) {
            Log.error(LogMessages.ALERT_OP_SYNC_DISABLED);
            return false;
        }

        long startTime = System.currentTimeMillis();

        String sql = "REPLACE INTO `" + databaseName + "`.`op`(uuid, name, isOp) VALUES (?, ?, false)";

        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, name);
                stmt.executeUpdate();
            }

            // Time taken.
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug("Deopped " + name + " | Took " + timeTaken + "ms");
            return true;

        } catch (SQLException e) {
            Log.error("Error deopping " + name + ".");
            Log.error(e.getMessage(), e);
        }

        return false;
    }

    @Override
    public synchronized boolean removeBannedPlayer(UUID uuid, String name) {
        if (!WhitelistSyncCore.CONFIG.syncBannedPlayers) {
            Log.error(LogMessages.ALERT_BANNED_PLAYERS_SYNC_DISABLED);
            return false;
        }

        long startTime = System.currentTimeMillis();

        String sql = "REPLACE INTO `" + databaseName + "`.`bannedPlayers`(uuid, name, reason, banned) VALUES (?, ?, NULL, false)";

        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, name);
                stmt.executeUpdate();
            }

            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug("Unbanned " + name + " | Took " + timeTaken + "ms");
            return true;

        } catch (SQLException e) {
            Log.error("Error unbanning " + name + ".");
            Log.error(e.getMessage(), e);
        }

        return false;
    }

    @Override
    public synchronized boolean removeBannedIp(String ip) {
        if (!WhitelistSyncCore.CONFIG.syncBannedIps) {
            Log.error(LogMessages.ALERT_BANNED_IPS_SYNC_DISABLED);
            return false;
        }

        long startTime = System.currentTimeMillis();

        String sql = "REPLACE INTO `" + databaseName + "`.`bannedIps`(ip, reason, banned) VALUES (?, NULL, false)";

        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, ip);
                stmt.executeUpdate();
            }

            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug("Unbanned " + ip + " | Took " + timeTaken + "ms");
            return true;

        } catch (SQLException e) {
            Log.error("Error unbanning " + ip + ".");
            Log.error(e.getMessage(), e);
        }

        return false;
    }
}
