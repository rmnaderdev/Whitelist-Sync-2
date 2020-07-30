package pw.twpi.whitelistsync2.services;

import com.mojang.authlib.GameProfile;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.WhitelistEntry;
import pw.twpi.whitelistsync2.Config;
import pw.twpi.whitelistsync2.WhitelistSync2;
import pw.twpi.whitelistsync2.json.OPlistRead;
import pw.twpi.whitelistsync2.json.WhitelistRead;
import pw.twpi.whitelistsync2.models.OpUser;
import pw.twpi.whitelistsync2.services.BaseService;

/**
 * Service for MYSQL Databases
 *
 * @author Richard Nader, Jr. <rmnader@svsu.edu>
 */
public class MySqlService implements BaseService {

    private Connection conn = null;
    private String S_SQL = "";

    private String databaseName;
    private String url;
    private String username;
    private String password;

    public MySqlService() {
        
        try {
            Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
        } catch (Exception ex) {
            WhitelistSync2.LOGGER.error("Failed to init mysql-connector. Is the library missing?");
        }
        
        this.databaseName = Config.MYSQL_DB_NAME.get();
        this.url = "jdbc:mysql://" + Config.MYSQL_IP.get() + ":" + Config.MYSQL_PORT.get() + "/?serverTimezone=UTC&autoReconnect=true";
        this.username = Config.MYSQL_USERNAME.get();
        this.password = Config.MYSQL_PASSWORD.get();

        try {
            conn = DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            WhitelistSync2.LOGGER.error("Failed to connect to the mySQL database! Did you set one up in the config?\n" + e.getMessage());
            throw new RuntimeException("Failed to connect to the mySQL database! Did you set one up in the config?\n" + e.getMessage());
        }

        loadDatabase();
    }

    private Connection getConnection() {
        return conn;
    }

    /**
     * Method to load database on startup.
     *
     * @return success
     */
    private boolean loadDatabase() {
        // Create database
        try {

            // Create database
            S_SQL = "CREATE DATABASE IF NOT EXISTS " + databaseName + ";";

            // Create statement
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(S_SQL);
            stmt.execute();

            // Create table
            S_SQL = "CREATE TABLE IF NOT EXISTS " + databaseName + ".whitelist ("
                    + "`uuid` VARCHAR(60) NOT NULL,"
                    + "`name` VARCHAR(20) NOT NULL,"
                    + "`whitelisted` TINYINT NOT NULL DEFAULT 1,"
                    + "PRIMARY KEY (`uuid`)"
                    + ")";
            PreparedStatement stmt2 = conn.prepareStatement(S_SQL);
            stmt2.execute();

            // Create table for op list
            if (Config.SYNC_OP_LIST.get()) {
                S_SQL = "CREATE TABLE IF NOT EXISTS " + databaseName + ".op ("
                        + "`uuid` VARCHAR(60) NOT NULL,"
                        + "`name` VARCHAR(20) NOT NULL,"
                        + "`level` INT NOT NULL,"
                        + "`bypassesPlayerLimit` TINYINT NOT NULL DEFAULT 0,"
                        + "`isOp` TINYINT NOT NULL DEFAULT 1,"
                        + "PRIMARY KEY (`uuid`)"
                        + ")";
                PreparedStatement stmt3 = conn.prepareStatement(S_SQL);
                stmt3.execute();

                WhitelistSync2.LOGGER.info("OP Sync is ENABLED!");
            } else {
                WhitelistSync2.LOGGER.info("OP Sync is DISABLED!");
            }

            WhitelistSync2.LOGGER.info("Loaded mySQL database!");
            
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } catch (Exception ee) {
            ee.printStackTrace();
            return false;
        }
    }

