package pw.twpi.whitelistsync2.services;

import java.util.ArrayList;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import pw.twpi.whitelistsync2.models.OppedPlayer;
import pw.twpi.whitelistsync2.models.WhitelistedPlayer;

/**
 * Interface for different database services
 * @author Richard Nader, Jr. <rmnader@svsu.edu>
 */
public interface BaseService {

    public boolean initializeDatabase();

    public boolean requiresSyncing();


    // Getter functions
    public ArrayList<WhitelistedPlayer> getWhitelistedPlayersFromDatabase();
    public ArrayList<OppedPlayer> getOppedPlayersFromDatabase();

    public ArrayList<WhitelistedPlayer> getWhitelistedPlayersFromLocal();
    public ArrayList<OppedPlayer> getOppedPlayersFromLocal();


    // Syncing functions
    public boolean copyLocalWhitelistedPlayersToDatabase();
    public boolean copyLocalOppedPlayersToDatabase();

    public boolean copyDatabaseWhitelistedPlayersToLocal(MinecraftServer server);
    public boolean copyDatabaseOppedPlayersToLocal(MinecraftServer server);


    // Addition functions
    public boolean addWhitelistPlayer(GameProfile player);
    public boolean addOppedPlayer(GameProfile player);


    // Removal functions
    public boolean removeWhitelistPlayer(GameProfile player);
    public boolean removeOppedPlayer(GameProfile player);

}
