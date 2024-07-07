package net.rmnad;

// Ideas borrowed from Dynmap's Logging implementation
public interface WhitelistSyncLogger {
    public void info(String msg);
    public void debug(String msg);
    public void error(Throwable e);
    public void error(String msg);
    public void error(String msg, Throwable e);
    public void warning(String msg);
    public void warning(String msg, Throwable e);
}
