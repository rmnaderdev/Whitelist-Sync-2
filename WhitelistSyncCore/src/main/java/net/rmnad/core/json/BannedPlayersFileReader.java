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
import java.io.IOException;
import java.util.ArrayList;

/**
 * Class to read json data from the server's banned-players.json file
 */
public class BannedPlayersFileReader {

    public static ArrayList<BannedPlayer> getBannedPlayers() {
        Path serverRootPath = Path.get(".");

        ArrayList<BannedPlayer> bannedPlayers = new ArrayList<>();

        // Get Json data
        getBannedPlayersFromFile(serverRootPath).forEach((element) -> {
            JsonObject record = element.getAsJsonObject();

            String uuid = record.get("uuid").getAsString();
            String name = record.get("name").getAsString();
            String created = record.get("created").getAsString();
            String source = record.get("source").getAsString();
            String expires = record.get("expires").getAsString();
            String reason = record.has("reason") ? record.get("reason").getAsString() : null;

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
        JsonArray bannedPlayers = new JsonArray();
        try (FileReader reader = new FileReader(serverRootPath + "/banned-players.json")) {
            // Read data as Json array from server directory
            bannedPlayers = JsonParser.parseReader(reader).getAsJsonArray();
        } catch (FileNotFoundException e) {
            Log.error("banned-players.json file not found.", e);
        } catch (JsonParseException | IllegalStateException e) {
            Log.error("banned-players.json parse error.", e);
        } catch (IOException e) {
            Log.error("banned-players.json read error.", e);
        }

        return bannedPlayers;
    }
}
