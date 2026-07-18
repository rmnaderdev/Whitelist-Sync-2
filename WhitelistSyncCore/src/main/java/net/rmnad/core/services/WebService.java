package net.rmnad.core.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.reactivex.rxjava3.annotations.Nullable;
import net.rmnad.core.Log;
import net.rmnad.core.WhitelistSyncCore;
import net.rmnad.core.callbacks.IServerControl;
import net.rmnad.core.json.BannedIpsFileReader;
import net.rmnad.core.json.BannedPlayersFileReader;
import net.rmnad.core.json.OppedPlayersFileReader;
import net.rmnad.core.json.WhitelistedPlayersFileReader;
import net.rmnad.core.logging.LogMessages;
import net.rmnad.core.models.BannedIp;
import net.rmnad.core.models.BannedPlayer;
import net.rmnad.core.models.OppedPlayer;
import net.rmnad.core.models.WhitelistedPlayer;
import net.rmnad.core.models.api.*;
import okhttp3.*;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class WebService implements BaseService {
    public final ApiClientHelper apiClientHelper;
    public final IServerControl serverControl;
    public final UUID serverUUID = UUID.randomUUID();

    public WebService(IServerControl serverControl) {

        String apiHost = WhitelistSyncCore.CONFIG.webApiHost;
        String apiKey = WhitelistSyncCore.CONFIG.webApiKey;

        this.apiClientHelper = new ApiClientHelper(apiHost, apiKey);
        this.serverControl = serverControl;
    }

    // --- Shared request/response plumbing -----------------------------------

    // Base request builder with the URL and the three headers every endpoint needs.
    private Request.Builder request(String path) {
        return new Request.Builder()
                .url(this.apiClientHelper.getApiHost() + path)
                .addHeader("content-type", "application/json")
                .addHeader("X-API-KEY", this.apiClientHelper.getApiKey())
                .addHeader("server-uuid", serverUUID.toString());
    }

    private static RequestBody jsonBody(JsonElement json) {
        return RequestBody.create(new Gson().toJson(json), MediaType.get("application/json"));
    }

    // GET an array of entries from the API, returning an empty array on any failure.
    @SuppressWarnings("unchecked")
    private <T> T[] getEntries(String path, Class<T[]> type, String label) {
        try {
            try (Response response = this.apiClientHelper.getClient().newCall(request(path).build()).execute()) {
                Log.debug(label + " Response Code : " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    return new Gson().fromJson(response.body().string(), type);
                }

                Log.error("Failed to get " + label + " from API. Response Code: " + response.code());
            }
        } catch (ConnectException e) {
            Log.warning(LogMessages.WARN_WhitelistSyncWebConnectException);
        } catch (IOException e) {
            Log.error("Error getting " + label + " from Web API.", e);
        }

        return (T[]) Array.newInstance(type.getComponentType(), 0);
    }

    // Execute a mutating request, returning whether the API reported success.
    private boolean execute(Request request, String errorPrefix) {
        try {
            try (Response response = this.apiClientHelper.getClient().newCall(request).execute()) {
                if (response.isSuccessful()) {
                    return true;
                }
                HandleApiNonSuccess(response, errorPrefix);
            }
        } catch (ConnectException e) {
            Log.warning(LogMessages.WARN_WhitelistSyncWebConnectException);
        } catch (IOException e) {
            Log.error(errorPrefix, e);
        }
        return false;
    }

    private boolean isAuthenticated() {
        try {
            try (Response response = this.apiClientHelper.getClient().newCall(request("/api/authentication").build()).execute()) {
                return response.isSuccessful();
            }
        } catch (ConnectException e) {
            Log.warning(LogMessages.WARN_WhitelistSyncWebConnectException);
        } catch (IOException e) {
            Log.error("Error authenticating with Web API.", e);
        }

        return false;
    }

    private WhitelistEntry[] getWhitelistEntries() {
        return getEntries("/api/whitelist", WhitelistEntry[].class, "getWhitelistEntries");
    }

    private OpEntry[] getOpEntries() {
        return getEntries("/api/op", OpEntry[].class, "getOpEntries");
    }

    private BannedPlayerEntry[] getBannedPlayerEntries() {
        return getEntries("/api/bannedplayer", BannedPlayerEntry[].class, "getBannedPlayerEntries");
    }

    private BannedIpEntry[] getBannedIpEntries() {
        return getEntries("/api/bannedip", BannedIpEntry[].class, "getBannedIpEntries");
    }

    @Override
    public boolean initializeDatabase() {
        if (this.apiClientHelper.getApiHost().isEmpty()) {
            Log.error("API Host is not set. Please set the API Host in the configuration file.");
            return false;
        }

        if (this.apiClientHelper.getApiKey().isEmpty()) {
            Log.error("API Key is not set. Please set the API Key in the configuration file.");
            return false;
        }

        if (isAuthenticated()) {
            Log.info("Connected to Web API successfully!");
            return true;
        } else {
            Log.error("Failed to authenticate with Web API. If you have not setup an API Key, you can create one on the website at "
                    + this.apiClientHelper.getApiHost() + ". Don't forget to set the API Key in the configuration file.");
            return false;
        }
    }

    @Override
    public ArrayList<WhitelistedPlayer> getWhitelistedPlayersFromDatabase() {
        ArrayList<WhitelistedPlayer> whitelistedPlayers = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        WhitelistEntry[] entries = getWhitelistEntries();

        for (WhitelistEntry entry : entries) {
            whitelistedPlayers.add(entry.toWhitelistedPlayer());
        }

        long timeTaken = System.currentTimeMillis() - startTime;
        Log.debug(LogMessages.SuccessGetWhitelistedPlayersFromDatabase(timeTaken, entries.length));

        return whitelistedPlayers;
    }

    @Override
    public ArrayList<OppedPlayer> getOppedPlayersFromDatabase() {
        ArrayList<OppedPlayer> oppedPlayers = new ArrayList<>();

        if (!WhitelistSyncCore.CONFIG.syncOpList) {
            Log.error(LogMessages.ALERT_OP_SYNC_DISABLED);
            return oppedPlayers;
        }

        long startTime = System.currentTimeMillis();
        OpEntry[] entries = getOpEntries();

        for (OpEntry entry : entries) {
            oppedPlayers.add(entry.toOppedPlayer());
        }

        long timeTaken = System.currentTimeMillis() - startTime;
        Log.debug(LogMessages.SuccessGetOppedPlayersFromDatabase(timeTaken, entries.length));

        return oppedPlayers;
    }

    @Override
    public ArrayList<BannedPlayer> getBannedPlayersFromDatabase() {
        ArrayList<BannedPlayer> bannedPlayers = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        BannedPlayerEntry[] entries = getBannedPlayerEntries();

        for (BannedPlayerEntry entry : entries) {
            bannedPlayers.add(entry.toBannedPlayer());
        }

        long timeTaken = System.currentTimeMillis() - startTime;
        Log.debug(LogMessages.SuccessGetBannedPlayersFromDatabase(timeTaken, entries.length));

        return bannedPlayers;
    }

    @Override
    public ArrayList<String> getBannedIpsFromDatabase() {
        ArrayList<String> bannedIps = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        BannedIpEntry[] entries = getBannedIpEntries();

        for (BannedIpEntry entry : entries) {
            bannedIps.add(entry.getIp());
        }

        long timeTaken = System.currentTimeMillis() - startTime;
        Log.debug(LogMessages.SuccessGetBannedIpsFromDatabase(timeTaken, entries.length));

        return bannedIps;
    }

    @Override
    public boolean pushLocalWhitelistToDatabase() {
        // TODO: Start job on thread to avoid lag?
        int records = 0;
        long startTime = System.currentTimeMillis();

        ArrayList<WhitelistedPlayer> whitelistedPlayers
                = WhitelistedPlayersFileReader.getWhitelistedPlayers();

        JsonArray jsonArray = new JsonArray();
        for (WhitelistedPlayer player : whitelistedPlayers) {
            JsonObject json = new JsonObject();
            json.addProperty("uuid", player.getUuid());
            json.addProperty("name", player.getName());
            jsonArray.add(json);
            records++;
        }

        Request request = request("/api/whitelist/push").post(jsonBody(jsonArray)).build();
        if (execute(request, "Error pushing local whitelist to database!")) {
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug(LogMessages.SuccessPushLocalWhitelistToDatabase(timeTaken, records));
            return true;
        }

        return false;
    }

    @Override
    public boolean pushLocalOpsToDatabase() {
        if (!WhitelistSyncCore.CONFIG.syncOpList) {
            Log.error(LogMessages.ALERT_OP_SYNC_DISABLED);
            return false;
        }

        // TODO: Start job on thread to avoid lag?
        int records = 0;
        long startTime = System.currentTimeMillis();

        ArrayList<OppedPlayer> oppedPlayers
                = OppedPlayersFileReader.getOppedPlayers();

        JsonArray jsonArray = new JsonArray();
        for (OppedPlayer player : oppedPlayers) {
            JsonObject json = new JsonObject();
            json.addProperty("uuid", player.getUuid());
            json.addProperty("name", player.getName());
            jsonArray.add(json);
            records++;
        }

        Request request = request("/api/op/push").post(jsonBody(jsonArray)).build();
        if (execute(request, "Error pushing local ops to database!")) {
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug(LogMessages.SuccessPushLocalOpsToDatabase(timeTaken, records));
            return true;
        }

        return false;
    }

    public boolean pushLocalBannedPlayersToDatabase() {
        if (!WhitelistSyncCore.CONFIG.webSyncBannedPlayers) {
            Log.error(LogMessages.ALERT_BANNED_PLAYERS_SYNC_DISABLED);
            return false;
        }

        int records = 0;
        long startTime = System.currentTimeMillis();

        ArrayList<BannedPlayer> bannedPlayers
                = BannedPlayersFileReader.getBannedPlayers();

        JsonArray jsonArray = new JsonArray();
        for (BannedPlayer player : bannedPlayers) {
            JsonObject json = new JsonObject();
            json.addProperty("uuid", player.getUuid());
            json.addProperty("name", player.getName());
            json.addProperty("reason", player.getReason());
            jsonArray.add(json);
            records++;
        }

        Request request = request("/api/bannedplayer/push").post(jsonBody(jsonArray)).build();
        if (execute(request, "Error pushing local banned players to database!")) {
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug(LogMessages.SuccessPushLocalBannedPlayersToDatabase(timeTaken, records));
            return true;
        }

        return false;
    }

    @Override
    public boolean pushLocalBannedIpsToDatabase() {
        if (!WhitelistSyncCore.CONFIG.webSyncBannedIps) {
            Log.error(LogMessages.ALERT_BANNED_IPS_SYNC_DISABLED);
            return false;
        }

        int records = 0;
        long startTime = System.currentTimeMillis();

        ArrayList<BannedIp> bannedIps
                = BannedIpsFileReader.getBannedIps();

        JsonArray jsonArray = new JsonArray();
        for (BannedIp ip : bannedIps) {
            JsonObject json = new JsonObject();
            json.addProperty("ip", ip.getIp());
            json.addProperty("reason", ip.getReason());
            jsonArray.add(json);
            records++;
        }

        Request request = request("/api/bannedip/push").post(jsonBody(jsonArray)).build();
        if (execute(request, "Error pushing local banned ips to database!")) {
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug(LogMessages.SuccessPushLocalBannedIpsToDatabase(timeTaken, records));
            return true;
        }

        return false;
    }

    @Override
    public boolean pullDatabaseWhitelistToLocal() {
        int records = 0;
        long startTime = System.currentTimeMillis();

        ArrayList<WhitelistedPlayer> localWhitelistedPlayers
                = WhitelistedPlayersFileReader.getWhitelistedPlayers();

        Set<String> localUuids = new HashSet<>();
        for (WhitelistedPlayer player : localWhitelistedPlayers) {
            if (player.getUuid() != null) {
                localUuids.add(player.getUuid());
            }
        }

        WhitelistEntry[] entries = getWhitelistEntries();

        for (WhitelistEntry player : entries) {
            UUID uuid = UUID.fromString(player.getUuid());
            String name = player.getName();
            boolean whitelisted = player.getWhitelisted();

            if (whitelisted) {
                if (!localUuids.contains(uuid.toString())) {
                    try {
                        this.serverControl.addWhitelistPlayer(uuid, name);
                        Log.debug(LogMessages.AddedUserToWhitelist(name));
                        records++;
                    } catch (NullPointerException e) {
                        Log.error(e.getMessage(), e);
                    }
                }
            } else {
                if (localUuids.contains(uuid.toString())) {
                    this.serverControl.removeWhitelistPlayer(uuid, name);
                    Log.debug(LogMessages.RemovedUserToWhitelist(name));
                    records++;
                }
            }
        }

        long timeTaken = System.currentTimeMillis() - startTime;
        Log.debug(LogMessages.SuccessPullDatabaseWhitelistToLocal( timeTaken, records));

        return true;
    }

    @Override
    public boolean pullDatabaseOpsToLocal() {
        if (!WhitelistSyncCore.CONFIG.syncOpList) {
            Log.error(LogMessages.ALERT_OP_SYNC_DISABLED);
            return false;
        }

        int records = 0;
        long startTime = System.currentTimeMillis();

        ArrayList<OppedPlayer> localOppedPlayers
                = OppedPlayersFileReader.getOppedPlayers();

        Set<String> localUuids = new HashSet<>();
        for (OppedPlayer player : localOppedPlayers) {
            if (player.getUuid() != null) {
                localUuids.add(player.getUuid());
            }
        }

        OpEntry[] entries = getOpEntries();

        for (OpEntry player : entries) {
            UUID uuid = UUID.fromString(player.getUuid());
            String name = player.getName();
            boolean opped = player.getOpped();

            if (opped) {
                if (!localUuids.contains(uuid.toString())) {
                    try {
                        this.serverControl.addOpPlayer(uuid, name);
                        Log.debug(LogMessages.OppedUser(name));
                        records++;
                    } catch (NullPointerException e) {
                        Log.error(e.getMessage(), e);
                    }
                }
            } else {
                if (localUuids.contains(uuid.toString())) {
                    this.serverControl.removeOpPlayer(uuid, name);
                    Log.debug(LogMessages.DeopUser(name));
                    records++;
                }
            }
        }

        long timeTaken = System.currentTimeMillis() - startTime;
        Log.debug(LogMessages.SuccessPullDatabaseOpsToLocal(timeTaken, records));

        return true;
    }

    @Override
    public boolean pullDatabaseBannedPlayersToLocal() {
        if (!WhitelistSyncCore.CONFIG.webSyncBannedPlayers) {
            Log.error(LogMessages.ALERT_BANNED_PLAYERS_SYNC_DISABLED);
            return false;
        }

        int records = 0;
        long startTime = System.currentTimeMillis();

        ArrayList<BannedPlayer> localBannedPlayers
                = BannedPlayersFileReader.getBannedPlayers();

        Set<String> localUuids = new HashSet<>();
        for (BannedPlayer player : localBannedPlayers) {
            if (player.getUuid() != null) {
                localUuids.add(player.getUuid());
            }
        }

        BannedPlayerEntry[] entries = getBannedPlayerEntries();

        for (BannedPlayerEntry player : entries) {
            UUID uuid = UUID.fromString(player.getUuid());
            String name = player.getName();
            String reason = player.getReason();

            if (!localUuids.contains(uuid.toString())) {
                try {
                    this.serverControl.addBannedPlayer(uuid, name, reason);
                    Log.debug(LogMessages.BannedPlayer(name));
                    records++;
                } catch (NullPointerException e) {
                    Log.error(e.getMessage(), e);
                }
            }
        }

        long timeTaken = System.currentTimeMillis() - startTime;
        Log.debug(LogMessages.SuccessPullDatabaseBannedPlayersToLocal(timeTaken, records));

        return true;
    }

    @Override
    public boolean pullDatabaseBannedIpsToLocal() {
        if (!WhitelistSyncCore.CONFIG.webSyncBannedIps) {
            Log.error(LogMessages.ALERT_BANNED_IPS_SYNC_DISABLED);
            return false;
        }

        int records = 0;
        long startTime = System.currentTimeMillis();

        ArrayList<BannedIp> localBannedIps
                = BannedIpsFileReader.getBannedIps();

        Set<String> localIps = new HashSet<>();
        for (BannedIp bannedIp : localBannedIps) {
            if (bannedIp.getIp() != null) {
                localIps.add(bannedIp.getIp());
            }
        }

        BannedIpEntry[] entries = getBannedIpEntries();

        for (BannedIpEntry ip : entries) {
            if (!localIps.contains(ip.getIp())) {
                try {
                    this.serverControl.addBannedIp(ip.getIp(), ip.getReason());
                    Log.debug(LogMessages.BannedIp(ip.getIp()));
                    records++;
                } catch (NullPointerException e) {
                    Log.error(e.getMessage(), e);
                }
            }
        }

        long timeTaken = System.currentTimeMillis() - startTime;
        Log.debug(LogMessages.SuccessPullDatabaseBannedIpsToLocal(timeTaken, records));

        return true;
    }

    @Override
    public boolean addWhitelistPlayer(UUID uuid, String name) {
        long startTime = System.currentTimeMillis();

        JsonObject json = new JsonObject();
        json.addProperty("uuid", uuid.toString());
        json.addProperty("name", name);

        Request request = request("/api/whitelist").post(jsonBody(json)).build();
        if (execute(request, "Error adding " + name + " to whitelist database!")) {
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug("Added " + name + " to whitelist | Took " + timeTaken + "ms");
            return true;
        }

        return false;
    }

    @Override
    public boolean addOppedPlayer(UUID uuid, String name) {
        if (!WhitelistSyncCore.CONFIG.syncOpList) {
            Log.error(LogMessages.ALERT_OP_SYNC_DISABLED);
            return false;
        }

        long startTime = System.currentTimeMillis();

        JsonObject json = new JsonObject();
        json.addProperty("uuid", uuid.toString());
        json.addProperty("name", name);

        Request request = request("/api/op").post(jsonBody(json)).build();
        if (execute(request, "Error opping " + name + " in database!")) {
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug("Opped " + name + " | Took " + timeTaken + "ms");
            return true;
        }

        return false;
    }

    @Override
    public boolean removeWhitelistPlayer(UUID uuid, String name) {
        long startTime = System.currentTimeMillis();

        Request request = request("/api/whitelist/" + uuid.toString()).delete().build();
        if (execute(request, "Error removing " + name + " from whitelist database!")) {
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug("Removed " + name + " from whitelist | Took " + timeTaken + "ms");
            return true;
        }

        return false;
    }

    @Override
    public boolean removeOppedPlayer(UUID uuid, String name) {
        if (!WhitelistSyncCore.CONFIG.syncOpList) {
            Log.error(LogMessages.ALERT_OP_SYNC_DISABLED);
            return false;
        }

        long startTime = System.currentTimeMillis();

        Request request = request("/api/op/" + uuid.toString()).delete().build();
        if (execute(request, "Error deopping " + name + " in database!")) {
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug("Deopped " + name + " | Took " + timeTaken + "ms");
            return true;
        }

        return false;
    }

    @Override
    public boolean addBannedPlayer(UUID uuid, String name, @Nullable String reason) {
        if (!WhitelistSyncCore.CONFIG.webSyncBannedPlayers) {
            Log.error(LogMessages.ALERT_BANNED_PLAYERS_SYNC_DISABLED);
            return false;
        }

        long startTime = System.currentTimeMillis();

        JsonObject json = new JsonObject();
        json.addProperty("uuid", uuid.toString());
        json.addProperty("name", name);
        json.addProperty("reason", reason);

        Request request = request("/api/bannedplayer").post(jsonBody(json)).build();
        if (execute(request, "Error banning " + name + " in database!")) {
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug("Banned " + name + " | Took " + timeTaken + "ms");
            return true;
        }

        return false;
    }

    @Override
    public boolean addBannedIp(String ip, @Nullable String reason) {
        if (!WhitelistSyncCore.CONFIG.webSyncBannedIps) {
            Log.error(LogMessages.ALERT_BANNED_IPS_SYNC_DISABLED);
            return false;
        }

        long startTime = System.currentTimeMillis();

        JsonObject json = new JsonObject();
        json.addProperty("ip", ip);
        json.addProperty("reason", reason);

        Request request = request("/api/bannedip").post(jsonBody(json)).build();
        if (execute(request, "Error banning " + ip + " in database!")) {
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug("Banned " + ip + " | Took " + timeTaken + "ms");
            return true;
        }

        return false;
    }

    @Override
    public boolean removeBannedPlayer(UUID uuid, String name) {
        if (!WhitelistSyncCore.CONFIG.webSyncBannedPlayers) {
            Log.error(LogMessages.ALERT_BANNED_PLAYERS_SYNC_DISABLED);
            return false;
        }

        long startTime = System.currentTimeMillis();

        Request request = request("/api/bannedplayer/" + uuid.toString()).delete().build();
        if (execute(request, "Error unbanning " + name + " in database!")) {
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug("Unbanned " + name + " | Took " + timeTaken + "ms");
            return true;
        }

        return false;
    }

    @Override
    public boolean removeBannedIp(String ip) {
        if (!WhitelistSyncCore.CONFIG.webSyncBannedIps) {
            Log.error(LogMessages.ALERT_BANNED_IPS_SYNC_DISABLED);
            return false;
        }

        long startTime = System.currentTimeMillis();

        Request request = request("/api/bannedip/" + ip).delete().build();
        if (execute(request, "Error unbanning " + ip + " in database!")) {
            long timeTaken = System.currentTimeMillis() - startTime;
            Log.debug("Unbanned " + ip + " | Took " + timeTaken + "ms");
            return true;
        }

        return false;
    }

    private static void HandleApiNonSuccess(Response response, String messagePrefix) {
        if (!response.isSuccessful() && response.body() != null) {
            Gson gson = new Gson();
            try {
                ErrorMessage error = gson.fromJson(response.body().string(), ErrorMessage.class);
                Log.error(messagePrefix + " Error: " + error.getMessage());
                return;
            } catch (IOException ignored) {}

            Log.error(messagePrefix + " Response Code: " + response.code());
        }
    }
}
