package net.rmnad.services;

import net.rmnad.Log;
import net.rmnad.callbacks.IOnUserOpAdd;
import net.rmnad.callbacks.IOnUserOpRemove;
import net.rmnad.callbacks.IOnUserWhitelistAdd;
import net.rmnad.callbacks.IOnUserWhitelistRemove;
import net.rmnad.models.OppedPlayer;
import net.rmnad.models.WhitelistedPlayer;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

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

    private boolean isAuthenticated() {
        try {

            CloseableHttpClient client = getClient();
            HttpGet request = new HttpGet("https://localhost:3000/api/v1/authenticate");
            request.addHeader("content-type", "application/json");
            request.addHeader("Authorization", apiKey);

            // Ignore SSL Certificates


            HttpResponse response = client.execute(request);
            Log.info("isAuthenticated Response Code : " + response.getStatusLine().getStatusCode());

            return response.getStatusLine().getStatusCode() == 200;
        } catch (IOException e) {
            Log.error(e);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }

        return false;
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
        return null;
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
        return false;
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