    /**
     * Pushes local json whitelist to the database
     *
     * @param server
     * @return success
     */
    @Override
    public boolean pushLocalWhitelistToDatabase(MinecraftServer server) {
        // Load local whitelist to memory.
        ArrayList<String> uuids = WhitelistRead.getWhitelistUUIDs();
        ArrayList<String> names = WhitelistRead.getWhitelistNames();

        // Start job on thread to avoid lag.
        // Keep track of records.
        int records = 0;
        try {
            // Connect to database.
            Connection conn1 = getConnection();
            long startTime = System.currentTimeMillis();
            // Loop through local whitelist and insert into database.
            for (int i = 0; i < uuids.size() || i < names.size(); i++) {
                if ((uuids.get(i) != null) && (names.get(i) != null)) {
                    try {
                        PreparedStatement sql = conn1.prepareStatement("INSERT IGNORE INTO " + databaseName + ".whitelist(uuid, name, whitelisted) VALUES (?, ?, 1)");
                        sql.setString(1, uuids.get(i));
                        sql.setString(2, names.get(i));
                        sql.executeUpdate();
                        records++;
                    } catch (ClassCastException e) {
                        e.printStackTrace();
                    }
                }
            }
            // Record time taken.
            long timeTaken = System.currentTimeMillis() - startTime;
            WhitelistSync2.LOGGER.info("Wrote " + records + " to whitelist table in " + timeTaken + "ms.");
            WhitelistSync2.LOGGER.debug("Whitelist Table Updated | Took " + timeTaken + "ms | Wrote " + records + " records.");
            return true;
        } catch (SQLException e) {
            WhitelistSync2.LOGGER.error("Failed to update database with local records.\n" + e.getMessage());
            return false;
        }
    }

    /**
     * Pushes local json op list to the database
     *
     * @param server
     * @return success
     */
    @Override
    public boolean pushLocalOpListToDatabase(MinecraftServer server) {

        ArrayList<OpUser> opUsers = OPlistRead.getOppedUsers();

        // Start job on thread to avoid lag.
        // Keep track of records.
        int records = 0;
        try {
            // Connect to database.
            Connection conn1 = getConnection();
            // If syncing op list
            if (Config.SYNC_OP_LIST.get()) {
                records = 0;
                long opStartTime = System.currentTimeMillis();
                // Loop through ops list and add to DB
                for (OpUser opUser : opUsers) {
                    try {
                        PreparedStatement sql = conn1.prepareStatement("INSERT IGNORE INTO " + databaseName + ".op(uuid, name, level, bypassesPlayerLimit, isOp) VALUES (?, ?, ?, ?, true)");
                        sql.setString(1, opUser.getUuid());
                        sql.setString(2, opUser.getName());
                        sql.setInt(3, opUser.getLevel());
                        sql.setInt(4, opUser.isBypassesPlayerLimit() ? 1 : 0);
                        sql.executeUpdate();
                        records++;
                    } catch (ClassCastException e) {
                        e.printStackTrace();
                    }
                    records++;
                }
                // Record time taken.
                long opTimeTaken = System.currentTimeMillis() - opStartTime;
                WhitelistSync2.LOGGER.info("Wrote " + records + " to op table in " + opTimeTaken + "ms.");
                WhitelistSync2.LOGGER.debug("Op Table Updated | Took " + opTimeTaken + "ms | Wrote " + records + " records.");
            }

            return true;
        } catch (SQLException e) {
            WhitelistSync2.LOGGER.error("Failed to update database with local records.\n" + e.getMessage());
            return false;
        }
    }

    /**
     * Pull uuids of whitelisted players from database
     *
     * @param server
     * @return List of whitelisted player uuids
     */
    @Override
    public ArrayList<String> pullWhitelistedUuidsFromDatabase(MinecraftServer server) {
        // ArrayList for uuids.
        ArrayList<String> uuids = new ArrayList<>();

        try {
            // Keep track of records.
            int records = 0;

            // Connect to database.
            Connection conn = getConnection();
            long startTime = System.currentTimeMillis();

            String sql = "SELECT uuid, whitelisted FROM " + databaseName + ".whitelist";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            // Add querried results to arraylist.
            while (rs.next()) {
                if (rs.getInt("whitelisted") == 1) {
                    uuids.add(rs.getString("uuid"));
                }
                records++;
            }

            // Time taken
            long timeTaken = System.currentTimeMillis() - startTime;

            WhitelistSync2.LOGGER.debug("Whitelist Database Pulled | Took " + timeTaken + "ms | Read " + records + " records.");
        } catch (SQLException e) {
            WhitelistSync2.LOGGER.error("Error querrying uuids from whitelist database!\n" + e.getMessage());
        }
        return uuids;
    }

