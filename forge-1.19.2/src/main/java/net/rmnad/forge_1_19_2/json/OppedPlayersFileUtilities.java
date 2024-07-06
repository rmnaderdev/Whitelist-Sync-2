package net.rmnad.forge_1_19_2.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.rmnad.forge_1_19_2.WhitelistSync2;
import net.rmnad.whitelistsync2.Log;
import net.rmnad.whitelistsync2.models.OppedPlayer;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;


/**
 * Class to read json data from the server's ops.json file
 */
public class OppedPlayersFileUtilities {
    
    private static JsonParser parser = new JsonParser();

    // Get Arraylist of opped players on server.
    public static ArrayList<OppedPlayer> getOppedPlayers() {
        ArrayList<OppedPlayer> users = new ArrayList<>();

        // Get Json data
        getOppedPlayersFromFile().forEach((record) -> {
            String uuid = ((JsonObject) record).get("uuid").getAsString();
            String name = ((JsonObject) record).get("name").getAsString();

            // Create DTO
            OppedPlayer oppedPlayer = new OppedPlayer();
            oppedPlayer.setUuid(uuid);
            oppedPlayer.setName(name);
            oppedPlayer.setIsOp(true);

            
            users.add(oppedPlayer);
        });

        return users;
    }

    private static JsonArray getOppedPlayersFromFile() {
        JsonArray oplist = null;
        try {
            // Read data as Json array from server directory
            oplist = (JsonArray) parser.parse(new FileReader(WhitelistSync2.SERVER_FILEPATH + "/ops.json"));
            
            // Log.debug("getOppedPlayersFromFile returned an array of " + oplist.size() + " entries.");
        } catch (FileNotFoundException e) {
            Log.error("ops.json file not found.");
            e.printStackTrace();
        } catch (JsonParseException e) {
            Log.error("ops.json parse error.");
            e.printStackTrace();
        }
        
        return oplist;
    }
    
}
