package net.rmnad.fabric_1_21_8;

import net.rmnad.core.WhitelistSyncLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Ideas borrowed from Dynmap's Logging implementation
public class FabricLogger implements WhitelistSyncLogger {

    public static final Logger LOGGER = LoggerFactory.getLogger(WhitelistSync2.MODID);

    @Override
    public void info(String msg) {
        LOGGER.info(msg);
    }

    @Override
    public void debug(String msg) {
        LOGGER.debug(msg);
    }

    @Override
    public void error(Throwable e) {
        LOGGER.error(String.valueOf(e));
    }

    @Override
    public void error(String msg) {
        LOGGER.error(msg);
    }

    @Override
    public void error(String msg, Throwable e) {
        LOGGER.error(msg, e);
    }

    @Override
    public void warning(String msg) {
        LOGGER.warn(msg);
    }

    @Override
    public void warning(String msg, Throwable e) {
        LOGGER.warn(msg, e);
    }
}
