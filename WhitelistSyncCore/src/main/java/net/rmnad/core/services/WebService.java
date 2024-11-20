package net.rmnad.core.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
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
import java.net.ConnectException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
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

    private boolean isAuthenticated() {
        try {
            OkHttpClient client = this.apiClientHelper.getClient();

            Request request = new Request.Builder()
                    .url(this.apiClientHelper.getApiHost() + "/api/authentication")
                    .addHeader("content-type", "application/json")
                    .addHeader("X-API-KEY", this.apiClientHelper.getApiKey())
                    .addHeader("server-uuid", serverUUID.toString())
                    .build();

            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (ConnectException e) {
            Log.warning(LogMessages.WARN_WhitelistSyncWebConnectException);
        }catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            Log.error("Error authenticating with Web API.", e);
        }

        return false;
    }

    private WhitelistEntry[] getWhitelistEntries() {
        try {
            OkHttpClient client = this.apiClientHelper.getClient();
            Request request = new Request.Builder()
                    .url(this.apiClientHelper.getApiHost() + "/api/whitelist")
                    .addHeader("content-type", "application/json")
                    .addHeader("X-API-KEY", this.apiClientHelper.getApiKey())
                    .addHeader("server-uuid", serverUUID.toString())
                    .build();

            try (Response response = client.newCall(request).execute()) {
                Log.debug("getWhitelistEntries Response Code : " + response.code());

                if (response.isSuccessful()) {
                    Gson gson = new Gson();

                    if (response.body() != null) {
                        return gson.fromJson(response.body().string(), WhitelistEntry[].class);
                    }
                }

                Log.error("Failed to get whitelist entries from API. Response Code: " + response.code());
            }

        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            Log.error("Error authenticating with Web API.", e);
        }

        return new WhitelistEntry[0];
    }

    private OpEntry[] getOpEntries() {
        try {
            OkHttpClient client = this.apiClientHelper.getClient();
            Request request = new Request.Builder()
                    .url(this.apiClientHelper.getApiHost() + "/api/op")
                    .addHeader("content-type", "application/json")
                    .addHeader("X-API-KEY", this.apiClientHelper.getApiKey())
                    .addHeader("server-uuid", serverUUID.toString())
                    .build();

            try (Response response = client.newCall(request).execute()) {
                Log.debug("getOpEntries Response Code : " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    Gson gson = new Gson();
                    return gson.fromJson(response.body().string(), OpEntry[].class);
                }

                Log.error("Failed to get op entries from API. Response Code: " + response.code());
            }

        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            Log.error("Error getting OP entries from Web API.", e);
        }

        return new OpEntry[0];
    }

    private BannedPlayerEntry[] getBannedPlayerEntries() {
        try {
            OkHttpClient client = this.apiClientHelper.getClient();
            Request request = new Request.Builder()
                    .url(this.apiClientHelper.getApiHost() + "/api/bannedplayer")
                    .addHeader("content-type", "application/json")
                    .addHeader("X-API-KEY", this.apiClientHelper.getApiKey())
                    .addHeader("server-uuid", serverUUID.toString())
                    .build();

            try (Response response = client.newCall(request).execute()) {
                Log.debug("getBannedPlayerEntries Response Code : " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    Gson gson = new Gson();
                    return gson.fromJson(response.body().string(), BannedPlayerEntry[].class);
                }

                Log.error("Failed to get banned player entries from API. Response Code: " + response.code());
            }

        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            Log.error("Error getting banned player entries from Web API.", e);
        }

        return new BannedPlayerEntry[0];
    }

    private BannedIpEntry[] getBannedIpEntries() {
        try {
            OkHttpClient client = this.apiClientHelper.getClient();
            Request request = new Request.Builder()
                    .url(this.apiClientHelper.getApiHost() + "/api/bannedip")
                    .addHeader("content-type", "application/json")
                    .addHeader("X-API-KEY", this.apiClientHelper.getApiKey())
                    .addHeader("server-uuid", serverUUID.toString())
                    .build();

            try (Response response = client.newCall(request).execute()) {
                Log.debug("getBannedIpEntries Response Code : " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    Gson gson = new Gson();
                    return gson.fromJson(response.body().string(), BannedIpEntry[].class);
                }

                Log.error("Failed to get banned ip entries from API. Response Code: " + response.code());
            }

        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            Log.error("Error getting banned ip entries from Web API.", e);
        }

        return new BannedIpEntry[0];
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

        try {
            OkHttpClient client = this.apiClientHelper.getClient();

            // Set body of request
            Gson gson = new Gson();
            JsonArray jsonArray = new JsonArray();
            for (WhitelistedPlayer player : whitelistedPlayers) {
                JsonObject json = new JsonObject();
                json.addProperty("uuid", player.getUuid());
                json.addProperty("name", player.getName());
                jsonArray.add(json);
                records++;
            }
            String jsonBody = gson.toJson(jsonArray);
            RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json"));

            Request request = new Request.Builder()
                    .url(this.apiClientHelper.getApiHost() + "/api/whitelist/push")
                    .addHeader("content-type", "application/json")
                    .addHeader("X-API-KEY", this.apiClientHelper.getApiKey())
                    .addHeader("server-uuid", serverUUID.toString())
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {

                if (response.isSuccessful()) {
                    // Record time taken.
                    long timeTaken = System.currentTimeMillis() - startTime;
                    Log.debug(LogMessages.SuccessPushLocalWhitelistToDatabase(timeTaken, records));
                    return true;
                } else {
                    HandleApiNonSuccess(response, "Error pushing local whitelist to database!");
                }
            }
        } catch (ConnectException e) {
            Log.warning(LogMessages.WARN_WhitelistSyncWebConnectException);
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            Log.error(LogMessages.ERROR_PushLocalWhitelistToDatabase, e);
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

        try {
            OkHttpClient client = this.apiClientHelper.getClient();

            // Set body of request
            Gson gson = new Gson();
            JsonArray jsonArray = new JsonArray();
            for (OppedPlayer player : oppedPlayers) {
                JsonObject json = new JsonObject();
                json.addProperty("uuid", player.getUuid());
                json.addProperty("name", player.getName());
                jsonArray.add(json);
                records++;
            }
            String jsonBody = gson.toJson(jsonArray);
            RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json"));

            Request request = new Request.Builder()
                    .url(this.apiClientHelper.getApiHost() + "/api/op/push")
                    .addHeader("content-type", "application/json")
                    .addHeader("X-API-KEY", this.apiClientHelper.getApiKey())
                    .addHeader("server-uuid", serverUUID.toString())
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {

                if (response.isSuccessful()) {
                    // Record time taken.
                    long timeTaken = System.currentTimeMillis() - startTime;
                    Log.debug(LogMessages.SuccessPushLocalWhitelistToDatabase(timeTaken, records));
                    return true;
                } else {
                    HandleApiNonSuccess(response, "Error pushing local ops to database!");
                }
            }
        } catch (ConnectException e) {
            Log.warning(LogMessages.WARN_WhitelistSyncWebConnectException);
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            Log.error(LogMessages.ERROR_PushLocalOpsToDatabase, e);
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

        try {
            OkHttpClient client = this.apiClientHelper.getClient();

            // Set body of request
            Gson gson = new Gson();
            JsonArray jsonArray = new JsonArray();
            for (BannedPlayer player : bannedPlayers) {
                JsonObject json = new JsonObject();
                json.addProperty("uuid", player.getUuid());
                json.addProperty("name", player.getName());
                json.addProperty("reason", player.getReason());
                jsonArray.add(json);
                records++;
            }
            String jsonBody = gson.toJson(jsonArray);
            RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json"));

            Request request = new Request.Builder()
                    .url(this.apiClientHelper.getApiHost() + "/api/bannedplayer/push")
                    .addHeader("content-type", "application/json")
                    .addHeader("X-API-KEY", this.apiClientHelper.getApiKey())
                    .addHeader("server-uuid", serverUUID.toString())
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {

                if (response.isSuccessful()) {
                    // Record time taken.
                    long timeTaken = System.currentTimeMillis() - startTime;
                    Log.debug(LogMessages.SuccessPushLocalBannedPlayersToDatabase(timeTaken, records));
                    return true;
                } else {
                    HandleApiNonSuccess(response, "Error pushing local banned players to database!");
                }
            }
        } catch (ConnectException e) {
            Log.warning(LogMessages.WARN_WhitelistSyncWebConnectException);
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            Log.error(LogMessages.ERROR_PushLocalBannedPlayersToDatabase, e);
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

        try {
            OkHttpClient client = this.apiClientHelper.getClient();

            // Set body of request
            Gson gson = new Gson();
            JsonArray jsonArray = new JsonArray();
            for (BannedIp ip : bannedIps) {
                JsonObject json = new JsonObject();
                json.addProperty("ip", ip.getIp());
                json.addProperty("reason", ip.getReason());
                jsonArray.add(json);
                records++;
            }
            String jsonBody = gson.toJson(jsonArray);
            RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json"));

            Request request = new Request.Builder()
                    .url(this.apiClientHelper.getApiHost() + "/api/bannedip/push")
                    .addHeader("content-type", "application/json")
                    .addHeader("X-API-KEY", this.apiClientHelper.getApiKey())
                    .addHeader("server-uuid", serverUUID.toString())
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {

                if (response.isSuccessful()) {
                    // Record time taken.
                    long timeTaken = System.currentTimeMillis() - startTime;
                    Log.debug(LogMessages.SuccessPushLocalBannedIpsToDatabase(timeTaken, records));
                    return true;
                } else {
                    HandleApiNonSuccess(response, "Error pushing local banned ips to database!");
                }
            }
        } catch (ConnectException e) {
            Log.warning(LogMessages.WARN_WhitelistSyncWebConnectException);
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            Log.error(LogMessages.ERROR_PushLocalBannedIpsToDatabase, e);
        }

        return false;
    }

    @Override
    public boolean pullDatabaseWhitelistToLocal() {
        int records = 0;
        long startTime = System.currentTimeMillis();

        ArrayList<WhitelistedPlayer> localWhitelistedPlayers
                = WhitelistedPlayersFileReader.getWhitelistedPlayers();

        WhitelistEntry[] entries = getWhitelistEntries();

        for (WhitelistEntry player : entries) {
            UUID uuid = UUID.fromString(player.getUuid());
            String name = player.getName();
            boolean whitelisted = player.getWhitelisted();

            if (whitelisted) {
                if (localWhitelistedPlayers.stream().noneMatch(o -> o.getUuid().equals(uuid.toString()))) {
                    try {
                        this.serverControl.addWhitelistPlayer(uuid, name);
                        Log.debug(LogMessages.AddedUserToWhitelist(name));
                        records++;
                    } catch (NullPointerException e) {
                        Log.error(e.getMessage(), e);
                    }
                }
            } else {
                if (localWhitelistedPlayers.stream().anyMatch(o -> o.getUuid().equals(uuid.toString()))) {
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

        OpEntry[] entries = getOpEntries();

        for (OpEntry player : entries) {
            UUID uuid = UUID.fromString(player.getUuid());
            String name = player.getName();
            boolean opped = player.getOpped();

            if (opped) {
                if (localOppedPlayers.stream().noneMatch(o -> o.getUuid().equals(uuid.toString()))) {
                    try {
                        this.serverControl.addOpPlayer(uuid, name);
                        Log.debug(LogMessages.OppedUser(name));
                        records++;
                    } catch (NullPointerException e) {
                        Log.error(e.getMessage(), e);
                    }
                }
            } else {
                if (localOppedPlayers.stream().anyMatch(o -> o.getUuid().equals(uuid.toString()))) {
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

        BannedPlayerEntry[] entries = getBannedPlayerEntries();

        for (BannedPlayerEntry player : entries) {
            UUID uuid = UUID.fromString(player.getUuid());
            String name = player.getName();
            String reason = player.getReason();

            if (localBannedPlayers.stream().noneMatch(o -> o.getUuid().equals(uuid.toString()))) {
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

        BannedIpEntry[] entries = getBannedIpEntries();

        for (BannedIpEntry ip : entries) {
            if (localBannedIps.stream().noneMatch(o -> o.getIp().equals(ip.getIp()))) {
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
        try {
            // Set body of request
            Gson gson = new Gson();
            JsonObject json = new JsonObject();
            json.addProperty("uuid", uuid.toString());
            json.addProperty("name", name);
            String jsonBody = gson.toJson(json);
            RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json"));

            OkHttpClient client = this.apiClientHelper.getClient();
            Request request = new Request.Builder()
                    .url(this.apiClientHelper.getApiHost() + "/api/whitelist")
                    .addHeader("content-type", "application/json")
                    .addHeader("X-API-KEY", this.apiClientHelper.getApiKey())
                    .addHeader("server-uuid", serverUUID.toString())
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {

                if (response.isSuccessful()) {
                    long timeTaken = System.currentTimeMillis() - startTime;
                    Log.debug("Added " + name + " to whitelist | Took " + timeTaken + "ms");

                    return true;
                } else {
                    HandleApiNonSuccess(response, "Error adding " + name + " to whitelist database!");
                }
            }

        } catch (ConnectException e) {
            Log.warning(LogMessages.WARN_WhitelistSyncWebConnectException);
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            Log.error("Error adding " + name + " to whitelist database!", e);
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
        try {
            OkHttpClient client = this.apiClientHelper.getClient();
            Gson gson = new Gson();
            JsonObject json = new JsonObject();
            json.addProperty("uuid", uuid.toString());
            json.addProperty("name", name);
            String jsonBody = gson.toJson(json);
            RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json"));

            Request request = new Request.Builder()
                    .url(this.apiClientHelper.getApiHost() + "/api/op")
                    .addHeader("content-type", "application/json")
                    .addHeader("X-API-KEY", this.apiClientHelper.getApiKey())
                    .addHeader("server-uuid", serverUUID.toString())
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {

                if (response.isSuccessful()) {
                    long timeTaken = System.currentTimeMillis() - startTime;
                    Log.debug("Opped " + name + " | Took " + timeTaken + "ms");

                    return true;
                } else {
                    HandleApiNonSuccess(response, "Error opping " + name + " in database!");
                }
            }

        } catch (ConnectException e) {
            Log.warning(LogMessages.WARN_WhitelistSyncWebConnectException);
        }
        catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            Log.error("Error opping " + name + " in database!", e);
        }

        return false;
    }

    @Override
    public boolean removeWhitelistPlayer(UUID uuid, String name) {
        long startTime = System.currentTimeMillis();
        try {
            OkHttpClient client = this.apiClientHelper.getClient();
            Request request = new Request.Builder()
                    .url(this.apiClientHelper.getApiHost() + "/api/whitelist/" + uuid.toString())
                    .addHeader("content-type", "application/json")
                    .addHeader("X-API-KEY", this.apiClientHelper.getApiKey())
                    .addHeader("server-uuid", serverUUID.toString())
                    .delete()
                    .build();

            try (Response response = client.newCall(request).execute()) {

                if (response.isSuccessful()) {
                    long timeTaken = System.currentTimeMillis() - startTime;
                    Log.debug("Removed " + name + " from whitelist | Took " + timeTaken + "ms");

                    return true;
                } else {
                    HandleApiNonSuccess(response, "Error removing " + name + " from whitelist database!");
                }
            }

        } catch (ConnectException e) {
            Log.warning(LogMessages.WARN_WhitelistSyncWebConnectException);
        }
        catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            Log.error("Error removing " + name + " from whitelist database!", e);
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
        try {
            OkHttpClient client = this.apiClientHelper.getClient();
            Request request = new Request.Builder()
                    .url(this.apiClientHelper.getApiHost() + "/api/op/" + uuid.toString())
                    .addHeader("content-type", "application/json")
                    .addHeader("X-API-KEY", this.apiClientHelper.getApiKey())
                    .addHeader("server-uuid", serverUUID.toString())
                    .delete()
                    .build();

            try (Response response = client.newCall(request).execute()) {

                if (response.isSuccessful()) {
                    long timeTaken = System.currentTimeMillis() - startTime;
                    Log.debug("Deopped " + name + " | Took " + timeTaken + "ms");

                    return true;
                } else {
                    HandleApiNonSuccess(response, "Error deopping " + name + " in database!");
                }
            }
        } catch (ConnectException e) {
            Log.warning(LogMessages.WARN_WhitelistSyncWebConnectException);
        }
        catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            Log.error("Error opping " + name + " in database!", e);
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
        try {
            // Set body of request
            Gson gson = new Gson();
            JsonObject json = new JsonObject();
            json.addProperty("uuid", uuid.toString());
            json.addProperty("name", name);
            json.addProperty("reason", reason);
            String jsonBody = gson.toJson(json);
            RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json"));

            OkHttpClient client = this.apiClientHelper.getClient();
            Request request = new Request.Builder()
                    .url(this.apiClientHelper.getApiHost() + "/api/bannedplayer")
                    .addHeader("content-type", "application/json")
                    .addHeader("X-API-KEY", this.apiClientHelper.getApiKey())
                    .addHeader("server-uuid", serverUUID.toString())
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {

                if (response.isSuccessful()) {
                    long timeTaken = System.currentTimeMillis() - startTime;
                    Log.debug("Banned " + name + " | Took " + timeTaken + "ms");

                    return true;
                } else {
                    HandleApiNonSuccess(response, "Error banning " + name + " in database!");
                }
            }

        } catch (ConnectException e) {
            Log.warning(LogMessages.WARN_WhitelistSyncWebConnectException);
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            Log.error("Error banning " + name + " in database!", e);
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
        try {
            // Set body of request
            Gson gson = new Gson();
            JsonObject json = new JsonObject();
            json.addProperty("ip", ip);
            json.addProperty("reason", reason);
            String jsonBody = gson.toJson(json);
            RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json"));

            OkHttpClient client = this.apiClientHelper.getClient();
            Request request = new Request.Builder()
                    .url(this.apiClientHelper.getApiHost() + "/api/bannedip")
                    .addHeader("content-type", "application/json")
                    .addHeader("X-API-KEY", this.apiClientHelper.getApiKey())
                    .addHeader("server-uuid", serverUUID.toString())
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {

                if (response.isSuccessful()) {
                    long timeTaken = System.currentTimeMillis() - startTime;
                    Log.debug("Banned " + ip + " | Took " + timeTaken + "ms");

                    return true;
                } else {
                    HandleApiNonSuccess(response, "Error banning " + ip + " in database!");
                }
            }

        } catch (ConnectException e) {
            Log.warning(LogMessages.WARN_WhitelistSyncWebConnectException);
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            Log.error("Error banning " + ip + " in database!", e);
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
        try {
            OkHttpClient client = this.apiClientHelper.getClient();
            Request request = new Request.Builder()
                    .url(this.apiClientHelper.getApiHost() + "/api/bannedplayer/" + uuid.toString())
                    .addHeader("content-type", "application/json")
                    .addHeader("X-API-KEY", this.apiClientHelper.getApiKey())
                    .addHeader("server-uuid", serverUUID.toString())
                    .delete()
                    .build();

            try (Response response = client.newCall(request).execute()) {

                if (response.isSuccessful()) {
                    long timeTaken = System.currentTimeMillis() - startTime;
                    Log.debug("Unbanned " + name + " | Took " + timeTaken + "ms");

                    return true;
                } else {
                    HandleApiNonSuccess(response, "Error unbanning " + name + " in database!");
                }
            }

        } catch (ConnectException e) {
            Log.warning(LogMessages.WARN_WhitelistSyncWebConnectException);
        }
        catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            Log.error("Error unbanning " + name + " in database!", e);
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
        try {
            OkHttpClient client = this.apiClientHelper.getClient();
            Request request = new Request.Builder()
                    .url(this.apiClientHelper.getApiHost() + "/api/bannedip/" + ip)
                    .addHeader("content-type", "application/json")
                    .addHeader("X-API-KEY", this.apiClientHelper.getApiKey())
                    .addHeader("server-uuid", serverUUID.toString())
                    .delete()
                    .build();

            try (Response response = client.newCall(request).execute()) {

                if (response.isSuccessful()) {
                    long timeTaken = System.currentTimeMillis() - startTime;
                    Log.debug("Unbanned " + ip + " | Took " + timeTaken + "ms");

                    return true;
                } else {
                    HandleApiNonSuccess(response, "Error unbanning " + ip + " in database!");
                }
            }

        } catch (ConnectException e) {
            Log.warning(LogMessages.WARN_WhitelistSyncWebConnectException);
        }
        catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            Log.error("Error unbanning " + ip + " in database!", e);
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
