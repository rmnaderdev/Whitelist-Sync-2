package net.rmnad.services;


import io.reactivex.rxjava3.annotations.Nullable;
import net.rmnad.Log;
import net.rmnad.callbacks.*;
import net.rmnad.config.WhitelistSyncConfig;
import net.rmnad.json.OppedPlayersFileReader;
import net.rmnad.json.WhitelistedPlayersFileReader;
import net.rmnad.logging.LogMessages;
import net.rmnad.models.BannedPlayer;
import net.rmnad.models.OppedPlayer;
import net.rmnad.models.WhitelistedPlayer;
import okio.Path;

import java.sql.*;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Service for SQLITE Databases
 */
public class SqLiteService implements BaseService {

    private final String databasePath;
    private final IServerControl serverControl;
    
    public SqLiteService(IServerControl serverControl) {
        this.databasePath = WhitelistSyncConfig.Config.getSqliteDatabasePath();
        this.serverControl = serverControl;
    }

    public Connection getConnection() throws SQLException {
        String url = "jdbc:sqlite:" + this.databasePath;
        return DriverManager.getConnection(url);
    }

    public void cleanup(Statement stmt, Connection conn) {
        cleanup(null, stmt, conn);
    }

    public void cleanup(ResultSet rs, Statement stmt, Connection conn) {
        try {
            if(rs != null) {
                rs.close();
                rs = null;
            }
        } catch (SQLException ignored){}

        try {
            if(stmt != null) {
                stmt.close();
                stmt = null;
            }
        } catch (SQLException ignored){}

        try {
            if(conn != null) {
                conn.close();
                conn = null;
            }
        } catch (SQLException ignored){}
    }

    // Function used to initialize the database file
    @Override
    public boolean initializeDatabase() {
        Log.info("Setting up the SQLite service...");
        boolean success = true;

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            Log.error("Failed to init org.sqlite.JDBC driver. Is the SQLite library missing? Try downloading it from https://modrinth.com/plugin/sqlite-jdbc and add it to your mods folder.");
            Log.error(e.getMessage(), e);
            success = false;
        }

        if(success) {
            Connection conn = null;
            Statement stmt = null;
            try {
                conn = getConnection();

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

                if (WhitelistSyncConfig.Config.isSyncOpList()) {
                    // SQL statement for creating a new table
                    sql = "CREATE TABLE IF NOT EXISTS op (\n"
                            + "	uuid text NOT NULL PRIMARY KEY,\n"
                            + "	name text NOT NULL,\n"
                            + " isOp integer NOT NULL);";
                    stmt = conn.createStatement();
                    stmt.executeUpdate(sql);

                    // Execute migration
                    // TODO: Handle migration for level and bypassesPlayerLimit in the future
                    //migrateOpList(conn);
                }
            } catch (SQLException e) {
                Log.error("Error creating whitelist or op table!");
                Log.error(e.getMessage(), e);
                success = false;
            } finally {
                cleanup(stmt, conn);
            }
        }

