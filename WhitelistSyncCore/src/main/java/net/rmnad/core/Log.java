package net.rmnad.core;

import java.util.logging.Level;
import java.util.logging.Logger;

// Ideas borrowed from Dynmap's Logging implementation
public class Log {
    private static Logger log = Logger.getLogger("WhitelistSync2");
    private static String prefix = "";
    private static WhitelistSyncLogger dlog = null;
    public static boolean verbose = false;

    public static String safeString(String s) { return s.replaceAll("[\\${}]", "_"); }

    public static void setLogger(Logger logger, String pre) {
        log = logger;
        if((pre != null) && (!pre.isEmpty()))
            prefix = pre + " ";
        else
            prefix = "";
    }

    public static void setLogger(WhitelistSyncLogger logger) {
        dlog = logger;
    }

    public static void info(String msg) {
        msg = safeString(msg);
        if (dlog != null) {
            dlog.info(msg);
        }
        else {
            log.log(Level.INFO, prefix + msg);
        }
    }
    public static void debug(String msg) {
        if(verbose) {
            msg = safeString(msg);
            if (dlog != null) {
                dlog.info(msg);
            }
            else {
                log.log(Level.INFO, prefix + msg);
            }
        }
    }
    public static void error(Throwable e) {
        if (dlog != null) {
            dlog.error(e);
        }
        else {
            log.log(Level.SEVERE, prefix + "Exception occured: ", e);
        }
    }
    public static void error(String msg) {
        msg = safeString(msg);
        if (dlog != null) {
            dlog.error(msg);
        }
        else {
            log.log(Level.SEVERE, prefix + msg);
        }
    }
    public static void error(String msg, Throwable e) {
        msg = safeString(msg);
        if (dlog != null) {
            dlog.error(msg, e);
        }
        else {
            log.log(Level.SEVERE, prefix + msg, e);
        }
    }
    public static void warning(String msg) {
        msg = safeString(msg);
        if (dlog != null) {
            dlog.warning(msg);
        }
        else {
            log.log(Level.WARNING, prefix + msg);
        }
    }
    public static void warning(String msg, Throwable e) {
        msg = safeString(msg);
        if (dlog != null) {
            dlog.warning(msg, e);
        }
        else {
            log.log(Level.WARNING, prefix + msg, e);
        }
    }
}
