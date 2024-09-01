package net.rmnad.services;

import com.google.gson.internal.LinkedTreeMap;
import com.microsoft.signalr.HttpHubConnectionBuilder;
import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import io.reactivex.rxjava3.core.Single;
import net.rmnad.Log;
import net.rmnad.callbacks.*;
import net.rmnad.logging.LogMessages;
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

            Dispatcher dispatcher = new Dispatcher();

            HttpHubConnectionBuilder hubConnectionBuilder = HubConnectionBuilder.create(this.service.getApiHost() + "/hubs/whitelistsyncmodhub")
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
                        .dispatcher(dispatcher)
                        .readTimeout(1, TimeUnit.MINUTES)
                        .sslSocketFactory(sslContext.getSocketFactory(), trustManager)
                        .hostnameVerifier((hostname, session) -> true));
            } else {
                hubConnectionBuilder = hubConnectionBuilder.setHttpClientBuilderCallback(httpClientBuilder -> httpClientBuilder
                        .dispatcher(dispatcher)
                        .readTimeout(1, TimeUnit.MINUTES));
            }

            this.hubConnection = hubConnectionBuilder
                    .build();

            this.hubConnection.onClosed(error -> {
                Log.info("Web-Socket: Disconnected from server. Retrying in 5 seconds...");

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {}
                // Reconnect
                this.hubConnection.start().blockingAwait();
            });

            // Define events
//            this.hubConnection.on("", args -> {
//                Log.info("Web-Socket: Connected to server");
//
//                // Get latest whitelist and op list on connect
//                this.service.pullDatabaseWhitelistToLocal();
//
//                if (this.service.syncingOpList) {
//                    this.service.pullDatabaseOpsToLocal();
//                }
//                Log.info("Database sync complete");
//            });

//            this.socket.on(Socket.EVENT_DISCONNECT, args -> {
//                Log.info("Web-Socket: Disconnected from server");
//            });
//
//            this.socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
//                Log.error("Web-Socket: Connection error");
//            });

            this.hubConnection.on("WhitelistUpdated", (data, serverUuid) -> {

                // Ignore if the server UUID is the same as this server
                if (serverUuid != null && serverUuid.equals(this.service.serverUUID.toString())) {
                    return;
                }

                try {
                    LinkedTreeMap whitelist = data;
                    boolean isWhitelisted = ((boolean) whitelist.get("isWhitelisted"));
                    String name = data.getString("name");
                    UUID uuid = UUID.fromString(data.getString("uuid"));

                    if (isWhitelisted) {
                        Log.info("Web-Socket: Player whitelisted: " + name + " (" + uuid + ")");
                        this.serverControl.addWhitelistPlayer(uuid, name);
                    } else {
                        Log.info("Web-Socket: Player removed from whitelist: " + name + " (" + uuid + ")");
                        this.serverControl.removeWhitelistPlayer(uuid, name);
                    }
                } catch (JSONException e) {
                    Log.error("Web-Socket: Error parsing JSON data: " + e.getMessage());
                }
            }, LinkedTreeMap.class, String.class);

            if (this.service.syncingOpList) {
                this.hubConnection.on("OpUpdated", (data, serverUuid) -> {
//                    try {
//                        JSONObject data = (JSONObject) args[0];
//                        boolean isOpped = data.getBoolean("isOpped");
//                        String name = data.getString("name");
//                        UUID uuid = UUID.fromString(data.getString("uuid"));
//
//                        if (isOpped) {
//                            Log.info("Web-Socket: Player opped: " + name + " (" + uuid + ")");
//                            this.serverControl.addOpPlayer(uuid, name);
//                        } else {
//                            Log.info("Web-Socket: Player deopped: " + name + " (" + uuid + ")");
//                            this.serverControl.removeOpPlayer(uuid, name);
//                        }
//                    } catch (JSONException e) {
//                        Log.error("Web-Socket: Error parsing JSON data: " + e.getMessage());
//                    }
                    Log.info("Web-Socket: Player opped: " + data.toString());
                }, Object.class, String.class);
            }

            hubConnection.start().blockingAwait();

//            try {
//                // Keep the thread alive
//                latch.await();
//            } catch (InterruptedException ignored) {
//                Log.debug("WhitelistSocketThread interrupted. Exiting.");
//            }
//
//            if (this.socket != null && this.socket.connected()) {
//                this.latch.countDown();
//                this.socket.disconnect();
//                this.socket.close();
//            }

            //dispatcher.executorService().shutdown();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }
}
