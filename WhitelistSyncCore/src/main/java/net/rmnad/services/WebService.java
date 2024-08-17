package net.rmnad.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.rmnad.Log;
import net.rmnad.callbacks.IOnUserOpAdd;
import net.rmnad.callbacks.IOnUserOpRemove;
import net.rmnad.callbacks.IOnUserWhitelistAdd;
import net.rmnad.callbacks.IOnUserWhitelistRemove;
import net.rmnad.models.OppedPlayer;
import net.rmnad.models.WhitelistedPlayer;
import net.rmnad.models.api.OpEntry;
import net.rmnad.models.api.WhitelistEntry;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
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

    private final String apiKey;
    private final boolean syncingOpList;

    public WebService(String apiKey, boolean syncingOpList) {
        this.apiKey = apiKey;
        this.syncingOpList = syncingOpList;
    }

    private CloseableHttpClient getClient() throws NoSuchAlgorithmException, KeyManagementException {
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

        return HttpClients.custom()
                .setSslcontext(sslContext)
                .setHostnameVerifier(new AllowAllHostnameVerifier())
                .build();
    }

    private String getApiHost() {
        return "https://localhost:3000";
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

            return response.getStatusLine().getStatusCode() == 200;
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

            if (response.getStatusLine().getStatusCode() == 200) {
                Gson gson = new Gson();
                return gson.fromJson(EntityUtils.toString(response.getEntity(), "UTF-8"), WhitelistEntry[].class);
            } else {
                Log.error("Failed to get whitelist entries from API. Response Code: " + response.getStatusLine().getStatusCode());
            }
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
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

            if (response.getStatusLine().getStatusCode() == 200) {
                Gson gson = new Gson();
                return gson.fromJson(EntityUtils.toString(response.getEntity(), "UTF-8"), OpEntry[].class);
            } else {
                Log.error("Failed to get op entries from API. Response Code: " + response.getStatusLine().getStatusCode());
            }
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            Log.error("Error getting opped players from Web API.", e);
        }

        return new OpEntry[0];
    }

    @Override
    public boolean initializeDatabase() {
        if (isAuthenticated()) {
            Log.info("Database Initialized");
            return true;
        }

        return false;
    }

    @Override
    public ArrayList<WhitelistedPlayer> getWhitelistedPlayersFromDatabase() {
        ArrayList<WhitelistedPlayer> whitelistedPlayers = new ArrayList<>();
        WhitelistEntry[] entries = getWhitelistEntries();

        for (WhitelistEntry entry : entries) {
            // TODO: Move to parameter of the api call
            if (!entry.getWhitelisted())
                continue;

            whitelistedPlayers.add(entry.toWhitelistedPlayer());
        }

        return whitelistedPlayers;
    }

    @Override
    public ArrayList<OppedPlayer> getOppedPlayersFromDatabase() {
        ArrayList<OppedPlayer> oppedPlayers = new ArrayList<>();
        OpEntry[] entries = getOpEntries();

        for (OpEntry entry : entries) {
            // TODO: Move to parameter of the api call
            if (!entry.getOpped())
                continue;

            oppedPlayers.add(entry.toOppedPlayer());
        }

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

            if (response.getStatusLine().getStatusCode() == 200) {
                // Record time taken.
                long timeTaken = System.currentTimeMillis() - startTime;
                Log.debug("Whitelist table updated | Took " + timeTaken + "ms | Wrote " + records + " records.");
                return true;
            } else {
                Log.error("Failed to update database with local records. Response Code: " + response.getStatusLine().getStatusCode());
            }
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            Log.error("Failed to update database with local records.", e);
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

            if (response.getStatusLine().getStatusCode() == 200) {
                // Record time taken.
                long timeTaken = System.currentTimeMillis() - startTime;
                Log.debug("Op table updated | Took " + timeTaken + "ms | Wrote " + records + " records.");
                return true;
            } else {
                Log.error("Failed to update database with local records. Response Code: " + response.getStatusLine().getStatusCode());
            }
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            Log.error("Failed to update database with local records.", e);
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
                        Log.debug("Added " + name + " to whitelist.");
                        records++;
                    } catch (NullPointerException e) {
                        Log.error("Player is null?");
                        Log.error(e.getMessage(), e);
                    }
                }
            } else {
                if (localWhitelistedPlayers.stream().anyMatch(o -> o.getUuid().equals(uuid.toString()))) {
                    onUserRemove.call(uuid, name);
                    Log.debug("Removed " + name + " from whitelist.");
                    records++;
                }
            }
        }

        long timeTaken = System.currentTimeMillis() - startTime;
        Log.debug("Copied whitelist database to local | Took " + timeTaken + "ms | Wrote " + records + " records.");

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
                        Log.debug("Opped " + name + ".");
                        records++;
                    } catch (NullPointerException e) {
                        Log.error("Player is null?");
                        Log.error(e.getMessage(), e);
                    }
                }
            } else {
                if (localOppedPlayers.stream().anyMatch(o -> o.getUuid().equals(uuid.toString()))) {
                    onUserRemove.call(uuid, name);
                    Log.debug("Deopped " + name + ".");
                    records++;
                }
            }
        }

        long timeTaken = System.currentTimeMillis() - startTime;
        Log.debug("Copied op database to local | Took " + timeTaken + "ms | Wrote " + records + " records.");

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

            if (response.getStatusLine().getStatusCode() == 200) {
                long timeTaken = System.currentTimeMillis() - startTime;
                Log.debug("Added " + name + " to whitelist | Took " + timeTaken + "ms");

                return true;
            } else {
                Log.error("Error adding " + name + " to whitelist database! Response Code: " + response.getStatusLine().getStatusCode());
            }

        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            Log.error("Error adding " + name + " to whitelist database!", e);
        }

        return false;
    }

    @Override
    public boolean addOppedPlayer(UUID uuid, String name) {
        if (!syncingOpList) {
            Log.error("Op list syncing is currently disabled in your config. "
                    + "Please enable it and restart the server to use this feature.");
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

            if (response.getStatusLine().getStatusCode() == 200) {
                long timeTaken = System.currentTimeMillis() - startTime;
                Log.debug("Opped " + name + " | Took " + timeTaken + "ms");

                return true;
            } else {
                Log.error("Error opping " + name + " in database! Response Code: " + response.getStatusLine().getStatusCode());
            }

        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
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

            if (response.getStatusLine().getStatusCode() == 200) {
                long timeTaken = System.currentTimeMillis() - startTime;
                Log.debug("Removed " + name + " from whitelist | Took " + timeTaken + "ms");

                return true;
            } else {
                Log.error("Error removing " + name + " from whitelist database! Response Code: " + response.getStatusLine().getStatusCode());
            }

        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            Log.error("Error removing " + name + " from whitelist database!", e);
        }

        return false;
    }

    @Override
    public boolean removeOppedPlayer(UUID uuid, String name) {
        if (!syncingOpList) {
            Log.error("Op list syncing is currently disabled in your config. "
                    + "Please enable it and restart the server to use this feature.");
            return false;
        }

        long startTime = System.currentTimeMillis();
        try {
            CloseableHttpClient client = getClient();
            HttpDelete request = new HttpDelete(getApiHost() + "/api/v1/op/" + uuid.toString());
            request.addHeader("content-type", "application/json");
            request.addHeader("Authorization", getApiKey());

            HttpResponse response = client.execute(request);

            if (response.getStatusLine().getStatusCode() == 200) {
                long timeTaken = System.currentTimeMillis() - startTime;
                Log.debug("Deopped " + name + " | Took " + timeTaken + "ms");

                return true;
            } else {
                Log.error("Error opping " + name + " in database! Response Code: " + response.getStatusLine().getStatusCode());
            }

        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            Log.error("Error opping " + name + " in database!", e);
        }

        return false;
    }

    private void log
}
