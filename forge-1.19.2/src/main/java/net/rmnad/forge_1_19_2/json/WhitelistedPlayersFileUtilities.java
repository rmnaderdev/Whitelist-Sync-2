package net.rmnad.forge_1_19_2.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.rmnad.forge_1_19_2.WhitelistSync2;
import net.rmnad.whitelistsync2.Log;
import net.rmnad.whitelistsync2.models.WhitelistedPlayer;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;


/**
 * Class to read json data from the server's whitelist.json file
 */
public class WhitelistedPlayersFileUtilities {

    private static JsonParser parser = new JsonParser();

    // Get Arraylist of whitelisted players on server.
    public static ArrayList<WhitelistedPlayer> getWhitelistedPlayers() {
        ArrayList<WhitelistedPlayer> users = new ArrayList<>();

        // Get Json data
        getWhitelistedPlayersFromFile().forEach((record) -> {
            String uuid = ((JsonObject) record).get("uuid").getAsString();
            String name = ((JsonObject) record).get("name").getAsString();

            // Create DTO
            WhitelistedPlayer whitelistedPlayer = new WhitelistedPlayer();
            whitelistedPlayer.setUuid(uuid);
            whitelistedPlayer.setName(name);
            whitelistedPlayer.setWhitelisted(true);


            users.add(whitelistedPlayer);
        });

        return users;
    }

    private static JsonArray getWhitelistedPlayersFromFile() {
        JsonArray whitelist = null;
        try {
            // Read data as Json array from server directory
            whitelist = (JsonArray) parser.parse(new FileReader(WhitelistSync2.SERVER_FILEPATH + "/whitelist.json"));

            // Log.debug("getWhitelistedPlayersFromFile returned an array of " + whitelist.size() + " entries.");
        } catch (FileNotFoundException e) {
            Log.error("whitelist.json file not found.");
            e.printStackTrace();
        } catch (JsonParseException e) {
            Log.error("whitelist.json parse error.");
            e.printStackTrace();
        }

        return whitelist;
    }

}
