package net.rmnad.services;

import io.socket.client.IO;
import io.socket.client.Socket;
import net.rmnad.Log;
import net.rmnad.callbacks.IOnUserOpAdd;
import net.rmnad.callbacks.IOnUserOpRemove;
import net.rmnad.callbacks.IOnUserWhitelistAdd;
import net.rmnad.callbacks.IOnUserWhitelistRemove;
import net.rmnad.logging.LogMessages;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.*;
import java.net.URI;
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

    private final IOnUserWhitelistAdd onUserAdd;
    private final IOnUserWhitelistRemove onUserRemove;
    private final IOnUserOpAdd onUserOpAdd;
    private final IOnUserOpRemove onUserOpRemove;

    private Socket socket;
    private final CountDownLatch latch = new CountDownLatch(1);

    public WhitelistSocketThread(
            WebService service,
            boolean errorOnSetup,
            IOnUserWhitelistAdd onUserAdd,
            IOnUserWhitelistRemove onUserRemove,
            IOnUserOpAdd onUserOpAdd,
            IOnUserOpRemove onUserOpRemove) {

        this.setName("WhitelistSocketThread");
        this.setDaemon(true);
        this.service = service;
        this.errorOnSetup = errorOnSetup;

        this.onUserAdd = onUserAdd;
        this.onUserRemove = onUserRemove;
        this.onUserOpAdd = onUserOpAdd;
        this.onUserOpRemove = onUserOpRemove;
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

            HashMap<String, String> auth = new HashMap<>();
            auth.put("token", this.service.getApiKey());

            HostnameVerifier hostnameVerifier = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession sslSession) {
                    return true;
                }
            };

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

            Dispatcher dispatcher = new Dispatcher();
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .dispatcher(dispatcher)
                    .hostnameVerifier(hostnameVerifier)
                    .sslSocketFactory(sslContext.getSocketFactory(), trustManager)
                    .readTimeout(1, TimeUnit.MINUTES) // important for HTTP long-polling
                    .build();

            URI uri = URI.create(this.service.getApiHost());
            IO.Options options = IO.Options.builder()
                    .setAuth(auth)
                    .build();

            options.callFactory = okHttpClient;
            options.webSocketFactory = okHttpClient;

            this.socket = IO.socket(uri, options);

            // Define events
            this.socket.on(Socket.EVENT_CONNECT, args -> {
                Log.info("Web-Socket: Connected to server");
            });

            this.socket.on(Socket.EVENT_DISCONNECT, args -> {
                Log.info("Web-Socket: Disconnected from server");
            });

            this.socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
                Log.error("Web-Socket: Connection error");
            });

            this.socket.on("whitelist-update", args -> {
                try {
                    JSONObject data = (JSONObject) args[0];
                    boolean isWhitelisted = data.getBoolean("isWhitelisted");
                    String name = data.getString("name");
                    UUID uuid = UUID.fromString(data.getString("uuid"));

                    if (isWhitelisted) {
                        Log.info("Web-Socket: Player whitelisted: " + name + " (" + uuid + ")");
                        this.onUserAdd.call(uuid, name);
                    } else {
                        Log.info("Web-Socket: Player removed from whitelist: " + name + " (" + uuid + ")");
                        this.onUserRemove.call(uuid, name);
                    }
                } catch (JSONException e) {
                    Log.error("Web-Socket: Error parsing JSON data: " + e.getMessage());
                }
            });

            if (this.service.syncingOpList) {
                this.socket.on("op-update", args -> {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        boolean isOpped = data.getBoolean("isOpped");
                        String name = data.getString("name");
                        UUID uuid = UUID.fromString(data.getString("uuid"));

                        if (isOpped) {
                            Log.info("Web-Socket: Player opped: " + name + " (" + uuid + ")");
                            this.onUserOpAdd.call(uuid, name);
                        } else {
                            Log.info("Web-Socket: Player deopped: " + name + " (" + uuid + ")");
                            this.onUserOpRemove.call(uuid, name);
                        }
                    } catch (JSONException e) {
                        Log.error("Web-Socket: Error parsing JSON data: " + e.getMessage());
                    }
                });
            }

            this.socket.connect();

            try {
                // Keep the thread alive
                latch.await();
            } catch (InterruptedException ignored) {
                Log.debug("WhitelistSocketThread interrupted. Exiting.");
            }

            if (this.socket != null && this.socket.connected()) {
                this.latch.countDown();
                this.socket.disconnect();
                this.socket.close();
                dispatcher.executorService().shutdown();
            }
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }
}
