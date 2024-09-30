package net.rmnad.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.rmnad.Log;
import net.rmnad.models.OppedPlayer;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;


/**
 * Class to read json data from the server's ops.json file
 */
public class OppedPlayersFileReader {
    
    private static JsonParser parser = new JsonParser();

    // Get Arraylist of opped players on server.
    public static ArrayList<OppedPlayer> getOppedPlayers(String serverRootPath) {
        ArrayList<OppedPlayer> users = new ArrayList<>();

        // Get Json data
        getOppedPlayersFromFile(serverRootPath).forEach((record) -> {
            String uuid = ((JsonObject) record).get("uuid").getAsString();
            String name = ((JsonObject) record).get("name").getAsString();
            int level = ((JsonObject) record).get("level").getAsInt();
            boolean bypassesPlayerLimit = ((JsonObject) record).get("bypassesPlayerLimit").getAsBoolean();

            // Create DTO
            OppedPlayer oppedPlayer = new OppedPlayer();
            oppedPlayer.setUuid(uuid);
            oppedPlayer.setName(name);
            oppedPlayer.setLevel(level);
            oppedPlayer.setBypassesPlayerLimit(bypassesPlayerLimit);

            
            users.add(oppedPlayer);
        });

        return users;
    }

    private static JsonArray getOppedPlayersFromFile(String serverRootPath) {
        JsonArray oplist = null;
        try {
            // Read data as Json array from server directory
            oplist = (JsonArray) parser.parse(new FileReader(serverRootPath + "/ops.json"));
            
            Log.debug("getOppedPlayersFromFile returned an array of " + oplist.size() + " entries.");
        } catch (FileNotFoundException e) {
            Log.error("ops.json file not found.", e);
        } catch (JsonParseException e) {
            Log.error("ops.json parse error.", e);
        }
        
        return oplist;
    }
    
}
