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

/**
 * Service for SQLITE Databases
 */
public class SqLiteService implements BaseService {

    private final String databasePath;
    private final IServerControl serverControl;

    // Single persistent connection, reused across sync operations instead of
    // opening a fresh one per query. Guarded by the per-method synchronization
    // below because a JDBC Connection is not thread-safe and both the polling
    // thread and command thread reach these methods.
    private Connection connection;

    public SqLiteService(IServerControl serverControl) {
        this.databasePath = WhitelistSyncCore.CONFIG.sqliteDatabasePath;
        this.serverControl = serverControl;
    }

    // Returns the shared connection, (re)opening it if absent or no longer valid.
    public Connection getConnection() throws SQLException {
        if (connection == null || !connection.isValid(2)) {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ignored) {}
            }
            String url = "jdbc:sqlite:" + this.databasePath;
            connection = DriverManager.getConnection(url);
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

    // Closes per-statement resources only. The connection is persistent and
    // owned by this service (see close()), so it is deliberately left open.
    public void cleanup(Statement stmt) {
        cleanup(null, stmt);
    }

    public void cleanup(ResultSet rs, Statement stmt) {
        try {
            if(rs != null) {
                rs.close();
            }
        } catch (SQLException ignored){}

        try {
            if(stmt != null) {
                stmt.close();
            }
        } catch (SQLException ignored){}
    }

    // Function used to initialize the database file
    @Override
    public synchronized boolean initializeDatabase() {
        Log.info("Setting up the SQLite service...");
        boolean success = true;

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            Log.error("Failed to init org.sqlite.JDBC driver. Please report this to the developer.");
            Log.error(e.getMessage(), e);
            success = false;
        }

        if(success) {
            Statement stmt = null;
            try {
                Connection conn = getConnection();

                // If the conn is valid, everything below this will run
                Log.info("Connected to SQLite database successfully!");

                // Create whitelist table if it doesn't exist.
                // SQL statement for creating a new table
                String sql = "CREATE TABLE IF NOT EXISTS whitelist (\n"
                        + "	uuid text NOT NULL PRIMARY KEY,\n"
                        + "	name text,\n"
                        + " whitelisted integer NOT NULL);";
                stmt = conn.createStatement();
                stmt.executeUpdate(sql);

                if (WhitelistSyncCore.CONFIG.syncOpList) {
                    // SQL statement for creating a new table
                    sql = "CREATE TABLE IF NOT EXISTS op (\n"
                            + "	uuid text NOT NULL PRIMARY KEY,\n"
                            + "	name text NOT NULL,\n"
                            + " isOp integer NOT NULL);";
                    stmt = conn.createStatement();
                    stmt.executeUpdate(sql);
                }

                if (WhitelistSyncCore.CONFIG.syncBannedPlayers) {
                    sql = "CREATE TABLE IF NOT EXISTS bannedPlayers (\n"
                            + "	uuid text NOT NULL PRIMARY KEY,\n"
                            + "	name text NOT NULL,\n"
                            + "	reason text,\n"
                            + " banned integer NOT NULL);";
                    stmt = conn.createStatement();
                    stmt.executeUpdate(sql);
                }

                if (WhitelistSyncCore.CONFIG.syncBannedIps) {
                    sql = "CREATE TABLE IF NOT EXISTS bannedIps (\n"
                            + "	ip text NOT NULL PRIMARY KEY,\n"
                            + "	reason text,\n"
                            + " banned integer NOT NULL);";
                    stmt = conn.createStatement();
                    stmt.executeUpdate(sql);
                }
            } catch (SQLException e) {
                Log.error("Error creating whitelist or op table!");
                Log.error(e.getMessage(), e);
                success = false;
            } finally {
                cleanup(stmt);
            }
        }

