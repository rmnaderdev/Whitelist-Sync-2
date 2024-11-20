package net.rmnad.core.services;

import com.microsoft.signalr.*;
import net.rmnad.core.Log;
import net.rmnad.core.WhitelistSyncCore;
import net.rmnad.core.callbacks.IServerControl;
import net.rmnad.core.logging.LogMessages;
import net.rmnad.core.models.api.BannedIpEntry;
import net.rmnad.core.models.api.BannedPlayerEntry;
import net.rmnad.core.models.api.OpEntry;
import net.rmnad.core.models.api.WhitelistEntry;
import javax.net.ssl.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.UUID;

public class WhitelistSocketThread extends Thread {

    private final WebService service;
    private final boolean errorOnSetup;

    private final IServerControl serverControl;

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
                    .create(this.service.apiClientHelper.getApiHost() + "/hubs/whitelistsyncmodhub")
                    .withHeader("X-Api-Key", this.service.apiClientHelper.getApiKey())
                    .withHeader("server-uuid", this.service.serverUUID.toString());

            if (this.service.apiClientHelper.getApiHost().startsWith("https://localhost")) {
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
                        .sslSocketFactory(sslContext.getSocketFactory(), trustManager)
                        .hostnameVerifier((hostname, session) -> true));
            }

            HubConnection hubConnection = hubConnectionBuilder.build();

            hubConnection.on("SyncRequested", () -> {
                Log.info("WebSyncThread: Sync requested from server.");
                this.service.pullDatabaseWhitelistToLocal();

                if (WhitelistSyncCore.CONFIG.syncOpList) {
                    this.service.pullDatabaseOpsToLocal();
                }

                if (WhitelistSyncCore.CONFIG.webSyncBannedPlayers) {
                    this.service.pullDatabaseBannedPlayersToLocal();
                }

                if (WhitelistSyncCore.CONFIG.webSyncBannedIps) {
                    this.service.pullDatabaseBannedIpsToLocal();
                }
            });

            hubConnection.on("WhitelistUpdated", (data, serverUuidStr) -> {

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
                        Log.info("WebSyncThread: Player whitelisted: " + name + " (" + uuid + ")");
                        this.serverControl.addWhitelistPlayer(uuid, name);
                    } else {
                        Log.info("WebSyncThread: Player removed from whitelist: " + name + " (" + uuid + ")");
                        this.serverControl.removeWhitelistPlayer(uuid, name);
                    }
                }
            }, WhitelistEntry[].class, String.class);

            if (WhitelistSyncCore.CONFIG.syncOpList) {
                hubConnection.on("OpUpdated", (data, serverUuidStr) -> {

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
                            Log.info("WebSyncThread: Player opped: " + name + " (" + uuid + ")");
                            this.serverControl.addOpPlayer(uuid, name);
                        } else {
                            Log.info("WebSyncThread: Player deopped: " + name + " (" + uuid + ")");
                            this.serverControl.removeOpPlayer(uuid, name);
                        }
                    }

                }, OpEntry[].class, String.class);
            }

            if (WhitelistSyncCore.CONFIG.webSyncBannedPlayers) {
                hubConnection.on("BannedPlayerUpdated", (data, serverUuidStr) -> {

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
                            Log.info("WebSyncThread: Player banned: " + name + " (" + uuid + "). Reason: " + reason);
                            this.serverControl.addBannedPlayer(uuid, name, reason);
                        } else {
                            Log.info("WebSyncThread: Player unbanned: " + name + " (" + uuid + ")");
                            this.serverControl.removeBannedPlayer(uuid, name);
                        }
                    }

                }, BannedPlayerEntry[].class, String.class);
            }

            if (WhitelistSyncCore.CONFIG.webSyncBannedIps) {
                hubConnection.on("BannedIpUpdated", (data, serverUuidStr) -> {

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
                            Log.info("WebSyncThread: Ip banned: " + ip + ". Reason: " + reason);
                            this.serverControl.addBannedIp(ip, reason);
                        } else {
                            Log.info("WebSyncThread: Ip unbanned: " + ip + ".");
                            this.serverControl.removeBannedIp(ip);
                        }
                    }

                }, BannedIpEntry[].class, String.class);
            }

            hubConnection.onClosed(error -> {
                Log.info("WebSyncThread: Disconnected from server.");
            });

            try {
                // Start the connection
                hubConnection.start().blockingAwait();
            } catch (HttpRequestException e) {
                if (e.getStatusCode() == 401) {
                    // Auth failed
                    Log.error("WebSyncThread: Authentication error: Invalid API key. Exiting.");
                    this.interrupt();
                } else if (e.getStatusCode() == 403) {
                    // Connected client limit reached
                    String errorMessage = e.getMessage();
                    Log.error("WebSyncThread: " + errorMessage + ". Exiting.");
                    this.interrupt();
                }
            }

            try {
                while (true) {
                    if (hubConnection.getConnectionState().equals(HubConnectionState.DISCONNECTED)) {
                        Thread.sleep(5000);

                        Log.info("WebSyncThread: Disconnected from server. Attempting to reconnect.");

                        try {
                            // Start the connection
                            hubConnection.start().blockingAwait();
                        } catch (HttpRequestException e) {
                            if (e.getStatusCode() == 401) {
                                // Auth failed
                                Log.error("WebSyncThread: Authentication error: Invalid API key. Exiting.");
                                this.interrupt();
                            } else if (e.getStatusCode() == 403) {
                                // Connected client limit reached
                                Log.error("WebSyncThread: Connected client limit reached. Please disconnect other servers and try reconnecting using the command '/wl restart'. Exiting.");
                                this.interrupt();
                            } else {
                                Log.error("WebSyncThread: Failed to reconnect to server. Retrying in 5 seconds.");
                            }
                        } catch (Exception e) {
                            Log.error("WebSyncThread: Failed to reconnect to server. Retrying in 5 seconds.");
                        }
                    }

                    Thread.sleep(5000);
                }
            } catch (InterruptedException ignored) {
                Log.debug("WhitelistSocketThread interrupted. Exiting.");
            }

            Log.info("WebSyncThread: Stopping...");
            hubConnection.stop().blockingAwait();
            hubConnection.close();
            Log.info("WebSyncThread: Stopped");
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

}
