package net.rmnad.core.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.rmnad.core.Log;
import net.rmnad.core.models.WhitelistedPlayer;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;


/**
 * Class to read json data from the server's whitelist.json file
 */
public class WhitelistedPlayersFileReader {

    private static JsonParser parser = new JsonParser();

    // Get Arraylist of whitelisted players on server.
    public static ArrayList<WhitelistedPlayer> getWhitelistedPlayers(String serverRootPath) {
        ArrayList<WhitelistedPlayer> users = new ArrayList<>();

        // Get Json data
        getWhitelistedPlayersFromFile(serverRootPath).forEach((record) -> {
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

    private static JsonArray getWhitelistedPlayersFromFile(String serverRootPath) {
        JsonArray whitelist = null;
        try {
            // Read data as Json array from server directory
            whitelist = (JsonArray) parser.parse(new FileReader(serverRootPath + "/whitelist.json"));

            Log.debug("getWhitelistedPlayersFromFile returned an array of " + whitelist.size() + " entries.");
        } catch (FileNotFoundException e) {
            Log.error("whitelist.json file not found.", e);
        } catch (JsonParseException e) {
            Log.error("whitelist.json parse error.", e);
        }

        return whitelist;
    }

}