    /**
     * Pull uuids of opped players from database
     *
     * @param server
     * @return List of opped player uuids
     */
    @Override
    public ArrayList<String> pullOpUuidsFromDatabase(MinecraftServer server) {
        // ArrayList for uuids.
        ArrayList<String> uuids = new ArrayList<>();

        try {
            // Keep track of records.
            int records = 0;

            // Connect to database.
            Connection conn = getConnection();
            long startTime = System.currentTimeMillis();

            String sql = "SELECT uuid, isOp FROM " + databaseName + ".op";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            // Add querried results to arraylist.
            while (rs.next()) {
                if (rs.getInt("isOp") == 1) {
                    uuids.add(rs.getString("uuid"));
                }
                records++;
            }

            // Time taken
            long timeTaken = System.currentTimeMillis() - startTime;

            WhitelistSync2.LOGGER.debug("Op Database Pulled | Took " + timeTaken + "ms | Read " + records + " records.");
        } catch (SQLException e) {
            WhitelistSync2.LOGGER.error("Error querrying uuids from op database!\n" + e.getMessage());
        }
        return uuids;
    }

    /**
     * Pull names of whitelisted players from database
     *
     * @param server
     * @return List of whitelisted players names
     */
    @Override
    public ArrayList<String> pullWhitelistedNamesFromDatabase(MinecraftServer server) {
        // ArrayList for names.
        ArrayList<String> names = new ArrayList<String>();

        try {

            // Keep track of records.
            int records = 0;

            // Connect to database.
            Connection conn = getConnection();
            long startTime = System.currentTimeMillis();

            String sql = "SELECT name, whitelisted FROM " + databaseName + ".whitelist";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            // Save querried return to names list.
            while (rs.next()) {
                if (rs.getInt("whitelisted") == 1) {
                    names.add(rs.getString("name"));
                }
                records++;
            }

            // Total time taken.
            long timeTaken = System.currentTimeMillis() - startTime;

            WhitelistSync2.LOGGER.debug("Whitelist Database Pulled | Took " + timeTaken + "ms | Read " + records + " records.");
        } catch (SQLException e) {
            WhitelistSync2.LOGGER.error("Error querrying names from whitelist database!\n" + e.getMessage());
        }
        return names;
    }

    /**
     * Pull names of opped players from database
     *
     * @param server
     * @return List of opped players names
     */
    @Override
    public ArrayList<String> pullOppedNamesFromDatabase(MinecraftServer server) {
        // ArrayList for names.
        ArrayList<String> names = new ArrayList<>();

        try {

            // Keep track of records.
            int records = 0;

            // Connect to database.
            Connection conn = getConnection();
            long startTime = System.currentTimeMillis();

            String sql = "SELECT name, isOp FROM " + databaseName + ".op";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            // Save querried return to names list.
            while (rs.next()) {
                if (rs.getInt("isOp") == 1) {
                    names.add(rs.getString("name"));
                }
                records++;
            }

            // Total time taken.
            long timeTaken = System.currentTimeMillis() - startTime;

            WhitelistSync2.LOGGER.debug("Op Database Pulled | Took " + timeTaken + "ms | Read " + records + " records.");
        } catch (SQLException e) {
            WhitelistSync2.LOGGER.error("Error querrying names from op database!\n" + e.getMessage());
        }
        return names;
    }

    /**
     * Method adds player to whitelist in database
     *
     * @param player
     * @return success
     */
    @Override
    public boolean addPlayerToDatabaseWhitelist(GameProfile player) {
        try {
            // Start time.
            long startTime = System.currentTimeMillis();
            // Open connection
            Connection conn1 = getConnection();
            String sql = "REPLACE INTO " + databaseName + ".whitelist(uuid, name, whitelisted) VALUES (?, ?, 1)";
            PreparedStatement stmt = conn1.prepareStatement(sql);
            stmt.setString(1, String.valueOf(player.getId()));
            stmt.setString(2, player.getName());
            // Execute statement.
            stmt.execute();
            // Time taken.
            long timeTaken = System.currentTimeMillis() - startTime;
            WhitelistSync2.LOGGER.debug("Database Added " + player.getName() + " | Took " + timeTaken + "ms");
            return true;

        } catch (SQLException e) {
            WhitelistSync2.LOGGER.error("Error adding " + player.getName() + " to database!\n" + e.getMessage());
            return false;
        }
    }

