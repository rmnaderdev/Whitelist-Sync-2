package net.rmnad.core.config;

import net.rmnad.core.config.WhitelistSyncConfig.DatabaseMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WhitelistSyncConfigTest {

    @Test
    void defaultsMatchExpectedValues() {
        WhitelistSyncConfig config = new WhitelistSyncConfig();

        assertEquals(DatabaseMode.SQLITE, config.databaseMode);
        assertEquals(60, config.syncTimer);
        assertFalse(config.syncOpList);
        assertFalse(config.verboseLogging);
        assertFalse(config.syncBannedPlayers);
        assertFalse(config.syncBannedIps);
        assertFalse(config.mysqlUseSsl);
        assertEquals(3306, config.mysqlPort);
    }

    @Test
    void configSpecAcceptsAFreshlyDefaultedConfig() {
        // The spec's defaults must line up with the field defaults above; if they
        // drift, load() would rewrite the user's file on every startup.
        assertEquals("WhitelistSync", new WhitelistSyncConfig().mysqlDbName);
    }
}
