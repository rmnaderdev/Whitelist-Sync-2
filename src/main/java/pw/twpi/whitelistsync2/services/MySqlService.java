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
import pw.twpi.whitelistsync2.models.OppedPlayer;
import pw.twpi.whitelistsync2.models.WhitelistedPlayer;

/**
 * Service for MYSQL Databases
 *
 * @author Richard Nader, Jr. <rmnader@svsu.edu>
 */
public class MySqlService implements BaseService {


    public MySqlService() {
        WhitelistSync2.LOGGER.info("Setting up the MYSQL service...");
    }

    // Function used to initialize the database file
    @Override
    public boolean initializeDatabase() {
        return false;
    }

    @Override
    public ArrayList<WhitelistedPlayer> getWhitelistedPlayersFromDatabase() {
        return null;
    }

    @Override
    public ArrayList<OppedPlayer> getOppedPlayersFromDatabase() {
        return null;
    }

    @Override
    public ArrayList<WhitelistedPlayer> getWhitelistedPlayersFromLocal() {
        return null;
    }

    @Override
    public ArrayList<OppedPlayer> getOppedPlayersFromLocal() {
        return null;
    }

    @Override
    public boolean copyLocalWhitelistedPlayersToDatabase() {
        return false;
    }

    @Override
    public boolean copyLocalOppedPlayersToDatabase() {
        return false;
    }

    @Override
    public boolean copyDatabaseWhitelistedPlayersToLocal(MinecraftServer server) {
        return false;
    }

    @Override
    public boolean copyDatabaseOppedPlayersToLocal(MinecraftServer server) {
        return false;
    }

    @Override
    public boolean addWhitelistPlayer(GameProfile player) {
        return false;
    }

    @Override
    public boolean addOppedPlayer(GameProfile player) {
        return false;
    }

    @Override
    public boolean removeWhitelistPlayer(GameProfile player) {
        return false;
    }

    @Override
    public boolean removeOppedPlayer(GameProfile player) {
        return false;
    }
}