    /**
     * Method removes player from whitelist in database
     *
     * @param player
     * @return success
     */
    @Override
    public boolean removePlayerFromDatabaseWhitelist(GameProfile player) {
        try {
            // Start time.
            long startTime = System.currentTimeMillis();
            // Open connection
            Connection conn1 = getConnection();
            String sql = "REPLACE INTO " + databaseName + ".whitelist(uuid, name, whitelisted) VALUES (?, ?, 0)";
            PreparedStatement stmt = conn1.prepareStatement(sql);
            stmt.setString(1, String.valueOf(player.getId()));
            stmt.setString(2, player.getName());
            // Execute statement.
            stmt.execute();
            // Time taken.
            long timeTaken = System.currentTimeMillis() - startTime;
            WhitelistSync2.LOGGER.debug("Database Removed " + player.getName() + " | Took " + timeTaken + "ms");
            return true;

        } catch (SQLException e) {
            WhitelistSync2.LOGGER.error("Error removing " + player.getName() + " to database!\n" + e.getMessage());
            return false;
        }
    }

    /**
     * Method adds player to op list in database
     *
     * @param player
     * @return success
     */
    @Override
    public boolean addPlayerToDatabaseOp(GameProfile player) {
        try {
            // Start time.
            long startTime = System.currentTimeMillis();
            // Open connection
            Connection conn1 = getConnection();
            String sql = "REPLACE INTO " + databaseName + ".op(uuid, name, level, isOp, bypassesPlayerLimit) VALUES (?, ?, ?, true, ?)";
            PreparedStatement stmt = conn1.prepareStatement(sql);
            stmt.setString(1, String.valueOf(player.getId()));
            stmt.setString(2, player.getName());
            stmt.setInt(3, 4);
            stmt.setInt(4, 1);

            // Execute statement.
            stmt.execute();
            // Time taken.
            long timeTaken = System.currentTimeMillis() - startTime;
            WhitelistSync2.LOGGER.debug("Op Database Added " + player.getName() + " | Took " + timeTaken + "ms");
            
            addPlayerToDatabaseWhitelist(player);   // Whitelist player too
            
            return true;
        } catch (SQLException e) {
            WhitelistSync2.LOGGER.error("Error adding " + player.getName() + " to op database!\n" + e.getMessage());
            return false;
        }
    }

    /**
     * Method removes player from op list in database
     *
     * @param player
     * @return success
     */
    @Override
    public boolean removePlayerFromDatabaseOp(GameProfile player) {
        try {
            ArrayList<OpUser> oppedUsers = OPlistRead.getOppedUsers();
            // Start time.
            long startTime = System.currentTimeMillis();
            // Open connection
            Connection conn1 = getConnection();
            String sql = "REPLACE INTO " + databaseName + ".op(uuid, name, level, isOp, bypassesPlayerLimit) VALUES (?, ?, ?, false, ?)";
            PreparedStatement stmt = conn1.prepareStatement(sql);
            stmt.setString(1, String.valueOf(player.getId()));
            stmt.setString(2, player.getName());
            
            for (OpUser opUser : oppedUsers) {
                if (opUser.getUuid().equalsIgnoreCase(player.getId().toString())) {
                    stmt.setInt(3, opUser.getLevel());
                    stmt.setInt(4, opUser.isBypassesPlayerLimit() ? 1 : 0);
                }
            }
            
            // Execute statement.
            stmt.execute();
            // Time taken.
            long timeTaken = System.currentTimeMillis() - startTime;
            WhitelistSync2.LOGGER.debug("Op Database Removed " + player.getName() + " | Took " + timeTaken + "ms");

            return true;
        } catch (SQLException e) {
            WhitelistSync2.LOGGER.error("Error removing " + player.getName() + " from op database!\n" + e.getMessage());
            return false;
        }
    }

