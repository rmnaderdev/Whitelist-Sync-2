package net.rmnad.core.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiClientHelperTest {

    @Test
    void getApiHostStripsSingleTrailingSlash() {
        ApiClientHelper helper = new ApiClientHelper("https://example.com/", "key");
        assertEquals("https://example.com", helper.getApiHost());
    }

    @Test
    void getApiHostLeavesHostWithoutTrailingSlash() {
        ApiClientHelper helper = new ApiClientHelper("https://example.com", "key");
        assertEquals("https://example.com", helper.getApiHost());
    }

    @Test
    void getApiKeyReturnsConfiguredKey() {
        ApiClientHelper helper = new ApiClientHelper("https://example.com", "abc123");
        assertEquals("abc123", helper.getApiKey());
    }

    @Test
    void getClientIsCachedAndReused() {
        ApiClientHelper helper = new ApiClientHelper("https://example.com", "key");
        assertEquals(helper.getClient(), helper.getClient());
    }
}
