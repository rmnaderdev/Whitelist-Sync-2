package net.rmnad.core.json;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.rmnad.core.Log;
import net.rmnad.core.models.OppedPlayer;
import okio.Path;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;


/**
 * Class to read json data from the server's ops.json file
 */
public class OppedPlayersFileReader {

    // Get Arraylist of opped players on server.
    public static ArrayList<OppedPlayer> getOppedPlayers() {
        Path serverRootPath = Path.get(".");

        ArrayList<OppedPlayer> users = new ArrayList<>();

        // Get Json data
        getOppedPlayersFromFile(serverRootPath).forEach((element) -> {
            JsonObject record = element.getAsJsonObject();

            String uuid = record.get("uuid").getAsString();
            String name = record.get("name").getAsString();
            int level = record.get("level").getAsInt();
            boolean bypassesPlayerLimit = record.get("bypassesPlayerLimit").getAsBoolean();

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

    private static JsonArray getOppedPlayersFromFile(Path serverRootPath) {
        JsonArray oplist = new JsonArray();
        try (FileReader reader = new FileReader(serverRootPath + "/ops.json")) {
            // Read data as Json array from server directory
            JsonArray parsed = new Gson().fromJson(reader, JsonArray.class);
            if (parsed != null) {
                oplist = parsed;
            }

            Log.debug("getOppedPlayersFromFile returned an array of " + oplist.size() + " entries.");
        } catch (FileNotFoundException e) {
            Log.error("ops.json file not found.", e);
        } catch (JsonParseException | IllegalStateException e) {
            Log.error("ops.json parse error.", e);
        } catch (IOException e) {
            Log.error("ops.json read error.", e);
        }

        return oplist;
    }
    
}
