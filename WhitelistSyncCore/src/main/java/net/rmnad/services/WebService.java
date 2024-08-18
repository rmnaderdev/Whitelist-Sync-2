package net.rmnad.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.rmnad.Log;
import net.rmnad.callbacks.IOnUserOpAdd;
import net.rmnad.callbacks.IOnUserOpRemove;
import net.rmnad.callbacks.IOnUserWhitelistAdd;
import net.rmnad.callbacks.IOnUserWhitelistRemove;
import net.rmnad.logging.LogMessages;
import net.rmnad.models.OppedPlayer;
import net.rmnad.models.WhitelistedPlayer;
import net.rmnad.models.api.OpEntry;
import net.rmnad.models.api.WhitelistEntry;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.UUID;

public class WebService implements BaseService {

    private final String apiHost;
    private final String apiKey;
    private final boolean syncingOpList;

    public WebService(String apiHost, String apiKey, boolean syncingOpList) {
        this.apiHost = apiHost;
        this.apiKey = apiKey;
        this.syncingOpList = syncingOpList;

        Log.debug("WebService API host is set to: " + this.apiHost);
    }

    private CloseableHttpClient getClient() throws NoSuchAlgorithmException, KeyManagementException {

        if (this.apiHost.contains("https://localhost")) {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                        public void checkClientTrusted(
                                java.security.cert.X509Certificate[] certs, String authType) {
                        }
                        public void checkServerTrusted(
                                java.security.cert.X509Certificate[] certs, String authType) {
                        }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            Log.debug("Creating custom http client (ignoring SSL)");

            return HttpClients.custom()
                    .setSslcontext(sslContext)
                    .setHostnameVerifier(new AllowAllHostnameVerifier())
                    .build();
        }

        return HttpClients.createDefault();
    }

    private String getApiHost() {
        if (this.apiHost.endsWith("/")) {
            return this.apiHost.substring(0, this.apiHost.length() - 1);
        }

        return this.apiHost;
    }

    private String getApiKey() {
        return apiKey;
    }

    private boolean isAuthenticated() {
        try {
            CloseableHttpClient client = getClient();
            HttpGet request = new HttpGet(getApiHost() + "/api/v1/authenticate");
            request.addHeader("content-type", "application/json");
            request.addHeader("Authorization", getApiKey());

            HttpResponse response = client.execute(request);
            Log.debug("isAuthenticated Response Code : " + response.getStatusLine().getStatusCode());

            return responseIsSuccess(response);
        }
        catch (HttpHostConnectException e) {
            Log.warning("Failed to connect to Whitelist Sync Web API.");
            // Return true to try again later.
            return true;
        }
        catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            Log.error("Error authenticating with Web API.", e);
        }

