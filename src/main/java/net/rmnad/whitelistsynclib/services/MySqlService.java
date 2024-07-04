package net.rmnad.whitelistsynclib.services;

import net.rmnad.whitelistsynclib.WhitelistSyncLib;
import net.rmnad.whitelistsynclib.callbacks.IOnUserAdd;
import net.rmnad.whitelistsynclib.callbacks.IOnUserRemove;
import net.rmnad.whitelistsynclib.models.OppedPlayer;
import net.rmnad.whitelistsynclib.models.WhitelistedPlayer;

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
        WhitelistSyncLib.LOGGER.info("Setting up the MySQL service...");
        boolean isSuccess = true;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            WhitelistSyncLib.LOGGER.error("Failed to init mysql-connector. Is the library missing?");
            WhitelistSyncLib.LOGGER.error(e.getMessage(), e);
            isSuccess = false;
        }


        if (isSuccess) {
            try {
                Connection conn = DriverManager.getConnection(url, username, password);
                WhitelistSyncLib.LOGGER.debug("Connected to " + url + " successfully!");
                conn.close();
            } catch (SQLException e) {
                WhitelistSyncLib.LOGGER.error("Failed to connect to the mySQL database! Did you set one up in the config?");
                WhitelistSyncLib.LOGGER.error(e.getMessage(), e);
                isSuccess = false;
            }
        }

        if (isSuccess) {
            // Create database
            try {

                // Create database
                String sql = "CREATE DATABASE IF NOT EXISTS " + databaseName + ";";

                // Create statement
                Connection conn = DriverManager.getConnection(url, username, password);
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.execute();
                stmt.close();

                // Create whitelist table
                sql = "CREATE TABLE IF NOT EXISTS " + databaseName + ".whitelist ("
                        + "`uuid` VARCHAR(60) NOT NULL,"
                        + "`name` VARCHAR(20) NOT NULL,"
                        + "`whitelisted` TINYINT NOT NULL DEFAULT 1,"
                        + "PRIMARY KEY (`uuid`)"
                        + ")";
                PreparedStatement stmt2 = conn.prepareStatement(sql);
                stmt2.execute();
                stmt2.close();

                // Create opped players table if enabled
                if (this.syncingOpList) {
                    sql = "CREATE TABLE IF NOT EXISTS " + databaseName + ".op ("
                            + "`uuid` VARCHAR(60) NOT NULL,"
                            + "`name` VARCHAR(20) NOT NULL,"
                            + "`isOp` TINYINT NOT NULL DEFAULT 1,"
                            + "PRIMARY KEY (`uuid`)"
                            + ")";
                    PreparedStatement stmt3 = conn.prepareStatement(sql);
                    stmt3.execute();
                    stmt3.close();


                    // Remove old op level field if it exists
                    sql =
                            "SELECT COUNT(*) AS count " +
                                    "FROM INFORMATION_SCHEMA.COLUMNS " +
                                    "WHERE TABLE_SCHEMA = '" + databaseName + "' AND TABLE_NAME = 'op' AND COLUMN_NAME = 'level'";
                    PreparedStatement stmt4 = conn.prepareStatement(sql);
                    ResultSet rs = stmt4.executeQuery();
                    rs.next();

                    int count = rs.getInt("count");

                    if(count > 0) {
                        sql = "ALTER TABLE " + databaseName + ".op DROP COLUMN level";
                        PreparedStatement stmt5 = conn.prepareStatement(sql);
                        stmt5.execute();
                        stmt5.close();
                        WhitelistSyncLib.LOGGER.info("Removed unused op table \"level\" column.");
                    }
                    rs.close();
                    stmt4.close();


                    // Remove old op bypassesPlayerLimit field if it exists
                    sql =
                            "SELECT COUNT(*) AS count " +
                                    "FROM INFORMATION_SCHEMA.COLUMNS " +
                                    "WHERE TABLE_SCHEMA = '" + databaseName + "' AND TABLE_NAME = 'op' AND COLUMN_NAME = 'bypassesPlayerLimit'";
                    PreparedStatement stmt5 = conn.prepareStatement(sql);
                    ResultSet rs1 = stmt5.executeQuery();
                    rs1.next();

                    int count1 = rs1.getInt("count");

                    if(count1 > 0) {
                        sql = "ALTER TABLE " + databaseName + ".op DROP COLUMN bypassesPlayerLimit";
                        PreparedStatement stmt6 = conn.prepareStatement(sql);
                        stmt6.execute();
                        stmt6.close();
                        WhitelistSyncLib.LOGGER.info("Removed unused op table \"bypassesPlayerLimit\" column.");
                    }
                    rs1.close();
                    stmt5.close();

                }

                WhitelistSyncLib.LOGGER.info("Setup MySQL database!");
                conn.close();
            } catch (Exception e) {
                WhitelistSyncLib.LOGGER.error("Error initializing database and database tables.");
                WhitelistSyncLib.LOGGER.error(e.getMessage(), e);
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

            WhitelistSyncLib.LOGGER.debug("Database pulled whitelisted players | Took " + timeTaken + "ms | Read " + records + " records.");

            rs.close();
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            // Something is wrong...
            WhitelistSyncLib.LOGGER.error("Error querying whitelisted players from database!");
            WhitelistSyncLib.LOGGER.error(e.getMessage(), e);
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

                String sql = "SELECT uuid, name FROM " + databaseName + ".op WHERE isOp = true;";
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();

                // Add queried results to arraylist.
                while (rs.next()) {
                    oppedPlayers.add(new OppedPlayer(rs.getString("uuid"), rs.getString("name"), true));
                    records++;
                }

                // Time taken
                long timeTaken = System.currentTimeMillis() - startTime;

                WhitelistSyncLib.LOGGER.debug("Database pulled opped players | Took " + timeTaken + "ms | Read " + records + " records.");

                rs.close();
                stmt.close();
                conn.close();
            } catch (SQLException e) {
                WhitelistSyncLib.LOGGER.error("Error querying opped players from database!");
                WhitelistSyncLib.LOGGER.error(e.getMessage(), e);
            }
        } else {
            WhitelistSyncLib.LOGGER.error("Op list syncing is currently disabled in your config. "
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
            WhitelistSyncLib.LOGGER.debug("Whitelist table updated | Took " + timeTaken + "ms | Wrote " + records + " records.");
            conn.close();

            return true;
        } catch (SQLException e) {
            WhitelistSyncLib.LOGGER.error("Failed to update database with local records.");
            WhitelistSyncLib.LOGGER.error(e.getMessage(), e);
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
                WhitelistSyncLib.LOGGER.debug("Op table updated | Took " + timeTaken + "ms | Wrote " + records + " records.");
                conn.close();

                return true;
            } catch (SQLException e) {
                WhitelistSyncLib.LOGGER.error("Failed to update database with local records.");
                WhitelistSyncLib.LOGGER.error(e.getMessage(), e);
            }
        } else {
            WhitelistSyncLib.LOGGER.error("Op list syncing is currently disabled in your config. "
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
                    if (localWhitelistedPlayers.stream().noneMatch(o -> o.getUuid().equals(uuid))) {
                        try {
                            onUserAdd.call(uuid, name);
                            WhitelistSyncLib.LOGGER.debug("Added " + name + " to whitelist.");
                            records++;
                        } catch (NullPointerException e) {
                            WhitelistSyncLib.LOGGER.error("Player is null?");
                            WhitelistSyncLib.LOGGER.error(e.getMessage(), e);
                        }
                    }
                } else {
                    if (localWhitelistedPlayers.stream().anyMatch(o -> o.getUuid().equals(uuid))) {
                        onUserRemove.call(uuid, name);
                        WhitelistSyncLib.LOGGER.debug("Removed " + name + " from whitelist.");
                        records++;
                    }
                }

            }
            long timeTaken = System.currentTimeMillis() - startTime;
            WhitelistSyncLib.LOGGER.debug("Copied whitelist database to local | Took " + timeTaken + "ms | Wrote " + records + " records.");

            rs.close();
            stmt.close();
            conn.close();
            return true;
        } catch (SQLException e) {
            WhitelistSyncLib.LOGGER.error("Error querying whitelisted players from database!");
            WhitelistSyncLib.LOGGER.error(e.getMessage(), e);
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

                String sql = "SELECT name, uuid, isOp FROM " + databaseName + ".op";
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String name = rs.getString("name");
                    int opped = rs.getInt("isOp");

                    if (opped == 1) {
                        if (localOppedPlayers.stream().noneMatch(o -> o.getUuid().equals(uuid))) {
                            try {
                                onUserAdd.call(uuid, name);
                                WhitelistSyncLib.LOGGER.debug("Opped " + name + ".");
                                records++;
                            } catch (NullPointerException e) {
                                WhitelistSyncLib.LOGGER.error("Player is null?");
                                WhitelistSyncLib.LOGGER.error(e.getMessage(), e);
                            }
                        }
                    } else {
                        if (localOppedPlayers.stream().anyMatch(o -> o.getUuid().equals(uuid))) {
                            onUserRemove.call(uuid, name);
                            WhitelistSyncLib.LOGGER.debug("Deopped " + name + ".");
                            records++;
                        }
                    }

                }
                long timeTaken = System.currentTimeMillis() - startTime;
                WhitelistSyncLib.LOGGER.debug("Copied op database to local | Took " + timeTaken + "ms | Wrote " + records + " records.");

                rs.close();
                stmt.close();
                conn.close();
                return true;
            } catch (SQLException e) {
                WhitelistSyncLib.LOGGER.error("Error querying opped players from database!");
                WhitelistSyncLib.LOGGER.error(e.getMessage(), e);
            }
        } else {
            WhitelistSyncLib.LOGGER.error("Op list syncing is currently disabled in your config. "
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
            WhitelistSyncLib.LOGGER.debug("Added " + name + " to whitelist | Took " + timeTaken + "ms");
            stmt.close();
            conn.close();
            return true;

        } catch (SQLException e) {
            WhitelistSyncLib.LOGGER.error("Error adding " + name + " to whitelist database!");
            WhitelistSyncLib.LOGGER.error(e.getMessage(), e);
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
                WhitelistSyncLib.LOGGER.debug("Database opped " + name + " | Took " + timeTaken + "ms");
                stmt.close();
                conn.close();
                return true;

            } catch (SQLException e) {
                WhitelistSyncLib.LOGGER.error("Error opping " + name + " !");
                WhitelistSyncLib.LOGGER.error(e.getMessage(), e);
            }
        } else {
            WhitelistSyncLib.LOGGER.error("Op list syncing is currently disabled in your config. "
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
            WhitelistSyncLib.LOGGER.debug("Removed " + name + " from whitelist | Took " + timeTaken + "ms");
            stmt.close();
            conn.close();
            return true;

        } catch (SQLException e) {
            WhitelistSyncLib.LOGGER.error("Error removing " + name + " to whitelist database!");
            WhitelistSyncLib.LOGGER.error(e.getMessage(), e);
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
                WhitelistSyncLib.LOGGER.debug("Deopped " + name + " | Took " + timeTaken + "ms");
                stmt.close();
                conn.close();
                return true;

            } catch (SQLException e) {
                WhitelistSyncLib.LOGGER.error("Error deopping " + name + ".");
                WhitelistSyncLib.LOGGER.error(e.getMessage(), e);
            }
        } else {
            WhitelistSyncLib.LOGGER.error("Op list syncing is currently disabled in your config. "
                    + "Please enable it and restart the server to use this feature.");
        }

        return false;
    }
}
