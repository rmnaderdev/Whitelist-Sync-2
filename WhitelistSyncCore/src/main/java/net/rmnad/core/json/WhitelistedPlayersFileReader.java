package net.rmnad.core.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.rmnad.core.Log;
import net.rmnad.core.models.WhitelistedPlayer;
import okio.Path;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;


/**
 * Class to read json data from the server's whitelist.json file
 */
public class WhitelistedPlayersFileReader {

    // Get Arraylist of whitelisted players on server.
    public static ArrayList<WhitelistedPlayer> getWhitelistedPlayers() {
        Path serverRootPath = Path.get(".");

        ArrayList<WhitelistedPlayer> users = new ArrayList<>();

        // Get Json data
        getWhitelistedPlayersFromFile(serverRootPath).forEach((element) -> {
            JsonObject record = element.getAsJsonObject();

            String uuid = record.get("uuid").getAsString();
            String name = record.get("name").getAsString();

            // Create DTO
            WhitelistedPlayer whitelistedPlayer = new WhitelistedPlayer();
            whitelistedPlayer.setUuid(uuid);
            whitelistedPlayer.setName(name);


            users.add(whitelistedPlayer);
        });

        return users;
    }

    private static JsonArray getWhitelistedPlayersFromFile(Path serverRootPath) {
        JsonArray whitelist = new JsonArray();
        try (FileReader reader = new FileReader(serverRootPath + "/whitelist.json")) {
            // Read data as Json array from server directory
            whitelist = JsonParser.parseReader(reader).getAsJsonArray();

            Log.debug("getWhitelistedPlayersFromFile returned an array of " + whitelist.size() + " entries.");
        } catch (FileNotFoundException e) {
            Log.error("whitelist.json file not found.", e);
        } catch (JsonParseException | IllegalStateException e) {
            Log.error("whitelist.json parse error.", e);
        } catch (IOException e) {
            Log.error("whitelist.json read error.", e);
        }

        return whitelist;
    }

}
