package net.rmnad.services;

import com.google.gson.internal.LinkedTreeMap;
import com.microsoft.signalr.HttpHubConnectionBuilder;
import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import com.microsoft.signalr.HubConnectionState;
import io.reactivex.rxjava3.core.Single;
import net.rmnad.Log;
import net.rmnad.callbacks.*;
import net.rmnad.logging.LogMessages;
import net.rmnad.models.OppedPlayer;
import net.rmnad.models.WhitelistedPlayer;
import net.rmnad.models.api.OpEntry;
import net.rmnad.models.api.WhitelistEntry;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;

import javax.net.ssl.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class WhitelistSocketThread extends Thread {

    private final WebService service;
    private final boolean errorOnSetup;

    private final IServerControl serverControl;

    private HubConnection hubConnection;
    private final CountDownLatch latch = new CountDownLatch(1);

    public WhitelistSocketThread(
            WebService service,
            boolean errorOnSetup,
            IServerControl serverControl) {

        this.setName("WhitelistSocketThread");
        this.setDaemon(true);
        this.service = service;
        this.errorOnSetup = errorOnSetup;

        this.serverControl = serverControl;
    }

    @Override
    public void run() {
        try {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.debug("WhitelistSocketThread interrupted. Exiting.");
                return;
            }

            Log.info(LogMessages.SOCKET_THREAD_STARTING);

            if(this.errorOnSetup) {
                Log.error(LogMessages.ERROR_INITIALIZING_WHITELIST_SYNC_WEB_API);
                return;
            }

            HttpHubConnectionBuilder hubConnectionBuilder = HubConnectionBuilder
                    .create(this.service.getApiHost() + "/hubs/whitelistsyncmodhub")
                    .withHeader("X-Api-Key", this.service.getApiKey())
                    .withHeader("server-uuid", this.service.serverUUID.toString());

            if (this.service.getApiHost().startsWith("https://localhost")) {
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

                hubConnectionBuilder = hubConnectionBuilder.setHttpClientBuilderCallback(httpClientBuilder -> httpClientBuilder
                        .retryOnConnectionFailure(true)
                        .sslSocketFactory(sslContext.getSocketFactory(), trustManager)
                        .hostnameVerifier((hostname, session) -> true));
            } else {
                hubConnectionBuilder = hubConnectionBuilder.setHttpClientBuilderCallback(httpClientBuilder -> httpClientBuilder
                        .retryOnConnectionFailure(true));
            }

            this.hubConnection = hubConnectionBuilder.build();

            this.hubConnection.on("WhitelistUpdated", (data, serverUuidStr) -> {

                UUID serverUUID = serverUuidStr != null ? UUID.fromString(serverUuidStr) : null;

                // Ignore if the server UUID is the same as this server
                if (serverUUID != null && serverUUID.equals(this.service.serverUUID)) {
                    return;
                }

                boolean isWhitelisted = data.getWhitelisted();
                String name = data.getName();
                UUID uuid = UUID.fromString(data.getUuid());

                if (isWhitelisted) {
                    Log.info("Web-Socket: Player whitelisted: " + name + " (" + uuid + ")");
                    this.serverControl.addWhitelistPlayer(uuid, name);
                } else {
                    Log.info("Web-Socket: Player removed from whitelist: " + name + " (" + uuid + ")");
                    this.serverControl.removeWhitelistPlayer(uuid, name);
                }
            }, WhitelistEntry.class, String.class);

            if (this.service.syncingOpList) {
                this.hubConnection.on("OpUpdated", (data, serverUuidStr) -> {

                    UUID serverUUID = serverUuidStr != null ? UUID.fromString(serverUuidStr) : null;

                    // Ignore if the server UUID is the same as this server
                    if (serverUUID != null && serverUUID.equals(this.service.serverUUID)) {
                        return;
                    }

                    boolean isOpped = data.getOpped();
                    String name = data.getName();
                    UUID uuid = UUID.fromString(data.getUuid());

                    if (isOpped) {
                        Log.info("Web-Socket: Player opped: " + name + " (" + uuid + ")");
                        this.serverControl.addOpPlayer(uuid, name);
                    } else {
                        Log.info("Web-Socket: Player deopped: " + name + " (" + uuid + ")");
                        this.serverControl.removeOpPlayer(uuid, name);
                    }
                }, OpEntry.class, String.class);
            }

            // Handle reconnection
            this.hubConnection.onClosed(error -> {
                Log.info("Web-Socket: Disconnected from server.");

                // Ignore if the latch is already at 0 (shutting down)
                if (this.latch.getCount() == 0) {
                    return;
                }

                // Wait 5 seconds before reconnecting
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {}

                // Loop until connected
                while (this.hubConnection.getConnectionState().equals(HubConnectionState.DISCONNECTED)) {
                    // Reconnect
                    hubConnection.start().subscribe(this::onConnected, this::onConnectError);

                    // Wait 5 seconds before checking again
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ignored) {}
                }
            });

            // Start the connection
            hubConnection.start().subscribe(this::onConnected, this::onConnectError);

            try {
                // Keep the thread alive
                latch.await();
            } catch (InterruptedException ignored) {
                Log.debug("WhitelistSocketThread interrupted. Exiting.");
                this.latch.countDown();
            }

            this.hubConnection.stop().blockingAwait();
            this.hubConnection.close();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    private void onConnected() {
        Log.info("Web-Socket: Connected to server");

        // Get latest whitelist and op list on connect
        this.service.pullDatabaseWhitelistToLocal();

        if (this.service.syncingOpList) {
            this.service.pullDatabaseOpsToLocal();
        }
        Log.info("Database sync complete");
    }

    private void onConnectError(Throwable error) {
        Log.error("Web-Socket: Connection error");
    }
}
