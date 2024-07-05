package net.rmnad.whitelistsync2.services;

import net.rmnad.whitelistsync2.WhitelistSyncCore;
import net.rmnad.whitelistsync2.callbacks.IOnUserAdd;
import net.rmnad.whitelistsync2.callbacks.IOnUserRemove;
import net.rmnad.whitelistsync2.models.OppedPlayer;
import net.rmnad.whitelistsync2.models.WhitelistedPlayer;

import java.sql.*;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Service for MYSQL Databases
 */
public class MySqlService implements BaseService {

    private final boolean syncingOpList;

    private final String databaseName;
    private final String url;
    private final String username;
    private final String password;

    public MySqlService(String databaseName, String ip, int port, String username, String password, boolean syncingOpList) {
        this.databaseName = databaseName;
        this.url = "jdbc:mysql://" + ip + ":" + port + "/?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC";
        this.username = username;
        this.password = password;
        
        this.syncingOpList = syncingOpList;
    }

    @Override
    public boolean requiresSyncing() {
        return true;
    }

    // Function used to initialize the database file
    @Override
    public boolean initializeDatabase() {
        WhitelistSyncCore.LOGGER.info("Setting up the MySQL service...");
        boolean isSuccess = true;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            WhitelistSyncCore.LOGGER.error("Failed to init mysql-connector. Is the library missing?");
            WhitelistSyncCore.LOGGER.error(e.getMessage(), e);
            isSuccess = false;
        }


        if (isSuccess) {
            try {
                Connection conn = DriverManager.getConnection(url, username, password);
                WhitelistSyncCore.LOGGER.debug("Connected to " + url + " successfully!");
                conn.close();
            } catch (SQLException e) {
                WhitelistSyncCore.LOGGER.error("Failed to connect to the mySQL database! Did you set one up in the config?");
                WhitelistSyncCore.LOGGER.error(e.getMessage(), e);
                isSuccess = false;
            }
        }

        if (isSuccess) {
            // Create database
            try {
                PreparedStatement stmt;


                // Create database
                String sql = "CREATE DATABASE IF NOT EXISTS " + databaseName + ";";

                // Create statement
                Connection conn = DriverManager.getConnection(url, username, password);
                stmt = conn.prepareStatement(sql);
                stmt.execute();
                stmt.close();

                // Create whitelist table
                sql = "CREATE TABLE IF NOT EXISTS " + databaseName + ".whitelist ("
                        + "`uuid` VARCHAR(60) NOT NULL,"
                        + "`name` VARCHAR(20) NOT NULL,"
                        + "`whitelisted` TINYINT NOT NULL DEFAULT 1,"
                        + "PRIMARY KEY (`uuid`)"
                        + ")";
                stmt = conn.prepareStatement(sql);
                stmt.execute();
                stmt.close();

                // Create opped players table if enabled
                if (this.syncingOpList) {
                    sql = "CREATE TABLE IF NOT EXISTS " + databaseName + ".op ("
                            + "`uuid` VARCHAR(60) NOT NULL,"
                            + "`name` VARCHAR(20) NOT NULL,"
                            + "`level` INTEGER NOT NULL,"
                            + "`bypassesPlayerLimit` TINYINT NOT NULL,"
                            + "`isOp` TINYINT NOT NULL DEFAULT 1,"
                            + "PRIMARY KEY (`uuid`)"
                            + ")";
                    stmt = conn.prepareStatement(sql);
                    stmt.execute();
                    stmt.close();


                    // Execute migration
                    migrateOpList(conn, databaseName);
                }

                WhitelistSyncCore.LOGGER.info("Setup MySQL database!");
                conn.close();
            } catch (Exception e) {
                WhitelistSyncCore.LOGGER.error("Error initializing database and database tables.");
                WhitelistSyncCore.LOGGER.error(e.getMessage(), e);
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

            String sql = "SELECT uuid, name FROM " + databaseName + ".whitelist WHERE whitelisted = true;";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            // Add queried results to arraylist.
            while (rs.next()) {
                whitelistedPlayers.add(new WhitelistedPlayer(rs.getString("uuid"), rs.getString("name"), true));
                records++;
            }

            // Time taken
            long timeTaken = System.currentTimeMillis() - startTime;

            WhitelistSyncCore.LOGGER.debug("Database pulled whitelisted players | Took " + timeTaken + "ms | Read " + records + " records.");

            rs.close();
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            // Something is wrong...
            WhitelistSyncCore.LOGGER.error("Error querying whitelisted players from database!");
            WhitelistSyncCore.LOGGER.error(e.getMessage(), e);
        }
        return whitelistedPlayers;
    }

