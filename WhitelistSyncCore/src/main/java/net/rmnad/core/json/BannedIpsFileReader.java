package net.rmnad.core.json;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.rmnad.core.Log;
import net.rmnad.core.models.BannedIp;
import okio.Path;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Class to read json data from the server's banned-ips.json file
 */
public class BannedIpsFileReader {

    public static ArrayList<BannedIp> getBannedIps() {
        Path serverRootPath = Path.get(".");

        ArrayList<BannedIp> bannedPlayers = new ArrayList<>();

        // Get Json data
        getBannedIpsFromFile(serverRootPath).forEach((element) -> {
            JsonObject record = element.getAsJsonObject();

            String ip = record.get("ip").getAsString();
            String created = record.get("created").getAsString();
            String source = record.get("source").getAsString();
            String expires = record.get("expires").getAsString();
            String reason = record.has("reason") ? record.get("reason").getAsString() : null;

            // Create DTO
            BannedIp bannedPlayer = new BannedIp();
            bannedPlayer.setIp(ip);
            bannedPlayer.setCreated(created);
            bannedPlayer.setSource(source);
            bannedPlayer.setExpires(expires);
            bannedPlayer.setReason(reason);

            bannedPlayers.add(bannedPlayer);
        });

        return bannedPlayers;
    }

    private static JsonArray getBannedIpsFromFile(Path serverRootPath) {
        JsonArray bannedPlayers = new JsonArray();
        try (FileReader reader = new FileReader(serverRootPath + "/banned-ips.json")) {
            // Read data as Json array from server directory
            JsonArray parsed = new Gson().fromJson(reader, JsonArray.class);
            if (parsed != null) {
                bannedPlayers = parsed;
            }
        } catch (FileNotFoundException e) {
            Log.error("banned-ips.json file not found.", e);
        } catch (JsonParseException | IllegalStateException e) {
            Log.error("banned-ips.json parse error.", e);
        } catch (IOException e) {
            Log.error("banned-ips.json read error.", e);
        }

        return bannedPlayers;
    }
}
