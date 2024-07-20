package net.rmnad.core.services;

import net.rmnad.core.callbacks.IOnUserOpAdd;
import net.rmnad.core.callbacks.IOnUserOpRemove;
import net.rmnad.core.callbacks.IOnUserWhitelistAdd;
import net.rmnad.core.models.OppedPlayer;
import net.rmnad.core.models.WhitelistedPlayer;
import net.rmnad.core.callbacks.IOnUserWhitelistRemove;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Interface for different database services
 */
public interface BaseService {

    public boolean initializeDatabase();
    public boolean requiresSyncing();

    // Getter functions
    public ArrayList<WhitelistedPlayer> getWhitelistedPlayersFromDatabase();
    public ArrayList<OppedPlayer> getOppedPlayersFromDatabase();

    // Syncing functions
    public boolean pushLocalWhitelistToDatabase(ArrayList<WhitelistedPlayer> whitelistedPlayers);
    public boolean pushLocalOpsToDatabase(ArrayList<OppedPlayer> oppedPlayers);

    public boolean pullDatabaseWhitelistToLocal(ArrayList<WhitelistedPlayer> localWhitelistedPlayers, IOnUserWhitelistAdd onUserAdd, IOnUserWhitelistRemove onUserRemove);
    public boolean pullDatabaseOpsToLocal(ArrayList<OppedPlayer> localOppedPlayers, IOnUserOpAdd onUserAdd, IOnUserOpRemove onUserRemove);


    // Addition functions
    public boolean addWhitelistPlayer(UUID uuid, String name);
    public boolean addOppedPlayer(UUID uuid, String name);


    // Removal functions
    public boolean removeWhitelistPlayer(UUID uuid, String name);
    public boolean removeOppedPlayer(UUID uuid, String name);

}