    @Override
    public ArrayList<OppedPlayer> getOppedPlayersFromDatabase() {
        // ArrayList for opped players.
        ArrayList<OppedPlayer> oppedPlayers = new ArrayList<>();

        if (this.syncingOpList) {
            try {
                // Keep track of records.
                int records = 0;

                // Connect to database.
                Connection conn = DriverManager.getConnection(url, username, password);
                long startTime = System.currentTimeMillis();

                String sql = "SELECT uuid, name, level, bypassesPlayerLimit FROM " + databaseName + ".op WHERE isOp = true;";
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();

                // Add queried results to arraylist.
                while (rs.next()) {
                    OppedPlayer oppedPlayer = new OppedPlayer();
                    oppedPlayer.setIsOp(true);
                    oppedPlayer.setUuid(rs.getString("uuid"));
                    oppedPlayer.setName(rs.getString("name"));
                    oppedPlayer.setLevel(rs.getInt("level"));
                    oppedPlayer.setBypassesPlayerLimit(rs.getBoolean("bypassesPlayerLimit"));

                    oppedPlayers.add(oppedPlayer);
                    records++;
                }

                // Time taken
                long timeTaken = System.currentTimeMillis() - startTime;

                WhitelistSyncCore.LOGGER.debug("Database pulled opped players | Took " + timeTaken + "ms | Read " + records + " records.");

                rs.close();
                stmt.close();
                conn.close();
            } catch (SQLException e) {
                WhitelistSyncCore.LOGGER.error("Error querying opped players from database!");
                WhitelistSyncCore.LOGGER.error(e.getMessage(), e);
            }
        } else {
            WhitelistSyncCore.LOGGER.error("Op list syncing is currently disabled in your config. "
                    + "Please enable it and restart the server to use this feature.");
        }

        return oppedPlayers;
    }

    @Override
    public boolean copyLocalWhitelistedPlayersToDatabase(ArrayList<WhitelistedPlayer> whitelistedPlayers) {
        // TODO: Start job on thread to avoid lag?
        // Keep track of records.
        int records = 0;
        try {
            // Connect to database.
            Connection conn = DriverManager.getConnection(url, username, password);
            long startTime = System.currentTimeMillis();
            // Loop through local whitelist and insert into database.
            for (WhitelistedPlayer player : whitelistedPlayers) {

                if (player.getUuid() != null && player.getName() != null) {
                    PreparedStatement stmt = conn.prepareStatement("INSERT IGNORE INTO " + databaseName + ".whitelist(uuid, name, whitelisted) VALUES (?, ?, true)");
                    stmt.setString(1, player.getUuid());
                    stmt.setString(2, player.getName());
                    stmt.executeUpdate();
                    stmt.close();

                    records++;
                }
            }
            // Record time taken.
            long timeTaken = System.currentTimeMillis() - startTime;
            WhitelistSyncCore.LOGGER.debug("Whitelist table updated | Took " + timeTaken + "ms | Wrote " + records + " records.");
            conn.close();

            return true;
        } catch (SQLException e) {
            WhitelistSyncCore.LOGGER.error("Failed to update database with local records.");
            WhitelistSyncCore.LOGGER.error(e.getMessage(), e);
        }

        return false;
    }

    @Override
    public boolean copyLocalOppedPlayersToDatabase(ArrayList<OppedPlayer> oppedPlayers) {
        if (this.syncingOpList) {
            // TODO: Start job on thread to avoid lag?
            // Keep track of records.
            int records = 0;
            try {
                // Connect to database.
                Connection conn = DriverManager.getConnection(url, username, password);
                long startTime = System.currentTimeMillis();
                // Loop through local whitelist and insert into database.
                for (OppedPlayer player : oppedPlayers) {

                    if (player.getUuid() != null && player.getName() != null) {
                        PreparedStatement stmt = conn.prepareStatement("INSERT IGNORE INTO " + databaseName + ".op(uuid, name, isOp) VALUES (?, ?, true)");
                        stmt.setString(1, player.getUuid());
                        stmt.setString(2, player.getName());
                        stmt.executeUpdate();
                        stmt.close();

                        records++;
                    }
                }
                // Record time taken.
                long timeTaken = System.currentTimeMillis() - startTime;
                WhitelistSyncCore.LOGGER.debug("Op table updated | Took " + timeTaken + "ms | Wrote " + records + " records.");
                conn.close();

                return true;
            } catch (SQLException e) {
                WhitelistSyncCore.LOGGER.error("Failed to update database with local records.");
                WhitelistSyncCore.LOGGER.error(e.getMessage(), e);
            }
        } else {
            WhitelistSyncCore.LOGGER.error("Op list syncing is currently disabled in your config. "
                    + "Please enable it and restart the server to use this feature.");
        }

        return false;
    }

