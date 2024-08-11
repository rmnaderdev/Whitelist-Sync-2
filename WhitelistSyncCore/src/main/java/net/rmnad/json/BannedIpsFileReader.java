package net.rmnad.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.rmnad.Log;
import net.rmnad.models.BannedIp;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;

/**
 * Class to read json data from the server's banned-ips.json file
 */
public class BannedIpsFileReader {
    private static JsonParser parser = new JsonParser();

    public static ArrayList<BannedIp> getBannedIps(String serverRootPath) {
        ArrayList<BannedIp> bannedPlayers = new ArrayList<>();

        // Get Json data
        getBannedIpsFromFile(serverRootPath).forEach((record) -> {
            String ip = ((JsonObject) record).get("ip").getAsString();
            String created = ((JsonObject) record).get("created").getAsString();
            String source = ((JsonObject) record).get("source").getAsString();
            String expires = ((JsonObject) record).get("expires").getAsString();
            String reason = ((JsonObject) record).get("reason").getAsString();

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

    private static JsonArray getBannedIpsFromFile(String serverRootPath) {
        JsonArray bannedPlayers = null;
        try {
            // Read data as Json array from server directory
            bannedPlayers = (JsonArray) parser.parse(new FileReader(serverRootPath + "/banned-ips.json"));
        } catch (FileNotFoundException e) {
            Log.error("banned-ips.json file not found.", e);
        } catch (JsonParseException e) {
            Log.error("banned-ips.json parse error.", e);
        }

        return bannedPlayers;
    }
}
