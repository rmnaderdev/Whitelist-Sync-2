package net.rmnad.core.services;

import io.reactivex.rxjava3.annotations.Nullable;
import net.rmnad.core.Log;
import net.rmnad.core.WhitelistSyncCore;
import net.rmnad.core.callbacks.IServerControl;
import net.rmnad.core.json.OppedPlayersFileReader;
import net.rmnad.core.json.WhitelistedPlayersFileReader;
import net.rmnad.core.logging.LogMessages;
import net.rmnad.core.models.BannedPlayer;
import net.rmnad.core.models.OppedPlayer;
import net.rmnad.core.models.WhitelistedPlayer;

import java.sql.*;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Service for MYSQL Databases
 */
public class MySqlService implements BaseService {

    private final String databaseName;
    private final String url;
    private final String username;
    private final String password;

    private final IServerControl serverControl;

    public MySqlService(IServerControl serverControl) {

        String ip = WhitelistSyncCore.CONFIG.mysqlIp;
        int port = WhitelistSyncCore.CONFIG.mysqlPort;
        this.databaseName = WhitelistSyncCore.CONFIG.mysqlDbName;
        this.url = "jdbc:mysql://" + ip + ":" + port + "/?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC";
        this.username = WhitelistSyncCore.CONFIG.mysqlUsername;
        this.password = WhitelistSyncCore.CONFIG.mysqlPassword;

        this.serverControl = serverControl;
    }

    // Function used to initialize the database file
    @Override
    public boolean initializeDatabase() {
        Log.info("Setting up the MySQL service...");
        boolean isSuccess = true;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            Log.error("Failed to init com.mysql.cj.jdbc.Driver. Is the MySQL library missing? Try downloading it from https://modrinth.com/plugin/mysql-jdbc and add it to your mods folder.");
            Log.error(e.getMessage(), e);
            isSuccess = false;
        }


        if (isSuccess) {
            try {
                Connection conn = DriverManager.getConnection(url, username, password);
                Log.info("Connected to " + url + " successfully!");
                conn.close();
            } catch (SQLException e) {
                Log.error("Failed to connect to the mySQL database! Did you set one up in the config?");
                Log.error(e.getMessage(), e);
                isSuccess = false;
            }
        }

        if (isSuccess) {
            // Create database
            try {
                PreparedStatement stmt;


                // Create database
                String sql = "CREATE DATABASE IF NOT EXISTS `" + databaseName + "`;";

                // Create statement
                Connection conn = DriverManager.getConnection(url, username, password);
                stmt = conn.prepareStatement(sql);
                stmt.execute();
                stmt.close();

                // Create whitelist table
                sql = "CREATE TABLE IF NOT EXISTS `" + databaseName + "`.`whitelist` ("
                        + "`uuid` VARCHAR(60) NOT NULL,"
                        + "`name` VARCHAR(20) NOT NULL,"
                        + "`whitelisted` TINYINT NOT NULL DEFAULT 1,"
                        + "PRIMARY KEY (`uuid`)"
                        + ")";
                stmt = conn.prepareStatement(sql);
                stmt.execute();
                stmt.close();

                // Create opped players table if enabled
                if (WhitelistSyncCore.CONFIG.syncOpList) {
                    sql = "CREATE TABLE IF NOT EXISTS `" + databaseName + "`.`op` ("
                            + "`uuid` VARCHAR(60) NOT NULL,"
                            + "`name` VARCHAR(20) NOT NULL,"
                            + "`isOp` TINYINT NOT NULL DEFAULT 1,"
                            + "PRIMARY KEY (`uuid`)"
                            + ")";
                    stmt = conn.prepareStatement(sql);
                    stmt.execute();
                    stmt.close();


                    // Execute migration
                    // TODO: Handle migration for level and bypassesPlayerLimit in the future
                    //migrateOpList(conn, databaseName);
                }

                Log.info("Setup MySQL database!");
                conn.close();
            } catch (Exception e) {
                Log.error("Error initializing database and database tables.");
                Log.error(e.getMessage(), e);
                isSuccess = false;
            }
        }


