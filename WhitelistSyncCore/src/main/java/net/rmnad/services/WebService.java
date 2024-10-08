package net.rmnad.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.reactivex.rxjava3.annotations.Nullable;
import net.rmnad.Log;
import net.rmnad.callbacks.IServerControl;
import net.rmnad.json.BannedIpsFileReader;
import net.rmnad.json.BannedPlayersFileReader;
import net.rmnad.json.OppedPlayersFileReader;
import net.rmnad.json.WhitelistedPlayersFileReader;
import net.rmnad.logging.LogMessages;
import net.rmnad.models.BannedIp;
import net.rmnad.models.BannedPlayer;
import net.rmnad.models.OppedPlayer;
import net.rmnad.models.WhitelistedPlayer;
import net.rmnad.models.api.BannedIpEntry;
import net.rmnad.models.api.BannedPlayerEntry;
import net.rmnad.models.api.OpEntry;
import net.rmnad.models.api.WhitelistEntry;
import okhttp3.*;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.ConnectException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.UUID;

public class WebService implements BaseService {

    private final String serverFilePath;
    private final String apiHost;
    private final String apiKey;
    public final boolean syncingOpList;
    public final boolean syncingBannedPlayers;
    public final boolean syncingBannedIps;
    public final IServerControl serverControl;
    public final UUID serverUUID = UUID.randomUUID();

    public WebService(
            String serverFilePath,
            String apiHost,
            String apiKey,
            boolean syncingOpList,
            boolean syncingBannedPlayers,
            boolean syncingBannedIps,
            IServerControl serverControl) {

        this.serverFilePath = serverFilePath;
        this.apiHost = apiHost;
        this.apiKey = apiKey;
        this.syncingOpList = syncingOpList;
        this.syncingBannedPlayers = syncingBannedPlayers;
        this.syncingBannedIps = syncingBannedIps;
        this.serverControl = serverControl;

        Log.debug("WebService API host is set to: " + this.apiHost);
    }

