package net.rmnad.forge_1_19_2.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.rmnad.forge_1_19_2.WhitelistSync2;
import net.rmnad.whitelistsync2.models.BannedIp;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;

/**
 * Class to read json data from the server's banned-ips.json file
 */
public class BannedIpsFileUtilties {
    private static JsonParser parser = new JsonParser();

    public static ArrayList<BannedIp> getBannedIps() {
        ArrayList<BannedIp> bannedPlayers = new ArrayList<>();

        // Get Json data
        getBannedIpsFromFile().forEach((record) -> {
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

    private static JsonArray getBannedIpsFromFile() {
        JsonArray bannedPlayers = null;
        try {
            // Read data as Json array from server directory
            bannedPlayers = (JsonArray) parser.parse(new FileReader(WhitelistSync2.SERVER_FILEPATH + "/banned-ips.json"));
        } catch (FileNotFoundException e) {
            WhitelistSync2.LOGGER.error("banned-ips.json file not found.");
            e.printStackTrace();
        } catch (JsonParseException e) {
            WhitelistSync2.LOGGER.error("banned-ips.json parse error.");
            e.printStackTrace();
        }

        return bannedPlayers;
    }
}
