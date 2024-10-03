package net.rmnad.services;

import com.microsoft.signalr.HttpHubConnectionBuilder;
import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import com.microsoft.signalr.HubConnectionState;
import net.rmnad.Log;
import net.rmnad.callbacks.*;
import net.rmnad.logging.LogMessages;
import net.rmnad.models.api.BannedIpEntry;
import net.rmnad.models.api.BannedPlayerEntry;
import net.rmnad.models.api.OpEntry;
import net.rmnad.models.api.WhitelistEntry;
import javax.net.ssl.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

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

                for (WhitelistEntry entry : data) {
                    boolean isWhitelisted = entry.getWhitelisted();
                    String name = entry.getName();
                    UUID uuid = UUID.fromString(entry.getUuid());

                    if (isWhitelisted) {
                        Log.info("Web-Socket: Player whitelisted: " + name + " (" + uuid + ")");
                        this.serverControl.addWhitelistPlayer(uuid, name);
                    } else {
                        Log.info("Web-Socket: Player removed from whitelist: " + name + " (" + uuid + ")");
                        this.serverControl.removeWhitelistPlayer(uuid, name);
                    }
                }
            }, WhitelistEntry[].class, String.class);

            if (this.service.syncingOpList) {
                this.hubConnection.on("OpUpdated", (data, serverUuidStr) -> {

                    UUID serverUUID = serverUuidStr != null ? UUID.fromString(serverUuidStr) : null;

                    // Ignore if the server UUID is the same as this server
                    if (serverUUID != null && serverUUID.equals(this.service.serverUUID)) {
                        return;
                    }

                    for (OpEntry player : data) {
                        boolean isOpped = player.getOpped();
                        String name = player.getName();
                        UUID uuid = UUID.fromString(player.getUuid());

                        if (isOpped) {
                            Log.info("Web-Socket: Player opped: " + name + " (" + uuid + ")");
                            this.serverControl.addOpPlayer(uuid, name);
                        } else {
                            Log.info("Web-Socket: Player deopped: " + name + " (" + uuid + ")");
                            this.serverControl.removeOpPlayer(uuid, name);
                        }
                    }

                }, OpEntry[].class, String.class);
            }

            if (this.service.syncingBannedPlayers) {
                this.hubConnection.on("BannedPlayerUpdated", (data, serverUuidStr) -> {

                    UUID serverUUID = serverUuidStr != null ? UUID.fromString(serverUuidStr) : null;

                    // Ignore if the server UUID is the same as this server
                    if (serverUUID != null && serverUUID.equals(this.service.serverUUID)) {
                        return;
                    }

                    for (BannedPlayerEntry player : data) {
                        boolean isBanned = player.getBanned();
                        String name = player.getName();
                        UUID uuid = UUID.fromString(player.getUuid());
                        String reason = player.getReason();

                        if (isBanned) {
                            Log.info("Web-Socket: Player banned: " + name + " (" + uuid + "). Reason: " + reason);
                            this.serverControl.addBannedPlayer(uuid, name, reason);
                        } else {
                            Log.info("Web-Socket: Player unbanned: " + name + " (" + uuid + ")");
                            this.serverControl.removeBannedPlayer(uuid, name);
                        }
                    }

                }, BannedPlayerEntry[].class, String.class);
            }

            if (this.service.syncingBannedIps) {
                this.hubConnection.on("BannedIpUpdated", (data, serverUuidStr) -> {

                    UUID serverUUID = serverUuidStr != null ? UUID.fromString(serverUuidStr) : null;

                    // Ignore if the server UUID is the same as this server
                    if (serverUUID != null && serverUUID.equals(this.service.serverUUID)) {
                        return;
                    }

                    for (BannedIpEntry player : data) {
                        boolean isBanned = player.getBanned();
                        String ip = player.getIp();
                        String reason = player.getReason();

                        if (isBanned) {
                            Log.info("Web-Socket: Ip banned: " + ip + ". Reason: " + reason);
                            this.serverControl.addBannedIp(ip, reason);
                        } else {
                            Log.info("Web-Socket: Ip unbanned: " + ip + ".");
                            this.serverControl.removeBannedIp(ip);
                        }
                    }

                }, BannedIpEntry[].class, String.class);
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

        if (this.service.syncingBannedPlayers) {
            this.service.pullDatabaseBannedPlayersToLocal();
        }

        if (this.service.syncingBannedIps) {
            this.service.pullDatabaseBannedIpsToLocal();
        }

        Log.info("Database sync complete");
    }

    private void onConnectError(Throwable error) {
        Log.error("Web-Socket: Connection error");
    }
}
