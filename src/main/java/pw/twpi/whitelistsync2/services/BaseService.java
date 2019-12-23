package pw.twpi.whitelistsync2.services;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import net.minecraft.server.MinecraftServer;

/**
 * Interface for different database services
 * @author Richard Nader, Jr. <nader1rm@cmich.edu>
 */
public interface BaseService {
    
    // Pushed local whitelist to database
    public boolean pushLocalWhitelistToDatabase(MinecraftServer server);
    
    // Pushed local op list to database
    public boolean pushLocalOpListToDatabase(MinecraftServer server);

    // Gets ArrayList of uuids whitelisted in database.
    public ArrayList<String> pullWhitelistedUuidsFromDatabase(MinecraftServer server);

    // Gets ArrayList of uuids ops in database.
    public ArrayList<String> pullOpUuidsFromDatabase(MinecraftServer server);

    // Gets ArrayList of names whitelisted in database.
    public ArrayList<String> pullWhitelistedNamesFromDatabase(MinecraftServer server);

    // Gets ArrayList of names ops in database.
    public ArrayList<String> pullOppedNamesFromDatabase(MinecraftServer server);

    // Adds player to database whitelist.
    public boolean addPlayerToDatabaseWhitelist(GameProfile player);

    // Adds op player to database.
    public boolean addPlayerToDatabaseOp(GameProfile player);

    // Removes player from database.
    public boolean removePlayerFromDatabaseWhitelist(GameProfile player);

    // Removes op player from database.
    public boolean removePlayerFromDatabaseOp(GameProfile player);

    // Copies whitelist from database to server.
    public boolean updateLocalWhitelistFromDatabase(MinecraftServer server);

    // Copies op list from database to server.
    public boolean updateLocalOpListFromDatabase(MinecraftServer server);
}
