package net.rmnad.services;

import net.rmnad.callbacks.*;
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

    // Syncing functions
    public boolean pushLocalWhitelistToDatabase();
    public boolean pushLocalOpsToDatabase();

    public boolean pullDatabaseWhitelistToLocal();
    public boolean pullDatabaseOpsToLocal();


    // Addition functions
    public boolean addWhitelistPlayer(UUID uuid, String name);
    public boolean addOppedPlayer(UUID uuid, String name);


    // Removal functions
    public boolean removeWhitelistPlayer(UUID uuid, String name);
    public boolean removeOppedPlayer(UUID uuid, String name);

}
