package pw.twpi.whitelistsync2.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import pw.twpi.whitelistsync2.WhitelistSync2;
import pw.twpi.whitelistsync2.models.OpUser;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;


/**
 * Class to read json data for the OP list
 * @author Richard Nader, Jr. <rmnader@svsu.edu>
 */
public class OPlistRead {
    
    private static JsonParser parser = new JsonParser();

    // Get whitelisted uuids as a string array list
    public static ArrayList getOpsUUIDs() {
        ArrayList<String> uuids = new ArrayList<>();
        // OMG ITS A LAMBDA EXPRESSION!!! :D
        getOplistJson().forEach(emp -> parseToList(emp.getAsJsonObject(), uuids, "uuid"));
        return uuids;
    }

    // Get whitelisted usernames as a string array list
    public static ArrayList getOpsNames() {
        ArrayList<String> names = new ArrayList<>();
        // WOAH ITS A LAMBDA EXPRESSION!!! CRAZY COMPLEX STUFF GOIN ON RIGHT HERE!!! :D
        getOplistJson().forEach(emp -> parseToList(emp.getAsJsonObject(), names, "name"));
        return names;
    }

    // Get Arraylist of whitelisted users on server.
    public static ArrayList<OpUser> getOppedUsers() {
        ArrayList<OpUser> users = new ArrayList<>();
        // HOLY SHIT.. ANOTHER LAMBDA EXPRESSION!!!!
        getOplistJson().forEach((user) -> {
            String uuid = ((JsonObject) user).get("uuid").getAsString();
            String name = ((JsonObject) user).get("name").getAsString();
            int level = Integer.parseInt(((JsonObject) user).get("level").getAsString());
            boolean bypassesPlayerLimit = Boolean.parseBoolean(((JsonObject) user).get("level").getAsString());
            
            users.add(new OpUser(uuid, name, level, bypassesPlayerLimit, true));
        });
        return users;
    }

    private static void parseToList(JsonObject oplist, List list, String key) {
        list.add(oplist.get(key).getAsString());
    }

    private static JsonArray getOplistJson() {
        JsonArray oplist = null;
        try {
            oplist = (JsonArray) parser.parse(new FileReader(WhitelistSync2.SERVER_FILEPATH + "/ops.json"));
            
            WhitelistSync2.LOGGER.debug("getOplistJson returned an array of " + oplist.size() + " entries.");
            
        } catch (FileNotFoundException e) {
            WhitelistSync2.LOGGER.error("ops.json file not found! :O\n" + e.getMessage());
        } catch (JsonParseException e) {
            WhitelistSync2.LOGGER.error("ops.json parse error!! D:\n" + e.getMessage());
        }
        
        return oplist;
    }
    
}
