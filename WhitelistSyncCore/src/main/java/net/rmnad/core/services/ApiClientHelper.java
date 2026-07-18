package net.rmnad.core.services;

import okhttp3.OkHttpClient;

import javax.net.ssl.SSLContext;

public class ApiClientHelper {

    private final String apiHost;
    private final String apiKey;
    private final OkHttpClient client;

    public ApiClientHelper(String apiHost, String apiKey) {
        this.apiHost = apiHost;
        this.apiKey = apiKey;
        this.client = buildClient(apiHost);
    }

    // Built once and reused. SSL setup exceptions are handled inside SslUtil so
    // callers never have to deal with NoSuchAlgorithmException/KeyManagementException.
    public OkHttpClient getClient() {
        return client;
    }

    private static OkHttpClient buildClient(String apiHost) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        if (SslUtil.isLocalHost(apiHost)) {
            SSLContext sslContext = SslUtil.trustAllContext();
            builder.hostnameVerifier((hostname, session) -> true)
                    .sslSocketFactory(sslContext.getSocketFactory(), SslUtil.TRUST_ALL);
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

}
