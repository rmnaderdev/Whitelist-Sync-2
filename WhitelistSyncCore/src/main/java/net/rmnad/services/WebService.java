package net.rmnad.services;

import com.google.gson.Gson;
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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class WebService implements BaseService {

    private final String apiKey;

    public WebService(String apiKey) {
        this.apiKey = apiKey;
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
        } catch (IOException e) {
            Log.error(e);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
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
        } catch (IOException e) {
            Log.error(e);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
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
                return gson.fromJson(response.getEntity().getContent().toString(), OpEntry[].class);
            } else {
                Log.error("Failed to get op entries from API. Response Code: " + response.getStatusLine().getStatusCode());
            }
        } catch (IOException e) {
            Log.error(e);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
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
    public boolean requiresSyncing() {
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

            WhitelistedPlayer player = new WhitelistedPlayer();
            player.setUuid(entry.getUuid());
            player.setName(entry.getName());
            player.setWhitelisted(entry.getWhitelisted());
            whitelistedPlayers.add(player);
        }

        return whitelistedPlayers;
    }

    @Override
    public ArrayList<OppedPlayer> getOppedPlayersFromDatabase() {
        return null;
    }

    @Override
    public boolean pushLocalWhitelistToDatabase(ArrayList<WhitelistedPlayer> whitelistedPlayers) {
        return false;
    }

    @Override
    public boolean pushLocalOpsToDatabase(ArrayList<OppedPlayer> oppedPlayers) {
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
        return false;
    }

    @Override
    public boolean addWhitelistPlayer(UUID uuid, String name) {
        return false;
    }

    @Override
    public boolean addOppedPlayer(UUID uuid, String name) {
        return false;
    }

    @Override
    public boolean removeWhitelistPlayer(UUID uuid, String name) {
        return false;
    }

    @Override
    public boolean removeOppedPlayer(UUID uuid, String name) {
        return false;
    }
}