        return false;
    }

    private WhitelistEntry[] getWhitelistEntries() {
        try {
            CloseableHttpClient client = getClient();
            HttpGet request = new HttpGet(getApiHost() + "/api/v1/whitelist");
            request.addHeader("content-type", "application/json");
            request.addHeader("Authorization", getApiKey());

            HttpResponse response = client.execute(request);
            Log.debug("getWhitelistEntries Response Code : " + response.getStatusLine().getStatusCode());

            if (responseIsSuccess(response)) {
                Gson gson = new Gson();
                return gson.fromJson(EntityUtils.toString(response.getEntity(), "UTF-8"), WhitelistEntry[].class);
            } else {
                Log.error("Failed to get whitelist entries from API. Response Code: " + response.getStatusLine().getStatusCode());
            }

        }
        catch (HttpHostConnectException e) {
            Log.warning("Failed to connect to Whitelist Sync Web API.");
        }
        catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            Log.error("Error getting whitelisted players from Web API.", e);
        }

        return new WhitelistEntry[0];
    }

    private OpEntry[] getOpEntries() {
        try {
            CloseableHttpClient client = getClient();
            HttpGet request = new HttpGet(getApiHost() + "/api/v1/op");
            request.addHeader("content-type", "application/json");
            request.addHeader("Authorization", getApiKey());

            HttpResponse response = client.execute(request);
            Log.debug("getOpEntries Response Code : " + response.getStatusLine().getStatusCode());

            if (responseIsSuccess(response)) {
                Gson gson = new Gson();
                return gson.fromJson(EntityUtils.toString(response.getEntity(), "UTF-8"), OpEntry[].class);
            } else {
                Log.error("Failed to get op entries from API. Response Code: " + response.getStatusLine().getStatusCode());
            }
        }
        catch (HttpHostConnectException e) {
            Log.warning("Failed to connect to Whitelist Sync Web API.");
        }
        catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            Log.error("Error getting opped players from Web API.", e);
        }

        return new OpEntry[0];
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
            // TODO: Move to parameter of the api call
            if (!entry.getWhitelisted())
                continue;

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
            // TODO: Move to parameter of the api call
            if (!entry.getOpped())
                continue;

            oppedPlayers.add(entry.toOppedPlayer());
        }

        long timeTaken = System.currentTimeMillis() - startTime;
        Log.debug(LogMessages.SuccessGetOppedPlayersFromDatabase(timeTaken, entries.length));

        return oppedPlayers;
    }

    @Override
    public boolean pushLocalWhitelistToDatabase(ArrayList<WhitelistedPlayer> whitelistedPlayers) {
        // TODO: Start job on thread to avoid lag?
        int records = 0;
        long startTime = System.currentTimeMillis();

        try {
            CloseableHttpClient client = getClient();
            HttpPost request = new HttpPost(getApiHost() + "/api/v1/whitelist/push");
            request.addHeader("content-type", "application/json");
            request.addHeader("Authorization", getApiKey());

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
            request.setEntity(new StringEntity(jsonBody));

            HttpResponse response = client.execute(request);

            if (responseIsSuccess(response)) {
                // Record time taken.
                long timeTaken = System.currentTimeMillis() - startTime;
                Log.debug(LogMessages.SuccessPushLocalWhitelistToDatabase(timeTaken, records));
                return true;
            } else {
                Log.error("Failed to update database with local records. Response Code: " + response.getStatusLine().getStatusCode());
            }
        }
        catch (HttpHostConnectException e) {
            Log.warning("Failed to connect to Whitelist Sync Web API.");
        }
        catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            Log.error(LogMessages.ERROR_PushLocalWhitelistToDatabase, e);
        }

        return false;
    }

    @Override
    public boolean pushLocalOpsToDatabase(ArrayList<OppedPlayer> oppedPlayers) {
        // TODO: Start job on thread to avoid lag?
        int records = 0;
        long startTime = System.currentTimeMillis();

        try {
            CloseableHttpClient client = getClient();
            HttpPost request = new HttpPost(getApiHost() + "/api/v1/op/push");
            request.addHeader("content-type", "application/json");
            request.addHeader("Authorization", getApiKey());

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
            request.setEntity(new StringEntity(jsonBody));

            HttpResponse response = client.execute(request);

            if (responseIsSuccess(response)) {
                // Record time taken.
                long timeTaken = System.currentTimeMillis() - startTime;
                Log.debug(LogMessages.SuccessPushLocalOpsToDatabase(timeTaken, records));
                return true;
            } else {
                Log.error("Failed to update database with local records. Response Code: " + response.getStatusLine().getStatusCode());
            }
        }
        catch (HttpHostConnectException e) {
            Log.warning("Failed to connect to Whitelist Sync Web API.");
        }
        catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            Log.error(LogMessages.ERROR_PushLocalOpsToDatabase, e);
        }

        return false;
    }

    @Override
    public boolean pullDatabaseWhitelistToLocal(ArrayList<WhitelistedPlayer> localWhitelistedPlayers, IOnUserWhitelistAdd onUserAdd, IOnUserWhitelistRemove onUserRemove) {
        int records = 0;

        long startTime = System.currentTimeMillis();

        WhitelistEntry[] entries = getWhitelistEntries();

        for (WhitelistEntry player : entries) {
            UUID uuid = UUID.fromString(player.getUuid());
            String name = player.getName();
            boolean whitelisted = player.getWhitelisted();

            if (whitelisted) {
                if (localWhitelistedPlayers.stream().noneMatch(o -> o.getUuid().equals(uuid.toString()))) {
                    try {
                        onUserAdd.call(uuid, name);
                        Log.debug(LogMessages.AddedUserToWhitelist(name));
                        records++;
                    } catch (NullPointerException e) {
                        Log.error(e.getMessage(), e);
                    }
                }
            } else {
                if (localWhitelistedPlayers.stream().anyMatch(o -> o.getUuid().equals(uuid.toString()))) {
                    onUserRemove.call(uuid, name);
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
    public boolean pullDatabaseOpsToLocal(ArrayList<OppedPlayer> localOppedPlayers, IOnUserOpAdd onUserAdd, IOnUserOpRemove onUserRemove) {
        int records = 0;

        long startTime = System.currentTimeMillis();

        OpEntry[] entries = getOpEntries();

        for (OpEntry player : entries) {
            UUID uuid = UUID.fromString(player.getUuid());
            String name = player.getName();
            boolean opped = player.getOpped();

            if (opped) {
                if (localOppedPlayers.stream().noneMatch(o -> o.getUuid().equals(uuid.toString()))) {
                    try {
                        onUserAdd.call(uuid, name);
                        Log.debug(LogMessages.OppedUser(name));
                        records++;
                    } catch (NullPointerException e) {
                        Log.error(e.getMessage(), e);
                    }
                }
            } else {
                if (localOppedPlayers.stream().anyMatch(o -> o.getUuid().equals(uuid.toString()))) {
                    onUserRemove.call(uuid, name);
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
    public boolean addWhitelistPlayer(UUID uuid, String name) {
        long startTime = System.currentTimeMillis();
        try {
            CloseableHttpClient client = getClient();
            HttpPost request = new HttpPost(getApiHost() + "/api/v1/whitelist");
            request.addHeader("content-type", "application/json");
            request.addHeader("Authorization", getApiKey());

            // Set body of request
            Gson gson = new Gson();
            JsonObject json = new JsonObject();
            json.addProperty("uuid", uuid.toString());
            json.addProperty("name", name);
            String jsonBody = gson.toJson(json);
            request.setEntity(new StringEntity(jsonBody));

            HttpResponse response = client.execute(request);

            if (responseIsSuccess(response)) {
                long timeTaken = System.currentTimeMillis() - startTime;
                Log.debug("Added " + name + " to whitelist | Took " + timeTaken + "ms");

                return true;
            } else {
                Log.error("Error adding " + name + " to whitelist database! Response Code: " + response.getStatusLine().getStatusCode());
            }

        }
        catch (HttpHostConnectException e) {
            Log.warning("Failed to connect to Whitelist Sync Web API.");
        }
        catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
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
            CloseableHttpClient client = getClient();
            HttpPost request = new HttpPost(getApiHost() + "/api/v1/op");
            request.addHeader("content-type", "application/json");
            request.addHeader("Authorization", getApiKey());

            // Set body of request
            Gson gson = new Gson();
            JsonObject json = new JsonObject();
            json.addProperty("uuid", uuid.toString());
            json.addProperty("name", name);
            String jsonBody = gson.toJson(json);
            request.setEntity(new StringEntity(jsonBody));

            HttpResponse response = client.execute(request);

            if (responseIsSuccess(response)) {
                long timeTaken = System.currentTimeMillis() - startTime;
                Log.debug("Opped " + name + " | Took " + timeTaken + "ms");

                return true;
            } else {
                Log.error("Error opping " + name + " in database! Response Code: " + response.getStatusLine().getStatusCode());
            }

        }
        catch (HttpHostConnectException e) {
            Log.warning("Failed to connect to Whitelist Sync Web API.");
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
            CloseableHttpClient client = getClient();
            HttpDelete request = new HttpDelete(getApiHost() + "/api/v1/whitelist/" + uuid.toString());
            request.addHeader("content-type", "application/json");
            request.addHeader("Authorization", getApiKey());

            HttpResponse response = client.execute(request);

            if (responseIsSuccess(response)) {
                long timeTaken = System.currentTimeMillis() - startTime;
                Log.debug("Removed " + name + " from whitelist | Took " + timeTaken + "ms");

                return true;
            } else {
                Log.error("Error removing " + name + " from whitelist database! Response Code: " + response.getStatusLine().getStatusCode());
            }

        }
        catch (HttpHostConnectException e) {
            Log.warning("Failed to connect to Whitelist Sync Web API.");
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
            CloseableHttpClient client = getClient();
            HttpDelete request = new HttpDelete(getApiHost() + "/api/v1/op/" + uuid.toString());
            request.addHeader("content-type", "application/json");
            request.addHeader("Authorization", getApiKey());

            HttpResponse response = client.execute(request);

            if (responseIsSuccess(response)) {
                long timeTaken = System.currentTimeMillis() - startTime;
                Log.debug("Deopped " + name + " | Took " + timeTaken + "ms");

                return true;
            } else {
                Log.error("Error opping " + name + " in database! Response Code: " + response.getStatusLine().getStatusCode());
            }
        }
        catch (HttpHostConnectException e) {
            Log.warning("Failed to connect to Whitelist Sync Web API.");
        }
        catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            Log.error("Error opping " + name + " in database!", e);
        }

        return false;
    }

    private boolean responseIsSuccess(HttpResponse response) {
        return response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() < 300;
    }
}
