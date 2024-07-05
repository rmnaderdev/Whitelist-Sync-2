package net.rmnad.whitelistsync2.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.rmnad.whitelistsync2.WhitelistSync2;
import net.rmnad.whitelistsync2.models.BannedPlayer;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;

/**
 * Class to read json data from the server's banned-players.json file
 */
public class BannedPlayersFileUtilties {
    private static JsonParser parser = new JsonParser();

    public static ArrayList<BannedPlayer> getBannedPlayers() {
        ArrayList<BannedPlayer> bannedPlayers = new ArrayList<>();

        // Get Json data
        getBannedPlayersFromFile().forEach((record) -> {
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

    private static JsonArray getBannedPlayersFromFile() {
        JsonArray bannedPlayers = null;
        try {
            // Read data as Json array from server directory
            bannedPlayers = (JsonArray) parser.parse(new FileReader(WhitelistSync2.SERVER_FILEPATH + "/banned-players.json"));
        } catch (FileNotFoundException e) {
            WhitelistSync2.LOGGER.error("banned-players.json file not found.");
            e.printStackTrace();
        } catch (JsonParseException e) {
            WhitelistSync2.LOGGER.error("banned-players.json parse error.");
            e.printStackTrace();
        }

        return bannedPlayers;
    }
}
