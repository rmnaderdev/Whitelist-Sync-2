package net.rmnad.core.services;

import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SslUtilTest {

    @Test
    void isLocalHostTrueForHttpsLocalhost() {
        assertTrue(SslUtil.isLocalHost("https://localhost:5173/"));
    }

    @Test
    void isLocalHostFalseForRemoteHost() {
        assertFalse(SslUtil.isLocalHost("https://whitelistsync.com/"));
    }

    @Test
    void isLocalHostFalseForNull() {
        assertFalse(SslUtil.isLocalHost(null));
    }

    @Test
    void isLocalHostFalseForPlainHttpLocalhost() {
        // Only the https dev host gets the trust-all treatment.
        assertFalse(SslUtil.isLocalHost("http://localhost"));
    }

    @Test
    void trustAllContextIsInitialised() {
        SSLContext context = SslUtil.trustAllContext();
        assertNotNull(context);
        assertNotNull(context.getSocketFactory());
    }

    @Test
    void trustAllManagerHasNoAcceptedIssuers() {
        assertEquals(0, SslUtil.TRUST_ALL.getAcceptedIssuers().length);
    }
}
