package net.rmnad.forge_1_19;

import net.rmnad.WhitelistSyncLogger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// Ideas borrowed from Dynmap's Logging implementation
public class ForgeLogger implements WhitelistSyncLogger {

    public static final Logger LOGGER = LogManager.getLogger(WhitelistSync2.MODID);

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
        LOGGER.error(e);
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
