package net.rmnad.core.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.rmnad.core.Log;
import net.rmnad.core.models.BannedPlayer;
import okio.Path;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;

/**
 * Class to read json data from the server's banned-players.json file
 */
public class BannedPlayersFileReader {
    private static JsonParser parser = new JsonParser();

    public static ArrayList<BannedPlayer> getBannedPlayers() {
        Path serverRootPath = Path.get(".");

        ArrayList<BannedPlayer> bannedPlayers = new ArrayList<>();

        // Get Json data
        getBannedPlayersFromFile(serverRootPath).forEach((record) -> {
            String uuid = ((JsonObject) record).get("uuid").getAsString();
            String name = ((JsonObject) record).get("name").getAsString();
            String created = ((JsonObject) record).get("created").getAsString();
            String source = ((JsonObject) record).get("source").getAsString();
            String expires = ((JsonObject) record).get("expires").getAsString();
            String reason = ((JsonObject) record).get("reason").getAsString();

            // Create DTO
            BannedPlayer bannedPlayer = new BannedPlayer();
            bannedPlayer.setUuid(uuid);
            bannedPlayer.setName(name);
            bannedPlayer.setCreated(created);
            bannedPlayer.setSource(source);
            bannedPlayer.setExpires(expires);
            bannedPlayer.setReason(reason);

            bannedPlayers.add(bannedPlayer);
        });

        return bannedPlayers;
    }

    private static JsonArray getBannedPlayersFromFile(Path serverRootPath) {
        JsonArray bannedPlayers = null;
        try {
            // Read data as Json array from server directory
            bannedPlayers = (JsonArray) parser.parse(new FileReader(serverRootPath + "/banned-players.json"));
        } catch (FileNotFoundException e) {
            Log.error("banned-players.json file not found.", e);
        } catch (JsonParseException e) {
            Log.error("banned-players.json parse error.", e);
        }

        return bannedPlayers;
    }
}