    @Override
    public boolean copyDatabaseWhitelistedPlayersToLocal(ArrayList<WhitelistedPlayer> localWhitelistedPlayers, IOnUserAdd onUserAdd, IOnUserRemove onUserRemove) {
        try {
            int records = 0;

            // Open connection
            Connection conn = DriverManager.getConnection(url, username, password);
            long startTime = System.currentTimeMillis();

            String sql = "SELECT name, uuid, whitelisted FROM " + databaseName + ".whitelist";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String name = rs.getString("name");
                int whitelisted = rs.getInt("whitelisted");

                if (whitelisted == 1) {
                    if (localWhitelistedPlayers.stream().noneMatch(o -> o.getUuid().equals(uuid.toString()))) {
                        try {
                            onUserAdd.call(uuid, name);
                            WhitelistSyncCore.LOGGER.debug("Added " + name + " to whitelist.");
                            records++;
                        } catch (NullPointerException e) {
                            WhitelistSyncCore.LOGGER.error("Player is null?");
                            WhitelistSyncCore.LOGGER.error(e.getMessage(), e);
                        }
                    }
                } else {
                    if (localWhitelistedPlayers.stream().anyMatch(o -> o.getUuid().equals(uuid.toString()))) {
                        onUserRemove.call(uuid, name);
                        WhitelistSyncCore.LOGGER.debug("Removed " + name + " from whitelist.");
                        records++;
                    }
                }

            }
            long timeTaken = System.currentTimeMillis() - startTime;
            WhitelistSyncCore.LOGGER.debug("Copied whitelist database to local | Took " + timeTaken + "ms | Wrote " + records + " records.");

            rs.close();
            stmt.close();
            conn.close();
            return true;
        } catch (SQLException e) {
            WhitelistSyncCore.LOGGER.error("Error querying whitelisted players from database!");
            WhitelistSyncCore.LOGGER.error(e.getMessage(), e);
        }