        return success;
    }

    @Override
    public synchronized ArrayList<WhitelistedPlayer> getWhitelistedPlayersFromDatabase() {
        // ArrayList for whitelisted players.
        ArrayList<WhitelistedPlayer> whitelistedPlayers = new ArrayList<>();

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            // Keep track of records.
            int records = 0;

            // Connect to database.
            Connection conn = getConnection();
            long startTime = System.currentTimeMillis();

            String sql = "SELECT uuid, name, whitelisted FROM whitelist WHERE whitelisted = 1;";
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            // Save queried return to names list.
            while (rs.next()) {
                whitelistedPlayers.add(new WhitelistedPlayer(rs.getString("uuid"), rs.getString("name")));
                records++;
            }

            // Total time taken.
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug(LogMessages.SuccessGetWhitelistedPlayersFromDatabase(timeTaken, records));

        } catch (SQLException e) {
            Log.error("Error querying whitelisted players from database!");
            Log.error(e.getMessage(), e);
        } finally {
            cleanup(rs, stmt);
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

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            // Keep track of records.
            int records = 0;

            // Connect to database.
            Connection conn = getConnection();
            long startTime = System.currentTimeMillis();

            String sql = "SELECT uuid, name FROM op WHERE isOp = 1;";
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            // Save queried return to names list.
            while (rs.next()) {
                OppedPlayer oppedPlayer = new OppedPlayer();
                oppedPlayer.setUuid(rs.getString("uuid"));
                oppedPlayer.setName(rs.getString("name"));

                oppedPlayers.add(oppedPlayer);
                records++;
            }

            // Total time taken.
            long timeTaken = System.currentTimeMillis() - startTime;

            Log.debug("Database pulled opped players | Took " + timeTaken + "ms | Read " + records + " records.");
        } catch (SQLException e) {
            Log.error("Error querying opped players from database!");
            Log.error(e.getMessage(), e);
        } finally {
            cleanup(rs, stmt);
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

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            int records = 0;

            Connection conn = getConnection();
            long startTime = System.currentTimeMillis();

            String sql = "SELECT uuid, name, reason FROM bannedPlayers WHERE banned = 1;";
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            while (rs.next()) {
                bannedPlayers.add(new BannedPlayer(rs.getString("uuid"), rs.getString("name"), rs.getString("reason")));
                records++;
            }

            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug(LogMessages.SuccessGetBannedPlayersFromDatabase(timeTaken, records));
        } catch (SQLException e) {
            Log.error("Error querying banned players from database!");
            Log.error(e.getMessage(), e);
        } finally {
            cleanup(rs, stmt);
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

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            int records = 0;

            Connection conn = getConnection();
            long startTime = System.currentTimeMillis();

            String sql = "SELECT ip FROM bannedIps WHERE banned = 1;";
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            while (rs.next()) {
                bannedIps.add(rs.getString("ip"));
                records++;
            }

            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug(LogMessages.SuccessGetBannedIpsFromDatabase(timeTaken, records));
        } catch (SQLException e) {
            Log.error("Error querying banned ips from database!");
            Log.error(e.getMessage(), e);
        } finally {
            cleanup(rs, stmt);
        }

        return bannedIps;
    }

    @Override
    public synchronized boolean pushLocalWhitelistToDatabase() {
        // TODO: Start job on thread to avoid lag?
        // Keep track of records.
        int records = 0;
        boolean success;
        long startTime = System.currentTimeMillis();

        ArrayList<WhitelistedPlayer> whitelistedPlayers = WhitelistedPlayersFileReader.getWhitelistedPlayers();

        PreparedStatement stmt = null;
        try {
            // Connect to database.
            Connection conn = getConnection();
            stmt = conn.prepareStatement("INSERT OR REPLACE INTO whitelist(uuid, name, whitelisted) VALUES (?, ?, 1)");
            // Loop through local whitelist and insert into database.
            for (WhitelistedPlayer player : whitelistedPlayers) {

                if (player.getUuid() != null && player.getName() != null) {
                    stmt.setString(1, player.getUuid());
                    stmt.setString(2, player.getName());
                    stmt.executeUpdate();

                    records++;
                }
            }
            // Record time taken.
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug(LogMessages.SuccessPushLocalWhitelistToDatabase(timeTaken, records));

            success = true;
        } catch (SQLException e) {
            Log.error(LogMessages.ERROR_PushLocalWhitelistToDatabase, e);
            success = false;
        } finally {
            cleanup(stmt);
        }

        return success;
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
        boolean success;
        long startTime = System.currentTimeMillis();

        ArrayList<OppedPlayer> oppedPlayers = OppedPlayersFileReader.getOppedPlayers();

        PreparedStatement stmt = null;
        try {
            // Connect to database.
            Connection conn = getConnection();
            stmt = conn.prepareStatement("INSERT OR REPLACE INTO op(uuid, name, isOp) VALUES (?, ?, 1)");
            // Loop through local opped players and insert into database.
            for (OppedPlayer player : oppedPlayers) {

                if (player.getUuid() != null && player.getName() != null) {
                    stmt.setString(1, player.getUuid());
                    stmt.setString(2, player.getName());
                    stmt.executeUpdate();

                    records++;
                }
            }
            // Record time taken.
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug(LogMessages.SuccessPushLocalOpsToDatabase(timeTaken, records));
            success = true;
        } catch (SQLException e) {
            Log.error(LogMessages.ERROR_PushLocalOpsToDatabase, e);
            success = false;
        } finally {
            cleanup(stmt);
        }

        return success;
    }

    @Override
    public synchronized boolean pushLocalBannedPlayersToDatabase() {
        if (!WhitelistSyncCore.CONFIG.syncBannedPlayers) {
            Log.error(LogMessages.ALERT_BANNED_PLAYERS_SYNC_DISABLED);
            return false;
        }

        int records = 0;
        boolean success;
        long startTime = System.currentTimeMillis();

        ArrayList<BannedPlayer> bannedPlayers = BannedPlayersFileReader.getBannedPlayers();

        PreparedStatement stmt = null;
        try {
            Connection conn = getConnection();
            stmt = conn.prepareStatement("INSERT OR REPLACE INTO bannedPlayers(uuid, name, reason, banned) VALUES (?, ?, ?, 1)");
            for (BannedPlayer player : bannedPlayers) {
                if (player.getUuid() != null && player.getName() != null) {
                    stmt.setString(1, player.getUuid());
                    stmt.setString(2, player.getName());
                    stmt.setString(3, player.getReason());
                    stmt.executeUpdate();

                    records++;
                }
            }
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug(LogMessages.SuccessPushLocalBannedPlayersToDatabase(timeTaken, records));

            success = true;
        } catch (SQLException e) {
            Log.error(LogMessages.ERROR_PushLocalBannedPlayersToDatabase, e);
            success = false;
        } finally {
            cleanup(stmt);
        }

        return success;
    }

    @Override
    public synchronized boolean pushLocalBannedIpsToDatabase() {
        if (!WhitelistSyncCore.CONFIG.syncBannedIps) {
            Log.error(LogMessages.ALERT_BANNED_IPS_SYNC_DISABLED);
            return false;
        }

        int records = 0;
        boolean success;
        long startTime = System.currentTimeMillis();

        ArrayList<BannedIp> bannedIps = BannedIpsFileReader.getBannedIps();

        PreparedStatement stmt = null;
        try {
            Connection conn = getConnection();
            stmt = conn.prepareStatement("INSERT OR REPLACE INTO bannedIps(ip, reason, banned) VALUES (?, ?, 1)");
            for (BannedIp ip : bannedIps) {
                if (ip.getIp() != null) {
                    stmt.setString(1, ip.getIp());
                    stmt.setString(2, ip.getReason());
                    stmt.executeUpdate();

                    records++;
                }
            }
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug(LogMessages.SuccessPushLocalBannedIpsToDatabase(timeTaken, records));

            success = true;
        } catch (SQLException e) {
            Log.error(LogMessages.ERROR_PushLocalBannedIpsToDatabase, e);
            success = false;
        } finally {
            cleanup(stmt);
        }

        return success;
    }

    @Override
    public synchronized boolean pullDatabaseWhitelistToLocal() {
        int records = 0;
        boolean success;
        long startTime = System.currentTimeMillis();

        ArrayList<WhitelistedPlayer> localWhitelistedPlayers = WhitelistedPlayersFileReader.getWhitelistedPlayers();

        Set<String> localUuids = new HashSet<>();
        for (WhitelistedPlayer player : localWhitelistedPlayers) {
            if (player.getUuid() != null) {
                localUuids.add(player.getUuid());
            }
        }

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            Connection conn = getConnection();

            String sql = "SELECT name, uuid, whitelisted FROM whitelist;";
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String name = rs.getString("name");
                int whitelisted = rs.getInt("whitelisted");

                if (whitelisted == 1) {
                    if (!localUuids.contains(uuid.toString())) {
                        try {
                            this.serverControl.addWhitelistPlayer(uuid, name);
                            Log.debug(LogMessages.AddedUserToWhitelist(name));
                            records++;
                        } catch (NullPointerException e) {
                            Log.error(e.getMessage(), e);
                        }
                    }
                } else {
                    if (localUuids.contains(uuid.toString())) {
                        this.serverControl.removeWhitelistPlayer(uuid, name);
                        Log.debug(LogMessages.RemovedUserToWhitelist(name));
                        records++;
                    }
                }

            }
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug(LogMessages.SuccessPullDatabaseWhitelistToLocal( timeTaken, records));

            success = true;
        } catch (SQLException e) {
            Log.error(LogMessages.ERROR_PullDatabaseWhitelistToLocal, e);
            success = false;
        } finally {
            cleanup(rs, stmt);
        }

        return success;
    }

    @Override
    public synchronized boolean pullDatabaseOpsToLocal() {

        // TODO: Compare level and bypassesPlayerLimit, sync if needed
        if (!WhitelistSyncCore.CONFIG.syncOpList) {
            Log.error(LogMessages.ALERT_OP_SYNC_DISABLED);
            return false;
        }

        int records = 0;
        boolean success;

        long startTime = System.currentTimeMillis();

        ArrayList<OppedPlayer> localOppedPlayers = OppedPlayersFileReader.getOppedPlayers();

        Set<String> localUuids = new HashSet<>();
        for (OppedPlayer player : localOppedPlayers) {
            if (player.getUuid() != null) {
                localUuids.add(player.getUuid());
            }
        }

        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            Connection conn = getConnection();

            String sql = "SELECT uuid, name, isOp FROM op;";
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String name = rs.getString("name");
                int opped = rs.getInt("isOp");

                if (opped == 1) {
                    if (!localUuids.contains(uuid.toString())) {
                        try {
                            this.serverControl.addOpPlayer(uuid, name);
                            Log.debug(LogMessages.OppedUser(name));
                            records++;
                        } catch (NullPointerException e) {
                            Log.error(e.getMessage(), e);
                        }
                    }
                } else {
                    if (localUuids.contains(uuid.toString())) {
                        this.serverControl.removeOpPlayer(uuid, name);
                        Log.debug(LogMessages.DeopUser(name));
                        records++;
                    }
                }
            }
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug(LogMessages.SuccessPullDatabaseOpsToLocal(timeTaken, records));

            success = true;
        } catch (SQLException e) {
            Log.error("Error querying opped players from database!");
            Log.error(e.getMessage(), e);
            success = false;
        } finally {
            cleanup(rs, stmt);
        }

        return success;
    }

    @Override
    public synchronized boolean pullDatabaseBannedPlayersToLocal() {
        if (!WhitelistSyncCore.CONFIG.syncBannedPlayers) {
            Log.error(LogMessages.ALERT_BANNED_PLAYERS_SYNC_DISABLED);
            return false;
        }

        int records = 0;
        boolean success;
        long startTime = System.currentTimeMillis();

        ArrayList<BannedPlayer> localBannedPlayers = BannedPlayersFileReader.getBannedPlayers();

        Set<String> localUuids = new HashSet<>();
        for (BannedPlayer player : localBannedPlayers) {
            if (player.getUuid() != null) {
                localUuids.add(player.getUuid());
            }
        }

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            Connection conn = getConnection();

            String sql = "SELECT uuid, name, reason, banned FROM bannedPlayers;";
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String name = rs.getString("name");
                String reason = rs.getString("reason");
                int banned = rs.getInt("banned");

                if (banned == 1) {
                    if (!localUuids.contains(uuid.toString())) {
                        try {
                            this.serverControl.addBannedPlayer(uuid, name, reason);
                            Log.debug(LogMessages.BannedPlayer(name));
                            records++;
                        } catch (NullPointerException e) {
                            Log.error(e.getMessage(), e);
                        }
                    }
                } else {
                    if (localUuids.contains(uuid.toString())) {
                        this.serverControl.removeBannedPlayer(uuid, name);
                        Log.debug("Unbanned player " + name + ".");
                        records++;
                    }
                }
            }
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug(LogMessages.SuccessPullDatabaseBannedPlayersToLocal(timeTaken, records));

            success = true;
        } catch (SQLException e) {
            Log.error("Error querying banned players from database!");
            Log.error(e.getMessage(), e);
            success = false;
        } finally {
            cleanup(rs, stmt);
        }

        return success;
    }

    @Override
    public synchronized boolean pullDatabaseBannedIpsToLocal() {
        if (!WhitelistSyncCore.CONFIG.syncBannedIps) {
            Log.error(LogMessages.ALERT_BANNED_IPS_SYNC_DISABLED);
            return false;
        }

        int records = 0;
        boolean success;
        long startTime = System.currentTimeMillis();

        ArrayList<BannedIp> localBannedIps = BannedIpsFileReader.getBannedIps();

        Set<String> localIps = new HashSet<>();
        for (BannedIp bannedIp : localBannedIps) {
            if (bannedIp.getIp() != null) {
                localIps.add(bannedIp.getIp());
            }
        }

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            Connection conn = getConnection();

            String sql = "SELECT ip, reason, banned FROM bannedIps;";
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            while (rs.next()) {
                String ip = rs.getString("ip");
                String reason = rs.getString("reason");
                int banned = rs.getInt("banned");

                if (banned == 1) {
                    if (!localIps.contains(ip)) {
                        try {
                            this.serverControl.addBannedIp(ip, reason);
                            Log.debug(LogMessages.BannedIp(ip));
                            records++;
                        } catch (NullPointerException e) {
                            Log.error(e.getMessage(), e);
                        }
                    }
                } else {
                    if (localIps.contains(ip)) {
                        this.serverControl.removeBannedIp(ip);
                        Log.debug("Unbanned ip " + ip + ".");
                        records++;
                    }
                }
            }
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug(LogMessages.SuccessPullDatabaseBannedIpsToLocal(timeTaken, records));

            success = true;
        } catch (SQLException e) {
            Log.error("Error querying banned ips from database!");
            Log.error(e.getMessage(), e);
            success = false;
        } finally {
            cleanup(rs, stmt);
        }

        return success;
    }

    @Override
    public synchronized boolean addWhitelistPlayer(UUID uuid, String name) {
        boolean success;
        PreparedStatement stmt = null;
        try {
            // Open connection
            Connection conn = getConnection();

            // Start time.
            long startTime = System.currentTimeMillis();

            String sql = "INSERT OR REPLACE INTO whitelist(uuid, name, whitelisted) VALUES (?, ?, 1)";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, uuid.toString());
            stmt.setString(2, name);
            stmt.executeUpdate();

            // Time taken.
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug("Added " + name + " to whitelist | Took " + timeTaken + "ms");

            success = true;
        } catch (SQLException e) {
            Log.error("Error adding " + name + " to whitelist database!");
            Log.error(e.getMessage(), e);
            success = false;
        } finally {
            cleanup(stmt);
        }

        return success;
    }

    @Override
    public synchronized boolean addOppedPlayer(UUID uuid, String name) {
        if (!WhitelistSyncCore.CONFIG.syncOpList) {
            Log.error(LogMessages.ALERT_OP_SYNC_DISABLED);
            return false;
        }

        boolean success;
        PreparedStatement stmt = null;
        try {
            // Open connection
            Connection conn = getConnection();

            // Start time.
            long startTime = System.currentTimeMillis();

            stmt = conn.prepareStatement("INSERT OR REPLACE INTO op(uuid, name, isOp) VALUES (?, ?, 1)");
            stmt.setString(1, uuid.toString());
            stmt.setString(2, name);
            stmt.executeUpdate();

            // Time taken.
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug("Database opped " + name + " | Took " + timeTaken + "ms");

            success = true;
        } catch (SQLException e) {
            Log.error("Error opping " + name + " !");
            Log.error(e.getMessage(), e);
            success = false;
        } finally {
            cleanup(stmt);
        }

        return success;
    }

    @Override
    public synchronized boolean addBannedPlayer(UUID uuid, String name, @Nullable String reason) {
        if (!WhitelistSyncCore.CONFIG.syncBannedPlayers) {
            Log.error(LogMessages.ALERT_BANNED_PLAYERS_SYNC_DISABLED);
            return false;
        }

        boolean success;
        PreparedStatement stmt = null;
        try {
            Connection conn = getConnection();
            long startTime = System.currentTimeMillis();

            stmt = conn.prepareStatement("INSERT OR REPLACE INTO bannedPlayers(uuid, name, reason, banned) VALUES (?, ?, ?, 1)");
            stmt.setString(1, uuid.toString());
            stmt.setString(2, name);
            stmt.setString(3, reason);
            stmt.executeUpdate();

            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug("Banned " + name + " | Took " + timeTaken + "ms");

            success = true;
        } catch (SQLException e) {
            Log.error("Error banning " + name + " !");
            Log.error(e.getMessage(), e);
            success = false;
        } finally {
            cleanup(stmt);
        }

        return success;
    }

    @Override
    public synchronized boolean addBannedIp(String ip, @Nullable String reason) {
        if (!WhitelistSyncCore.CONFIG.syncBannedIps) {
            Log.error(LogMessages.ALERT_BANNED_IPS_SYNC_DISABLED);
            return false;
        }

        boolean success;
        PreparedStatement stmt = null;
        try {
            Connection conn = getConnection();
            long startTime = System.currentTimeMillis();

            stmt = conn.prepareStatement("INSERT OR REPLACE INTO bannedIps(ip, reason, banned) VALUES (?, ?, 1)");
            stmt.setString(1, ip);
            stmt.setString(2, reason);
            stmt.executeUpdate();

            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug("Banned " + ip + " | Took " + timeTaken + "ms");

            success = true;
        } catch (SQLException e) {
            Log.error("Error banning " + ip + " !");
            Log.error(e.getMessage(), e);
            success = false;
        } finally {
            cleanup(stmt);
        }

        return success;
    }

    @Override
    public synchronized boolean removeWhitelistPlayer(UUID uuid, String name) {
        boolean success;
        PreparedStatement stmt = null;
        try {
            // Open connection
            Connection conn = getConnection();

            // Start time.
            long startTime = System.currentTimeMillis();

            stmt = conn.prepareStatement("INSERT OR REPLACE INTO whitelist(uuid, name, whitelisted) VALUES (?, ?, 0)");
            stmt.setString(1, uuid.toString());
            stmt.setString(2, name);
            stmt.executeUpdate();

            // Time taken.
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug("Removed " + name + " from whitelist | Took " + timeTaken + "ms");

            success = true;
        } catch (SQLException e) {
            Log.error("Error removing " + name + " to whitelist database!");
            Log.error(e.getMessage(), e);
            success = false;
        } finally {
            cleanup(stmt);
        }

        return success;
    }

    @Override
    public synchronized boolean removeOppedPlayer(UUID uuid, String name) {
        if (!WhitelistSyncCore.CONFIG.syncOpList) {
            Log.error(LogMessages.ALERT_OP_SYNC_DISABLED);
            return false;
        }

        boolean success;
        PreparedStatement stmt = null;
        try {
            // Open connection
            Connection conn = getConnection();

            // Start time.
            long startTime = System.currentTimeMillis();

            stmt = conn.prepareStatement("INSERT OR REPLACE INTO op(uuid, name, isOp) VALUES (?, ?, 0)");
            stmt.setString(1, uuid.toString());
            stmt.setString(2, name);
            stmt.executeUpdate();

            // Time taken
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug("Deopped " + name + " | Took " + timeTaken + "ms");

            success = true;
        } catch (SQLException e) {
            Log.error("Error deopping " + name + ".");
            Log.error(e.getMessage(), e);
            success = false;
        } finally {
            cleanup(stmt);
        }

        return success;
    }

    @Override
    public synchronized boolean removeBannedPlayer(UUID uuid, String name) {
        if (!WhitelistSyncCore.CONFIG.syncBannedPlayers) {
            Log.error(LogMessages.ALERT_BANNED_PLAYERS_SYNC_DISABLED);
            return false;
        }

        boolean success;
        PreparedStatement stmt = null;
        try {
            Connection conn = getConnection();
            long startTime = System.currentTimeMillis();

            stmt = conn.prepareStatement("INSERT OR REPLACE INTO bannedPlayers(uuid, name, reason, banned) VALUES (?, ?, NULL, 0)");
            stmt.setString(1, uuid.toString());
            stmt.setString(2, name);
            stmt.executeUpdate();

            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug("Unbanned " + name + " | Took " + timeTaken + "ms");

            success = true;
        } catch (SQLException e) {
            Log.error("Error unbanning " + name + ".");
            Log.error(e.getMessage(), e);
            success = false;
        } finally {
            cleanup(stmt);
        }

        return success;
    }

    @Override
    public synchronized boolean removeBannedIp(String ip) {
        if (!WhitelistSyncCore.CONFIG.syncBannedIps) {
            Log.error(LogMessages.ALERT_BANNED_IPS_SYNC_DISABLED);
            return false;
        }

        boolean success;
        PreparedStatement stmt = null;
        try {
            Connection conn = getConnection();
            long startTime = System.currentTimeMillis();

            stmt = conn.prepareStatement("INSERT OR REPLACE INTO bannedIps(ip, reason, banned) VALUES (?, NULL, 0)");
            stmt.setString(1, ip);
            stmt.executeUpdate();

            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug("Unbanned " + ip + " | Took " + timeTaken + "ms");

            success = true;
        } catch (SQLException e) {
            Log.error("Error unbanning " + ip + ".");
            Log.error(e.getMessage(), e);
            success = false;
        } finally {
            cleanup(stmt);
        }

        return success;
    }
}