    /**
     * Method pulls whitelist from database and merges it into the local
     * whitelist
     *
     * @param server
     * @return success
     */
    @Override
    public boolean updateLocalWhitelistFromDatabase(MinecraftServer server) {
        try {
            int records = 0;

            // Start time
            long startTime = System.currentTimeMillis();

            // Open connection
            Connection conn = getConnection();
            String sql = "SELECT name, uuid, whitelisted FROM " + databaseName + ".whitelist";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            ArrayList<String> localUuids = WhitelistRead.getWhitelistUUIDs();
            while (rs.next()) {
                int whitelisted = rs.getInt("whitelisted");
                String uuid = rs.getString("uuid");
                String name = rs.getString("name");
                GameProfile player = new GameProfile(UUID.fromString(uuid), name);

                if (whitelisted == 1) {
                    if (!localUuids.contains(uuid)) {
                        try {
                            server.getPlayerList().getWhitelistedPlayers().addEntry(new WhitelistEntry(player));
                            
                            if(Config.ENABLE_DEBUG_LOGGING.get()) {
                                WhitelistSync2.LOGGER.info("Added " + name + " to whitelist.");
                            } else {
                                WhitelistSync2.LOGGER.debug("Added " + name + " to whitelist.");
                            }
                            
                        } catch (NullPointerException e) {
                            WhitelistSync2.LOGGER.error("Player is null?\n" + e.getMessage());
                        }
                    }
                } else {
                    if (localUuids.contains(uuid)) {
                        server.getPlayerList().getWhitelistedPlayers().removeEntry(player);
                        
                        if(Config.ENABLE_DEBUG_LOGGING.get()) {
                            WhitelistSync2.LOGGER.info("Removed " + name + " from whitelist.");
                        } else {
                            WhitelistSync2.LOGGER.debug("Removed " + name + " from whitelist.");
                        }
                    }
                }
                records++;
            }
            long timeTaken = System.currentTimeMillis() - startTime;
            WhitelistSync2.LOGGER.debug("Whitelist Table Pulled | Took " + timeTaken + "ms | Wrote " + records + " records.");
            //WhitelistSync2.LOGGER.info("Local whitelist.json up to date!");

            return true;
        } catch (SQLException e) {
            WhitelistSync2.LOGGER.error("Error querying whitelisted players from database!\n" + e.getMessage());
            return false;
        }
    }

    /**
     * Method pulls op list from database and merges it into the local op list
     *
     * @param server
     * @return success
     */
    @Override
    public boolean updateLocalOpListFromDatabase(MinecraftServer server) {
        try {
            int records = 0;

            // Start time
            long startTime = System.currentTimeMillis();

            // Open connection
            Connection conn = getConnection();
            String sql = "SELECT name, uuid, isOp FROM " + databaseName + ".op";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            ArrayList<String> localUuids = OPlistRead.getOpsUUIDs();

            while (rs.next()) {
                String uuid = rs.getString("uuid");
                String name = rs.getString("name");
                int isOp = rs.getInt("isOp");

                GameProfile player = new GameProfile(UUID.fromString(uuid), name);

                if (isOp == 1) {
                    if (!localUuids.contains(uuid)) {
                        try {
                            server.getPlayerList().addOp(player);
                            
                            if(Config.ENABLE_DEBUG_LOGGING.get()) {
                                WhitelistSync2.LOGGER.info("Opped " + name + ".");
                            } else {
                                WhitelistSync2.LOGGER.debug("Opped " + name + ".");
                            }
                            
                        } catch (NullPointerException e) {
                            WhitelistSync2.LOGGER.error("Player is null?\n" + e.getMessage());
                        }
                    }
                } else {
                    if (localUuids.contains(uuid)) {
                        server.getPlayerList().removeOp(player);
                        
                        if(Config.ENABLE_DEBUG_LOGGING.get()) {
                            WhitelistSync2.LOGGER.info("Deopped " + name + ".");
                        } else {
                            WhitelistSync2.LOGGER.debug("Deopped " + name + ".");
                        }
                    }
                }
                records++;
            }
            long timeTaken = System.currentTimeMillis() - startTime;
            WhitelistSync2.LOGGER.debug("Ops Table Pulled | Took " + timeTaken + "ms | Wrote " + records + " records.");
            //WhitelistSync2.LOGGER.debug("Local ops.json up to date!");

            return true;
        } catch (SQLException e) {
            WhitelistSync2.LOGGER.error("Error querying opped players from database!\n" + e.getMessage());
            return false;
        }
    }

}