    private OkHttpClient getClient() throws NoSuchAlgorithmException, KeyManagementException {

        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        if (this.apiHost.contains("https://localhost")) {
            Log.debug("Creating custom http client (ignoring SSL)");
            X509TrustManager trustManager = new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[] {};
                }

                @Override
                public void checkClientTrusted(X509Certificate[] arg0, String arg1) {
                    // not implemented
                }

                @Override
                public void checkServerTrusted(X509Certificate[] arg0, String arg1) {
                    // not implemented
                }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] { trustManager }, null);


            builder.hostnameVerifier((hostname, session) -> true)
                    .sslSocketFactory(sslContext.getSocketFactory(), trustManager);
        }

        return builder.build();
    }

    public String getApiHost() {
        if (this.apiHost.endsWith("/")) {
            return this.apiHost.substring(0, this.apiHost.length() - 1);
        }

        return this.apiHost;
    }

    public String getApiKey() {
        return apiKey;
    }

    private boolean isAuthenticated() {
        try {
            OkHttpClient client = getClient();

            Request request = new Request.Builder()
                    .url(getApiHost() + "/api/authentication")
                    .addHeader("content-type", "application/json")
                    .addHeader("X-API-KEY", getApiKey())
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
            OkHttpClient client = getClient();
            Request request = new Request.Builder()
                    .url(getApiHost() + "/api/whitelist")
                    .addHeader("content-type", "application/json")
                    .addHeader("X-API-KEY", getApiKey())
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
            OkHttpClient client = getClient();
            Request request = new Request.Builder()
                    .url(getApiHost() + "/api/op")
                    .addHeader("content-type", "application/json")
                    .addHeader("X-API-KEY", getApiKey())
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
            OkHttpClient client = getClient();
            Request request = new Request.Builder()
                    .url(getApiHost() + "/api/bannedplayer")
                    .addHeader("content-type", "application/json")
                    .addHeader("X-API-KEY", getApiKey())
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
            OkHttpClient client = getClient();
            Request request = new Request.Builder()
                    .url(getApiHost() + "/api/bannedip")
                    .addHeader("content-type", "application/json")
                    .addHeader("X-API-KEY", getApiKey())
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
        if (getApiHost().isEmpty()) {
            Log.error("API Host is not set. Please set the API Host in the configuration file.");
            return false;
        }

        if (getApiKey().isEmpty()) {
            Log.error("API Key is not set. Please set the API Key in the configuration file.");
            return false;
        }

        if (isAuthenticated()) {
            Log.info("Connected to Web API successfully!");
            return true;
        } else {
            Log.error("Failed to authenticate with Web API. If you have not setup an API Key, you can create one on the website at "
                    + getApiHost() + ". Don't forget to set the API Key in the configuration file.");
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

        if (!this.syncingOpList) {
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
                = WhitelistedPlayersFileReader.getWhitelistedPlayers(this.serverFilePath);

        try {
            OkHttpClient client = getClient();

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
                    .url(getApiHost() + "/api/whitelist/push")
                    .addHeader("content-type", "application/json")
                    .addHeader("X-API-KEY", getApiKey())
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
                    Log.error("Failed to update database with local records. Response Code: " + response.code());
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
        if (!this.syncingOpList) {
            Log.error(LogMessages.ALERT_OP_SYNC_DISABLED);
            return false;
        }

        // TODO: Start job on thread to avoid lag?
        int records = 0;
        long startTime = System.currentTimeMillis();

        ArrayList<OppedPlayer> oppedPlayers
                = OppedPlayersFileReader.getOppedPlayers(this.serverFilePath);

        try {
            OkHttpClient client = getClient();

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
                    .url(getApiHost() + "/api/op/push")
                    .addHeader("content-type", "application/json")
                    .addHeader("X-API-KEY", getApiKey())
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
                    Log.error("Failed to update database with local records. Response Code: " + response.code());
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
        if (!this.syncingBannedPlayers) {
            Log.error(LogMessages.ALERT_BANNED_PLAYERS_SYNC_DISABLED);
            return false;
        }

        int records = 0;
        long startTime = System.currentTimeMillis();

        ArrayList<BannedPlayer> bannedPlayers
                = BannedPlayersFileReader.getBannedPlayers(this.serverFilePath);

        try {
            OkHttpClient client = getClient();

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
                    .url(getApiHost() + "/api/bannedplayer/push")
                    .addHeader("content-type", "application/json")
                    .addHeader("X-API-KEY", getApiKey())
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
                    Log.error("Failed to update database with local records. Response Code: " + response.code());
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
        if (!this.syncingBannedIps) {
            Log.error(LogMessages.ALERT_BANNED_IPS_SYNC_DISABLED);
            return false;
        }

        int records = 0;
        long startTime = System.currentTimeMillis();

        ArrayList<BannedIp> bannedIps
                = BannedIpsFileReader.getBannedIps(this.serverFilePath);

        try {
            OkHttpClient client = getClient();

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
                    .url(getApiHost() + "/api/bannedip/push")
                    .addHeader("content-type", "application/json")
                    .addHeader("X-API-KEY", getApiKey())
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
                    Log.error("Failed to update database with local records. Response Code: " + response.code());
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
                = WhitelistedPlayersFileReader.getWhitelistedPlayers(this.serverFilePath);

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
        if (!this.syncingOpList) {
            Log.error(LogMessages.ALERT_OP_SYNC_DISABLED);
            return false;
        }

        int records = 0;
        long startTime = System.currentTimeMillis();

        ArrayList<OppedPlayer> localOppedPlayers
                = OppedPlayersFileReader.getOppedPlayers(this.serverFilePath);

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
        if (!this.syncingBannedPlayers) {
            Log.error(LogMessages.ALERT_BANNED_PLAYERS_SYNC_DISABLED);
            return false;
        }

        int records = 0;
        long startTime = System.currentTimeMillis();

        ArrayList<BannedPlayer> localBannedPlayers
                = BannedPlayersFileReader.getBannedPlayers(this.serverFilePath);

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
        if (!this.syncingBannedIps) {
            Log.error(LogMessages.ALERT_BANNED_IPS_SYNC_DISABLED);
            return false;
        }

        int records = 0;
        long startTime = System.currentTimeMillis();

        ArrayList<BannedIp> localBannedIps
                = BannedIpsFileReader.getBannedIps(this.serverFilePath);

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

            OkHttpClient client = getClient();
            Request request = new Request.Builder()
                    .url(getApiHost() + "/api/whitelist")
                    .addHeader("content-type", "application/json")
                    .addHeader("X-API-KEY", getApiKey())
                    .addHeader("server-uuid", serverUUID.toString())
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {

                if (response.isSuccessful()) {
                    long timeTaken = System.currentTimeMillis() - startTime;
                    Log.debug("Added " + name + " to whitelist | Took " + timeTaken + "ms");

                    return true;
                } else {
                    Log.error("Error adding " + name + " to whitelist database! Response Code: " + response.code());
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
        if (!syncingOpList) {
            Log.error(LogMessages.ALERT_OP_SYNC_DISABLED);
            return false;
        }

        long startTime = System.currentTimeMillis();
        try {
            OkHttpClient client = getClient();
            Gson gson = new Gson();
            JsonObject json = new JsonObject();
            json.addProperty("uuid", uuid.toString());
            json.addProperty("name", name);
            String jsonBody = gson.toJson(json);
            RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json"));

            Request request = new Request.Builder()
                    .url(getApiHost() + "/api/op")
                    .addHeader("content-type", "application/json")
                    .addHeader("X-API-KEY", getApiKey())
                    .addHeader("server-uuid", serverUUID.toString())
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {

                if (response.isSuccessful()) {
                    long timeTaken = System.currentTimeMillis() - startTime;
                    Log.debug("Opped " + name + " | Took " + timeTaken + "ms");

                    return true;
                } else {
                    Log.error("Error opping " + name + " in database! Response Code: " + response.code());
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
            OkHttpClient client = getClient();
            Request request = new Request.Builder()
                    .url(getApiHost() + "/api/whitelist/" + uuid.toString())
                    .addHeader("content-type", "application/json")
                    .addHeader("X-API-KEY", getApiKey())
                    .addHeader("server-uuid", serverUUID.toString())
                    .delete()
                    .build();

            try (Response response = client.newCall(request).execute()) {

                if (response.isSuccessful()) {
                    long timeTaken = System.currentTimeMillis() - startTime;
                    Log.debug("Removed " + name + " from whitelist | Took " + timeTaken + "ms");

                    return true;
                } else {
                    Log.error("Error removing " + name + " from whitelist database! Response Code: " + response.code());
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
        if (!syncingOpList) {
            Log.error(LogMessages.ALERT_OP_SYNC_DISABLED);
            return false;
        }

        long startTime = System.currentTimeMillis();
        try {
            OkHttpClient client = getClient();
            Request request = new Request.Builder()
                    .url(getApiHost() + "/api/op/" + uuid.toString())
                    .addHeader("content-type", "application/json")
                    .addHeader("X-API-KEY", getApiKey())
                    .addHeader("server-uuid", serverUUID.toString())
                    .delete()
                    .build();

            try (Response response = client.newCall(request).execute()) {

                if (response.isSuccessful()) {
                    long timeTaken = System.currentTimeMillis() - startTime;
                    Log.debug("Deopped " + name + " | Took " + timeTaken + "ms");

                    return true;
                } else {
                    Log.error("Error opping " + name + " in database! Response Code: " + response.code());
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
        if (!syncingBannedPlayers) {
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

            OkHttpClient client = getClient();
            Request request = new Request.Builder()
                    .url(getApiHost() + "/api/bannedplayer")
                    .addHeader("content-type", "application/json")
                    .addHeader("X-API-KEY", getApiKey())
                    .addHeader("server-uuid", serverUUID.toString())
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {

                if (response.isSuccessful()) {
                    long timeTaken = System.currentTimeMillis() - startTime;
                    Log.debug("Banned " + name + " | Took " + timeTaken + "ms");

                    return true;
                } else {
                    Log.error("Error banning " + name + " in database! Response Code: " + response.code());
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
        if (!syncingBannedIps) {
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

            OkHttpClient client = getClient();
            Request request = new Request.Builder()
                    .url(getApiHost() + "/api/bannedip")
                    .addHeader("content-type", "application/json")
                    .addHeader("X-API-KEY", getApiKey())
                    .addHeader("server-uuid", serverUUID.toString())
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {

                if (response.isSuccessful()) {
                    long timeTaken = System.currentTimeMillis() - startTime;
                    Log.debug("Banned " + ip + " | Took " + timeTaken + "ms");

                    return true;
                } else {
                    Log.error("Error banning " + ip + " in database! Response Code: " + response.code());
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
        if (!syncingBannedPlayers) {
            Log.error(LogMessages.ALERT_BANNED_PLAYERS_SYNC_DISABLED);
            return false;
        }

        long startTime = System.currentTimeMillis();
        try {
            OkHttpClient client = getClient();
            Request request = new Request.Builder()
                    .url(getApiHost() + "/api/bannedplayer/" + uuid.toString())
                    .addHeader("content-type", "application/json")
                    .addHeader("X-API-KEY", getApiKey())
                    .addHeader("server-uuid", serverUUID.toString())
                    .delete()
                    .build();

            try (Response response = client.newCall(request).execute()) {

                if (response.isSuccessful()) {
                    long timeTaken = System.currentTimeMillis() - startTime;
                    Log.debug("Unbanned " + name + " | Took " + timeTaken + "ms");

                    return true;
                } else {
                    Log.error("Error unbanning " + name + " in database! Response Code: " + response.code());
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
        if (!syncingBannedIps) {
            Log.error(LogMessages.ALERT_BANNED_IPS_SYNC_DISABLED);
            return false;
        }

        long startTime = System.currentTimeMillis();
        try {
            OkHttpClient client = getClient();
            Request request = new Request.Builder()
                    .url(getApiHost() + "/api/bannedip/" + ip)
                    .addHeader("content-type", "application/json")
                    .addHeader("X-API-KEY", getApiKey())
                    .addHeader("server-uuid", serverUUID.toString())
                    .delete()
                    .build();

            try (Response response = client.newCall(request).execute()) {

                if (response.isSuccessful()) {
                    long timeTaken = System.currentTimeMillis() - startTime;
                    Log.debug("Unbanned " + ip + " | Took " + timeTaken + "ms");

                    return true;
                } else {
                    Log.error("Error unbanning " + ip + " in database! Response Code: " + response.code());
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
}
