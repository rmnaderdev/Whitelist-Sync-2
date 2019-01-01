package com.potatosaucevfx.whitelistsync2.services;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import net.minecraft.server.MinecraftServer;

/**
 * Interface for different database services
 * @author Richard Nader, Jr. <nader1rm@cmich.edu>
 */
public interface BaseService {
    
    // Pushed local whitelist to database
    public void pushLocalWhitelistToDatabase(MinecraftServer server);
    
    // Pushed local op list to database
    public void pushLocalOpListToDatabase(MinecraftServer server);

    // Gets ArrayList of uuids whitelisted in database.
    public ArrayList<String> pullWhitelistedUuidsFromDatabase(MinecraftServer server);

    // Gets ArrayList of uuids ops in database.
    public ArrayList<String> pullOpUuidsFromDatabase(MinecraftServer server);

    // Gets ArrayList of names whitelisted in database.
    public ArrayList<String> pullWhitelistedNamesFromDatabase(MinecraftServer server);

    // Gets ArrayList of names ops in database.
    public ArrayList<String> pullOppedNamesFromDatabase(MinecraftServer server);

    // Adds player to database whitelist.
    public void addPlayerToDatabaseWhitelist(GameProfile player);

    // Adds op player to database.
    public void addPlayerToDatabaseOp(GameProfile player);

    // Removes player from database.
    public void removePlayerFromDatabaseWhitelist(GameProfile player);

    // Removes op player from database.
    public void removePlayerFromDatabaseOp(GameProfile player);

    // Copies whitelist from database to server.
    public void updateLocalWhitelistFromDatabase(MinecraftServer server);

    // Copies op list from database to server.
    public void updateLocalOpListFromDatabase(MinecraftServer server);
}