        return success;
    }

    @Override
    public ArrayList<WhitelistedPlayer> getWhitelistedPlayersFromDatabase() {
        // ArrayList for whitelisted players.
        ArrayList<WhitelistedPlayer> whitelistedPlayers = new ArrayList<>();

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            // Keep track of records.
            int records = 0;

            // Connect to database.
            conn = getConnection();
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
            cleanup(rs, stmt, conn);
        }

        return whitelistedPlayers;
    }

    @Override
    public ArrayList<OppedPlayer> getOppedPlayersFromDatabase() {
        // ArrayList for opped players.
        ArrayList<OppedPlayer> oppedPlayers = new ArrayList<>();

        if (!WhitelistSyncConfig.Config.isSyncOpList()) {
            Log.error(LogMessages.ALERT_OP_SYNC_DISABLED);
            return oppedPlayers;
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            // Keep track of records.
            int records = 0;

            // Connect to database.
            conn = getConnection();
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
            cleanup(rs, stmt, conn);
        }

        return oppedPlayers;
    }

    @Override
    public ArrayList<BannedPlayer> getBannedPlayersFromDatabase() {
        Log.error(LogMessages.ALERT_BANNED_PLAYERS_SYNC_DISABLED);
        return new ArrayList<>();
    }

    @Override
    public ArrayList<String> getBannedIpsFromDatabase() {
        Log.error(LogMessages.ALERT_BANNED_IPS_SYNC_DISABLED);
        return new ArrayList<>();
    }

    @Override
    public boolean pushLocalWhitelistToDatabase() {
        // TODO: Start job on thread to avoid lag?
        // Keep track of records.
        int records = 0;
        boolean success;
        long startTime = System.currentTimeMillis();

        ArrayList<WhitelistedPlayer> whitelistedPlayers = WhitelistedPlayersFileReader.getWhitelistedPlayers();

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            // Connect to database.
            conn = getConnection();
            // Loop through local whitelist and insert into database.
            for (WhitelistedPlayer player : whitelistedPlayers) {

                if (player.getUuid() != null && player.getName() != null) {
                    stmt = conn.prepareStatement("INSERT OR REPLACE INTO whitelist(uuid, name, whitelisted) VALUES (?, ?, 1)");
                    stmt.setString(1, player.getUuid());
                    stmt.setString(2, player.getName());
                    stmt.executeUpdate();
                    stmt.close();

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
            cleanup(stmt, conn);
        }

        return success;
    }

    @Override
    public boolean pushLocalOpsToDatabase() {
        if (!WhitelistSyncConfig.Config.isSyncOpList()) {
            Log.error(LogMessages.ALERT_OP_SYNC_DISABLED);
            return false;
        }

        // TODO: Start job on thread to avoid lag?
        // Keep track of records.
        int records = 0;
        boolean success;
        long startTime = System.currentTimeMillis();

        ArrayList<OppedPlayer> oppedPlayers = OppedPlayersFileReader.getOppedPlayers();

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            // Connect to database.
            conn = getConnection();
            // Loop through local opped players and insert into database.
            for (OppedPlayer player : oppedPlayers) {

                if (player.getUuid() != null && player.getName() != null) {
                    stmt = conn.prepareStatement("INSERT OR REPLACE INTO op(uuid, name, isOp) VALUES (?, ?, 1)");
                    stmt.setString(1, player.getUuid());
                    stmt.setString(2, player.getName());
                    stmt.executeUpdate();
                    stmt.close();

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
            cleanup(stmt, conn);
        }

        return success;
    }

    @Override
    public boolean pushLocalBannedPlayersToDatabase() {
        Log.error(LogMessages.ALERT_BANNED_PLAYERS_SYNC_DISABLED);
        return false;
    }

    @Override
    public boolean pushLocalBannedIpsToDatabase() {
        Log.error(LogMessages.ALERT_BANNED_IPS_SYNC_DISABLED);
        return false;
    }

    @Override
    public boolean pullDatabaseWhitelistToLocal() {
        int records = 0;
        boolean success;
        long startTime = System.currentTimeMillis();

        ArrayList<WhitelistedPlayer> localWhitelistedPlayers = WhitelistedPlayersFileReader.getWhitelistedPlayers();

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = getConnection();

            String sql = "SELECT name, uuid, whitelisted FROM whitelist;";
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String name = rs.getString("name");
                int whitelisted = rs.getInt("whitelisted");

                if (whitelisted == 1) {
                    if (localWhitelistedPlayers.stream().noneMatch(o -> o.getUuid().equals(uuid.toString()))) {
                        try {
                            this.serverControl.addWhitelistPlayer(uuid, name);
                            Log.debug(LogMessages.AddedUserToWhitelist(name));
                            records++;
                        } catch (NullPointerException e) {
                            Log.error(e.getMessage(), e);
                        }
                    }
                } else {
                    if (localWhitelistedPlayers.stream().anyMatch(o -> o.getUuid().equals(uuid.toString()))) {
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
            cleanup(rs, stmt, conn);
        }

        return success;
    }

    @Override
    public boolean pullDatabaseOpsToLocal() {

        // TODO: Compare level and bypassesPlayerLimit, sync if needed
        if (!WhitelistSyncConfig.Config.isSyncOpList()) {
            Log.error(LogMessages.ALERT_OP_SYNC_DISABLED);
            return false;
        }

        int records = 0;
        boolean success;

        long startTime = System.currentTimeMillis();

        ArrayList<OppedPlayer> localOppedPlayers = OppedPlayersFileReader.getOppedPlayers();

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();

            String sql = "SELECT uuid, name, isOp FROM op;";
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String name = rs.getString("name");
                int opped = rs.getInt("isOp");

                if (opped == 1) {
                    if (localOppedPlayers.stream().noneMatch(o -> o.getUuid().equals(uuid.toString()))) {
                        try {
                            this.serverControl.addOpPlayer(uuid, name);
                            Log.debug(LogMessages.OppedUser(name));
                            records++;
                        } catch (NullPointerException e) {
                            Log.error(e.getMessage(), e);
                        }
                    }
                } else {
                    if (localOppedPlayers.stream().anyMatch(o -> o.getUuid().equals(uuid.toString()))) {
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
            cleanup(rs, stmt, conn);
        }

        return success;
    }

    @Override
    public boolean pullDatabaseBannedPlayersToLocal() {
        Log.error(LogMessages.ALERT_BANNED_PLAYERS_SYNC_DISABLED);
        return false;
    }

    @Override
    public boolean pullDatabaseBannedIpsToLocal() {
        Log.error(LogMessages.ALERT_BANNED_IPS_SYNC_DISABLED);
        return false;
    }

    @Override
    public boolean addWhitelistPlayer(UUID uuid, String name) {
        boolean success;
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            // Open connection
            conn = getConnection();

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
            cleanup(stmt, conn);
        }

        return success;
    }

    @Override
    public boolean addOppedPlayer(UUID uuid, String name) {
        if (!WhitelistSyncConfig.Config.isSyncOpList()) {
            Log.error(LogMessages.ALERT_OP_SYNC_DISABLED);
            return false;
        }

        boolean success;
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            // Open connection
            conn = getConnection();

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
            cleanup(stmt, conn);
        }

        return success;
    }

    @Override
    public boolean addBannedPlayer(UUID uuid, String name, @Nullable String reason) {
        Log.error(LogMessages.ALERT_BANNED_PLAYERS_SYNC_DISABLED);
        return false;
    }

    @Override
    public boolean addBannedIp(String ip, @Nullable String reason) {
        Log.error(LogMessages.ALERT_BANNED_IPS_SYNC_DISABLED);
        return false;
    }

    @Override
    public boolean removeWhitelistPlayer(UUID uuid, String name) {
        boolean success;
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            // Open connection
            conn = getConnection();

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
            cleanup(stmt, conn);
        }

        return success;
    }

    @Override
    public boolean removeOppedPlayer(UUID uuid, String name) {
        if (!WhitelistSyncConfig.Config.isSyncOpList()) {
            Log.error(LogMessages.ALERT_OP_SYNC_DISABLED);
            return false;
        }

        boolean success;
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            // Open connection
            conn = getConnection();

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
            cleanup(stmt, conn);
        }

        return success;
    }

    @Override
    public boolean removeBannedPlayer(UUID uuid, String name) {
        Log.error(LogMessages.ALERT_BANNED_PLAYERS_SYNC_DISABLED);
        return false;
    }

    @Override
    public boolean removeBannedIp(String ip) {
        Log.error(LogMessages.ALERT_BANNED_IPS_SYNC_DISABLED);
        return false;
    }

    // TODO: Handle migration for level and bypassesPlayerLimit in the future
