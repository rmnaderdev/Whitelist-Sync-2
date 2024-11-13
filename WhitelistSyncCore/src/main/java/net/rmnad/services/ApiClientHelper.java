package net.rmnad.services;

import okhttp3.OkHttpClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

public class ApiClientHelper {

    private final String apiHost;
    private final String apiKey;

    public ApiClientHelper(String apiHost, String apiKey) {
        this.apiHost = apiHost;
        this.apiKey = apiKey;
    }

    public OkHttpClient getClient() throws NoSuchAlgorithmException, KeyManagementException {

        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        if (this.apiHost.contains("https://localhost")) {
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


            builder.hostnameVerifier((hostname, session) -> true)
                    .sslSocketFactory(sslContext.getSocketFactory(), trustManager);
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