        return isSuccess;
    }

    @Override
    public ArrayList<WhitelistedPlayer> getWhitelistedPlayersFromDatabase() {
        // ArrayList for whitelisted players.
        ArrayList<WhitelistedPlayer> whitelistedPlayers = new ArrayList<>();

        try {
            // Keep track of records.
            int records = 0;

            // Connect to database.
            Connection conn = DriverManager.getConnection(url, username, password);
            long startTime = System.currentTimeMillis();

            String sql = "SELECT uuid, name FROM `" + databaseName + "`.`whitelist` WHERE whitelisted = true;";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            // Add queried results to arraylist.
            while (rs.next()) {
                whitelistedPlayers.add(new WhitelistedPlayer(rs.getString("uuid"), rs.getString("name")));
                records++;
            }

            // Time taken
            long timeTaken = System.currentTimeMillis() - startTime;

            Log.debug(LogMessages.SuccessGetWhitelistedPlayersFromDatabase(timeTaken, records));

            rs.close();
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            // Something is wrong...
            Log.error("Error querying whitelisted players from database!");
            Log.error(e.getMessage(), e);
        }
        return whitelistedPlayers;
    }

    @Override
    public ArrayList<OppedPlayer> getOppedPlayersFromDatabase() {
        // ArrayList for opped players.
        ArrayList<OppedPlayer> oppedPlayers = new ArrayList<>();

        if (!WhitelistSyncCore.CONFIG.syncOpList) {
            Log.error(LogMessages.ALERT_OP_SYNC_DISABLED);
            return oppedPlayers;
        }

        try {
            // Keep track of records.
            int records = 0;

            // Connect to database.
            Connection conn = DriverManager.getConnection(url, username, password);
            long startTime = System.currentTimeMillis();

            String sql = "SELECT uuid, name FROM `" + databaseName + "`.`op` WHERE isOp = true;";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

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

            rs.close();
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            Log.error("Error querying opped players from database!");
            Log.error(e.getMessage(), e);
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
        long startTime = System.currentTimeMillis();

        ArrayList<WhitelistedPlayer> whitelistedPlayers
                = WhitelistedPlayersFileReader.getWhitelistedPlayers();

        try {
            // Connect to database.
            Connection conn = DriverManager.getConnection(url, username, password);
            // Loop through local whitelist and insert into database.
            for (WhitelistedPlayer player : whitelistedPlayers) {

                if (player.getUuid() != null && player.getName() != null) {
                    PreparedStatement stmt = conn.prepareStatement("INSERT IGNORE INTO `" + databaseName + "`.`whitelist`(uuid, name, whitelisted) VALUES (?, ?, true)");
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
            conn.close();

            return true;
        } catch (SQLException e) {
            Log.error(LogMessages.ERROR_PushLocalWhitelistToDatabase, e);
        }

        return false;
    }

    @Override
    public boolean pushLocalOpsToDatabase() {
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

        try {
            // Connect to database.
            Connection conn = DriverManager.getConnection(url, username, password);
            // Loop through local whitelist and insert into database.
            for (OppedPlayer player : oppedPlayers) {

                if (player.getUuid() != null && player.getName() != null) {
                    PreparedStatement stmt = conn.prepareStatement("INSERT IGNORE INTO `" + databaseName + "`.`op`(uuid, name, isOp) VALUES (?, ?, true)");
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
            conn.close();

            return true;
        } catch (SQLException e) {
            Log.error(LogMessages.ERROR_PushLocalOpsToDatabase, e);
        }

        return false;
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
        try {
            int records = 0;
            long startTime = System.currentTimeMillis();

            ArrayList<WhitelistedPlayer> localWhitelistedPlayers
                    = WhitelistedPlayersFileReader.getWhitelistedPlayers();

            // Open connection
            Connection conn = DriverManager.getConnection(url, username, password);

            String sql = "SELECT name, uuid, whitelisted FROM `" + databaseName + "`.`whitelist`";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String name = rs.getString("name");
                int whitelisted = rs.getInt("whitelisted");

                if (whitelisted == 1) {
                    if (localWhitelistedPlayers.stream().noneMatch(o -> o.getUuid().equals(uuid.toString()))) {
                        try {
                            serverControl.addWhitelistPlayer(uuid, name);
                            Log.debug(LogMessages.AddedUserToWhitelist(name));
                            records++;
                        } catch (NullPointerException e) {
                            Log.error(e.getMessage(), e);
                        }
                    }
                } else {
                    if (localWhitelistedPlayers.stream().anyMatch(o -> o.getUuid().equals(uuid.toString()))) {
                        serverControl.removeWhitelistPlayer(uuid, name);
                        Log.debug(LogMessages.RemovedUserToWhitelist(name));
                        records++;
                    }
                }

            }
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug(LogMessages.SuccessPullDatabaseWhitelistToLocal( timeTaken, records));

            rs.close();
            stmt.close();
            conn.close();
            return true;
        } catch (SQLException e) {
            Log.error(LogMessages.ERROR_PullDatabaseWhitelistToLocal, e);
        }

        return false;
    }

    @Override
    public boolean pullDatabaseOpsToLocal() {
        if (!WhitelistSyncCore.CONFIG.syncOpList) {
            Log.error(LogMessages.ALERT_OP_SYNC_DISABLED);
            return false;
        }

        // TODO: Compare level and bypassesPlayerLimit, sync if needed
        try {
            int records = 0;
            long startTime = System.currentTimeMillis();

            ArrayList<OppedPlayer> localOppedPlayers
                    = OppedPlayersFileReader.getOppedPlayers();

            // Open connection
            Connection conn = DriverManager.getConnection(url, username, password);

            String sql = "SELECT uuid, name, isOp FROM `" + databaseName + "`.`op`";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String name = rs.getString("name");
                int opped = rs.getInt("isOp");

                if (opped == 1) {
                    if (localOppedPlayers.stream().noneMatch(o -> o.getUuid().equals(uuid.toString()))) {
                        try {
                            serverControl.addOpPlayer(uuid, name);
                            Log.debug(LogMessages.OppedUser(name));
                            records++;
                        } catch (NullPointerException e) {
                            Log.error(e.getMessage(), e);
                        }
                    }
                } else {
                    if (localOppedPlayers.stream().anyMatch(o -> o.getUuid().equals(uuid.toString()))) {
                        serverControl.removeOpPlayer(uuid, name);
                        Log.debug(LogMessages.DeopUser(name));
                        records++;
                    }
                }

            }
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug(LogMessages.SuccessPullDatabaseOpsToLocal(timeTaken, records));

            rs.close();
            stmt.close();
            conn.close();
            return true;
        } catch (SQLException e) {
            Log.error("Error querying opped players from database!");
            Log.error(e.getMessage(), e);
        }

        return false;
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
        long startTime = System.currentTimeMillis();

        try {
            // Open connection=
            Connection conn = DriverManager.getConnection(url, username, password);

            String sql = "REPLACE INTO `" + databaseName + "`.`whitelist`(uuid, name, whitelisted) VALUES (?, ?, true)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, uuid.toString());
            stmt.setString(2, name);
            stmt.executeUpdate();

            // Time taken.
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug("Added " + name + " to whitelist | Took " + timeTaken + "ms");
            stmt.close();
            conn.close();
            return true;

        } catch (SQLException e) {
            Log.error("Error adding " + name + " to whitelist database!");
            Log.error(e.getMessage(), e);
        }

        return false;
    }

    @Override
    public boolean addOppedPlayer(UUID uuid, String name) {
        if (!WhitelistSyncCore.CONFIG.syncOpList) {
            Log.error(LogMessages.ALERT_OP_SYNC_DISABLED);
            return false;
        }

        long startTime = System.currentTimeMillis();
        try {
            // Open connection=
            Connection conn = DriverManager.getConnection(url, username, password);

            String sql = "REPLACE INTO `" + databaseName + "`.`op`(uuid, name, isOp) VALUES (?, ?, true)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, uuid.toString());
            stmt.setString(2, name);
            stmt.executeUpdate();

            // Time taken.
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug("Database opped " + name + " | Took " + timeTaken + "ms");
            stmt.close();
            conn.close();
            return true;

        } catch (SQLException e) {
            Log.error("Error opping " + name + " !");
            Log.error(e.getMessage(), e);
        }

        return false;
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
        long startTime = System.currentTimeMillis();

        try {
            // Open connection=
            Connection conn = DriverManager.getConnection(url, username, password);

            String sql = "REPLACE INTO `" + databaseName + "`.`whitelist`(uuid, name, whitelisted) VALUES (?, ?, false)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, uuid.toString());
            stmt.setString(2, name);
            stmt.executeUpdate();

            // Time taken.
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug("Removed " + name + " from whitelist | Took " + timeTaken + "ms");
            stmt.close();
            conn.close();
            return true;

        } catch (SQLException e) {
            Log.error("Error removing " + name + " from whitelist database!");
            Log.error(e.getMessage(), e);
        }

        return false;
    }

    @Override
    public boolean removeOppedPlayer(UUID uuid, String name) {
        if (!WhitelistSyncCore.CONFIG.syncOpList) {
            Log.error(LogMessages.ALERT_OP_SYNC_DISABLED);
            return false;
        }

        long startTime = System.currentTimeMillis();

        try {
            // Open connection=
            Connection conn = DriverManager.getConnection(url, username, password);

            String sql = "REPLACE INTO `" + databaseName + "`.`op`(uuid, name, isOp) VALUES (?, ?, false)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, uuid.toString());
            stmt.setString(2, name);
            stmt.executeUpdate();

            // Time taken.
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug("Deopped " + name + " | Took " + timeTaken + "ms");
            stmt.close();
            conn.close();
            return true;

        } catch (SQLException e) {
            Log.error("Error deopping " + name + ".");
            Log.error(e.getMessage(), e);
        }

        return false;
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
//    private static void migrateOpList(Connection conn, String databaseName) throws SQLException {
//        String sql;
//        PreparedStatement stmt;
//
//        // Add new level field to op table if it doesn't exist
//        sql = "SELECT COUNT(*) AS count " +
//                "FROM INFORMATION_SCHEMA.COLUMNS " +
//                "WHERE TABLE_SCHEMA = '" + databaseName + "' AND TABLE_NAME = 'op' AND COLUMN_NAME = 'level'";
//        stmt = conn.prepareStatement(sql);
//        ResultSet rs = stmt.executeQuery();
//        rs.next();
//
//        if(rs.getInt("count") == 0) {
//            sql = "ALTER TABLE " + databaseName + ".op ADD COLUMN level INTEGER NOT NULL DEFAULT 4";
//            PreparedStatement stmt2 = conn.prepareStatement(sql);
//            stmt2.execute();
//            stmt2.close();
//            Log.info("Added new op table \"level\" column. Existing entries get set to default level 4.");
//        }
//        rs.close();
//        stmt.close();
//
//
//        // Add new bypassesPlayerLimit field to op table if it doesn't exist
//        sql =
//                "SELECT COUNT(*) AS count " +
//                        "FROM INFORMATION_SCHEMA.COLUMNS " +
//                        "WHERE TABLE_SCHEMA = '" + databaseName + "' AND TABLE_NAME = 'op' AND COLUMN_NAME = 'bypassesPlayerLimit'";
//        stmt = conn.prepareStatement(sql);
//        ResultSet rs1 = stmt.executeQuery();
//        rs1.next();
//
//        if(rs1.getInt("count") == 0) {
//            sql = "ALTER TABLE " + databaseName + ".op ADD COLUMN bypassesPlayerLimit TINYINT NOT NULL DEFAULT 0";
//            PreparedStatement stmt2 = conn.prepareStatement(sql);
//            stmt2.execute();
//            stmt2.close();
//            Log.info("Added new op table \"bypassesPlayerLimit\" column. Existing entries get set to default bypassesPlayerLimit false.");
//        }
//        rs1.close();
//        stmt.close();
//    }
}