        return false;
    }

    @Override
    public boolean copyDatabaseOppedPlayersToLocal(ArrayList<OppedPlayer> localOppedPlayers, IOnUserAdd onUserAdd, IOnUserRemove onUserRemove) {
        if (this.syncingOpList) {

            try {
                int records = 0;

                // Open connection
                Connection conn = DriverManager.getConnection(url, username, password);
                long startTime = System.currentTimeMillis();

                String sql = "SELECT uuid, name, level, bypassesPlayerLimit, isOp FROM " + databaseName + ".op";
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String name = rs.getString("name");
                    int opped = rs.getInt("isOp");

                    if (opped == 1) {
                        if (localOppedPlayers.stream().noneMatch(o -> o.getUuid().equals(uuid.toString()))) {
                            try {
                                onUserAdd.call(uuid, name);
                                WhitelistSyncCore.LOGGER.debug("Opped " + name + ".");
                                records++;
                            } catch (NullPointerException e) {
                                WhitelistSyncCore.LOGGER.error("Player is null?");
                                WhitelistSyncCore.LOGGER.error(e.getMessage(), e);
                            }
                        }
                    } else {
                        if (localOppedPlayers.stream().anyMatch(o -> o.getUuid().equals(uuid.toString()))) {
                            onUserRemove.call(uuid, name);
                            WhitelistSyncCore.LOGGER.debug("Deopped " + name + ".");
                            records++;
                        }
                    }

                }
                long timeTaken = System.currentTimeMillis() - startTime;
                WhitelistSyncCore.LOGGER.debug("Copied op database to local | Took " + timeTaken + "ms | Wrote " + records + " records.");

                rs.close();
                stmt.close();
                conn.close();
                return true;
            } catch (SQLException e) {
                WhitelistSyncCore.LOGGER.error("Error querying opped players from database!");
                WhitelistSyncCore.LOGGER.error(e.getMessage(), e);
            }
        } else {
            WhitelistSyncCore.LOGGER.error("Op list syncing is currently disabled in your config. "
                    + "Please enable it and restart the server to use this feature.");
        }

        return false;
    }

    @Override
    public boolean addWhitelistPlayer(UUID uuid, String name) {
        try {
            // Open connection=
            Connection conn = DriverManager.getConnection(url, username, password);
            long startTime = System.currentTimeMillis();

            String sql = "REPLACE INTO " + databaseName + ".whitelist(uuid, name, whitelisted) VALUES (?, ?, true)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, uuid.toString());
            stmt.setString(2, name);
            stmt.executeUpdate();

            // Time taken.
            long timeTaken = System.currentTimeMillis() - startTime;
            WhitelistSyncCore.LOGGER.debug("Added " + name + " to whitelist | Took " + timeTaken + "ms");
            stmt.close();
            conn.close();
            return true;

        } catch (SQLException e) {
            WhitelistSyncCore.LOGGER.error("Error adding " + name + " to whitelist database!");
            WhitelistSyncCore.LOGGER.error(e.getMessage(), e);
        }

        return false;
    }

    @Override
    public boolean addOppedPlayer(UUID uuid, String name) {
        if (this.syncingOpList) {
            try {
                // Open connection=
                Connection conn = DriverManager.getConnection(url, username, password);
                long startTime = System.currentTimeMillis();

                String sql = "REPLACE INTO " + databaseName + ".op(uuid, name, isOp) VALUES (?, ?, true)";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, uuid.toString());
                stmt.setString(2, name);
                stmt.executeUpdate();

                // Time taken.
                long timeTaken = System.currentTimeMillis() - startTime;
                WhitelistSyncCore.LOGGER.debug("Database opped " + name + " | Took " + timeTaken + "ms");
                stmt.close();
                conn.close();
                return true;

            } catch (SQLException e) {
                WhitelistSyncCore.LOGGER.error("Error opping " + name + " !");
                WhitelistSyncCore.LOGGER.error(e.getMessage(), e);
            }
        } else {
            WhitelistSyncCore.LOGGER.error("Op list syncing is currently disabled in your config. "
                    + "Please enable it and restart the server to use this feature.");
        }

        return false;
    }

    @Override
    public boolean removeWhitelistPlayer(UUID uuid, String name) {
        try {
            // Open connection=
            Connection conn = DriverManager.getConnection(url, username, password);
            long startTime = System.currentTimeMillis();

            String sql = "REPLACE INTO " + databaseName + ".whitelist(uuid, name, whitelisted) VALUES (?, ?, false)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, uuid.toString());
            stmt.setString(2, name);
            stmt.executeUpdate();

            // Time taken.
            long timeTaken = System.currentTimeMillis() - startTime;
            WhitelistSyncCore.LOGGER.debug("Removed " + name + " from whitelist | Took " + timeTaken + "ms");
            stmt.close();
            conn.close();
            return true;

        } catch (SQLException e) {
            WhitelistSyncCore.LOGGER.error("Error removing " + name + " to whitelist database!");
            WhitelistSyncCore.LOGGER.error(e.getMessage(), e);
        }

        return false;
    }

    @Override
    public boolean removeOppedPlayer(UUID uuid, String name) {
        if (this.syncingOpList) {
            try {
                // Open connection=
                Connection conn = DriverManager.getConnection(url, username, password);
                long startTime = System.currentTimeMillis();

                String sql = "REPLACE INTO " + databaseName + ".op(uuid, name, isOp) VALUES (?, ?, false)";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, uuid.toString());
                stmt.setString(2, name);
                stmt.executeUpdate();

                // Time taken.
                long timeTaken = System.currentTimeMillis() - startTime;
                WhitelistSyncCore.LOGGER.debug("Deopped " + name + " | Took " + timeTaken + "ms");
                stmt.close();
                conn.close();
                return true;

            } catch (SQLException e) {
                WhitelistSyncCore.LOGGER.error("Error deopping " + name + ".");
                WhitelistSyncCore.LOGGER.error(e.getMessage(), e);
            }
        } else {
            WhitelistSyncCore.LOGGER.error("Op list syncing is currently disabled in your config. "
                    + "Please enable it and restart the server to use this feature.");
        }

        return false;
    }

    private static void migrateOpList(Connection conn, String databaseName) throws SQLException {
        String sql;
        PreparedStatement stmt;

        // Add new level field to op table if it doesn't exist
        sql =
                "SELECT COUNT(*) AS count " +
                        "FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = '" + databaseName + "' AND TABLE_NAME = 'op' AND COLUMN_NAME = 'level'";
        stmt = conn.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery();
        stmt.close();
        rs.next();

        if(rs.getInt("count") == 0) {
            sql = "ALTER TABLE " + databaseName + ".op ADD COLUMN level INTEGER NOT NULL DEFAULT 4";
            stmt = conn.prepareStatement(sql);
            stmt.execute();
            stmt.close();
            WhitelistSyncCore.LOGGER.info("Added new op table \"level\" column. Existing entries get set to default level 4.");
        }
        rs.close();


        // Add new bypassesPlayerLimit field to op table if it doesn't exist
        sql =
                "SELECT COUNT(*) AS count " +
                        "FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = '" + databaseName + "' AND TABLE_NAME = 'op' AND COLUMN_NAME = 'bypassesPlayerLimit'";
        stmt = conn.prepareStatement(sql);
        ResultSet rs1 = stmt.executeQuery();
        stmt.close();
        rs1.next();

        if(rs1.getInt("count") == 0) {
            sql = "ALTER TABLE " + databaseName + ".op ADD COLUMN bypassesPlayerLimit TINYINT NOT NULL DEFAULT 0";
            stmt = conn.prepareStatement(sql);
            stmt.execute();
            stmt.close();
            WhitelistSyncCore.LOGGER.info("Added new op table \"bypassesPlayerLimit\" column. Existing entries get set to default bypassesPlayerLimit false.");
        }
        rs1.close();
    }
}