//    private static void migrateOpList(Connection conn) throws SQLException {
//        String sql;
//        PreparedStatement stmt;
//
//        // Add new level field to op table if it doesn't exist
//        sql = "SELECT COUNT(*) AS count FROM pragma_table_info('op') WHERE name='level'";
//        stmt = conn.prepareStatement(sql);
//        ResultSet rs = stmt.executeQuery();
//        stmt.close();
//        rs.next();
//
//        if(rs.getInt("count") == 0) {
//            sql = "ALTER TABLE op ADD COLUMN level INTEGER NULL DEFAULT 4";
//            stmt = conn.prepareStatement(sql);
//            stmt.execute();
//            stmt.close();
//            Log.info("Added new op table \"level\" column. Existing entries get set to default level 4.");
//        }
//        rs.close();
//
//
//        // Add new bypassesPlayerLimit field to op table if it doesn't exist
//        sql = "SELECT COUNT(*) AS count FROM pragma_table_info('op') WHERE name='bypassesPlayerLimit'";
//        stmt = conn.prepareStatement(sql);
//        ResultSet rs1 = stmt.executeQuery();
//        stmt.close();
//        rs1.next();
//
//        if(rs1.getInt("count") == 0) {
//            sql = "ALTER TABLE op ADD COLUMN bypassesPlayerLimit INTEGER NULL DEFAULT 0";
//            stmt = conn.prepareStatement(sql);
//            stmt.execute();
//            stmt.close();
//            Log.info("Added new op table \"bypassesPlayerLimit\" column. Existing entries get set to default bypassesPlayerLimit false.");
//        }
//        rs1.close();
//    }
}
