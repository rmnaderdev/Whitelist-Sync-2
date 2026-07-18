package net.rmnad.core.services;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

/**
 * Shared TLS helpers.
 * <p>
 * The trust-all configuration is intentionally applied only to local development
 * hosts ({@code https://localhost...}), where the web service is expected to use a
 * self-signed certificate. It must never be used against a real remote host.
 */
public final class SslUtil {

    private SslUtil() {}

    /** A trust manager that accepts every certificate. Localhost/dev use only. */
    public static final X509TrustManager TRUST_ALL = new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[] {};
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
            // Intentionally trust all — localhost/dev only.
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
            // Intentionally trust all — localhost/dev only.
        }
    };

    /**
     * Builds an SSLContext backed by {@link #TRUST_ALL}. The checked exceptions
     * from the JCE are wrapped here, in one place, so callers do not each have to
     * declare or rethrow them.
     */
    public static SSLContext trustAllContext() {
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[] { TRUST_ALL }, null);
            return context;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException("Failed to initialise trust-all SSL context", e);
        }
    }

    /** True for the local development hosts that use a self-signed certificate. */
    public static boolean isLocalHost(String apiHost) {
        return apiHost != null && apiHost.startsWith("https://localhost");
    }
}
