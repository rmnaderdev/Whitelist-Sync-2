package net.rmnad.services;

import io.reactivex.rxjava3.annotations.Nullable;
import net.rmnad.models.BannedPlayer;
import net.rmnad.models.OppedPlayer;
import net.rmnad.models.WhitelistedPlayer;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Interface for different database services
 */
public interface BaseService {

    public boolean initializeDatabase();

    // Getter functions
    public ArrayList<WhitelistedPlayer> getWhitelistedPlayersFromDatabase();
    public ArrayList<OppedPlayer> getOppedPlayersFromDatabase();
    public ArrayList<BannedPlayer> getBannedPlayersFromDatabase();
    public ArrayList<String> getBannedIpsFromDatabase();

    // Syncing functions
    public boolean pushLocalWhitelistToDatabase();
    public boolean pushLocalOpsToDatabase();
    public boolean pushLocalBannedPlayersToDatabase();
    public boolean pushLocalBannedIpsToDatabase();

    public boolean pullDatabaseWhitelistToLocal();
    public boolean pullDatabaseOpsToLocal();
    public boolean pullDatabaseBannedPlayersToLocal();
    public boolean pullDatabaseBannedIpsToLocal();



    // Addition functions
    public boolean addWhitelistPlayer(UUID uuid, String name);
    public boolean addOppedPlayer(UUID uuid, String name);
    public boolean addBannedPlayer(UUID uuid, String name, @Nullable String reason);
    public boolean addBannedIp(String ip, @Nullable String reason);


    // Removal functions
    public boolean removeWhitelistPlayer(UUID uuid, String name);
    public boolean removeOppedPlayer(UUID uuid, String name);
    public boolean removeBannedPlayer(UUID uuid, String name);
    public boolean removeBannedIp(String ip);

}
