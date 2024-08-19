package net.rmnad.logging;

import net.rmnad.Log;

public class LogMessages {

    public static String SYNC_THREAD_STARTING = "Sync thread starting...";
    public static String SOCKET_THREAD_STARTING = "Web Socket thread starting...";
    public static String HELLO_MESSAGE = "Hello from Whitelist Sync 2!";

    // Errors
    public static String ERROR_INITIALIZING_WHITELIST_SYNC_DATABASE = "Error initializing whitelist sync database. Disabling mod functionality. Please correct errors and restart.";
    public static String ERROR_INITIALIZING_WHITELIST_SYNC_WEB_API = "Error initializing whitelist sync WEB API. Disabling mod functionality. Please correct errors and restart.";
    public static String ERROR_WHITELIST_MODE = "Please check what WHITELIST_MODE is set in the config and make sure it is set to a supported mode.";
    public static String ERROR_WHITELIST_SYNC_THREAD = "Error in the whitelist sync thread! Syncing will stop until the server is restarted.";

    public static String WARN_WHITELIST_NOT_ENABLED = "Oh no! I see whitelisting isn't enabled in the server properties. I assume this is not intentional, I'll enable it for you!";
    public static String OP_SYNC_ENABLED = "Opped Player Sync is enabled";
    public static String OP_SYNC_DISABLED = "Opped Player Sync is disabled";

    public static String ALERT_OP_SYNC_DISABLED = "Op list syncing is currently disabled in your config. Please enable it and restart the server to use this feature.";

    public static void ShowModStartupHeaderMessage() {
        Log.info("----------------------------------------------");
        Log.info("---------------WHITELIST SYNC 2---------------");
        Log.info("----------------------------------------------");
    }

    public static void ShowModStartupFooterMessage() {
        Log.info("----------------------------------------------");
        Log.info("----------------------------------------------");
        Log.info("----------------------------------------------");
    }


    // Service Messages

    // PushLocalWhitelistToDatabase
    public static String SuccessPushLocalWhitelistToDatabase(long timeTaken, int records) {
        return String.format("Whitelist table updated | Took %sms | Wrote %s records.", timeTaken, records);
    }
    public static String ERROR_PushLocalWhitelistToDatabase = "Error pushing local whitelist to database.";


    // PushLocalOpsToDatabase
    public static String SuccessPushLocalOpsToDatabase(long timeTaken, int records) {
        return String.format("Op table updated | Took %sms | Wrote %s records.", timeTaken, records);
    }
    public static String ERROR_PushLocalOpsToDatabase = "Failed to update database with local records.";


    // PullDatabaseWhitelistToLocal
    public static String SuccessPullDatabaseWhitelistToLocal(long timeTaken, int records) {
        return String.format("Copied whitelist database to local | Took %sms | Wrote %s records.", timeTaken, records);
    }
    public static String ERROR_PullDatabaseWhitelistToLocal = "Error querying whitelisted players from database!";

    // PullDatabaseOpsToLocal
    public static String SuccessPullDatabaseOpsToLocal(long timeTaken, int records) {
        return String.format("Copied op database to local | Took %sms | Wrote %s records.", timeTaken, records);
    }

    // GetWhitelistedPlayersFromDatabase
    public static String SuccessGetWhitelistedPlayersFromDatabase(long timeTaken, int records) {
        return String.format("Retrieved whitelist from database | Took %sms | Found %s records.", timeTaken, records);
    }

    // GetOppedPlayersFromDatabase
    public static String SuccessGetOppedPlayersFromDatabase(long timeTaken, int records) {
        return String.format("Retrieved op list from database | Took %sms | Found %s records.", timeTaken, records);
    }

    // Added user to whitelist
    public static String AddedUserToWhitelist(String name) {
        return String.format("Added %s to whitelist.", name);
    }

    // Removed user to whitelist
    public static String RemovedUserToWhitelist(String name) {
        return String.format("Removed %s from whitelist.", name);
    }

    // Added user to op list
    public static String OppedUser(String name) {
        return String.format("Opped %s.", name);
    }

    // Removed user to op list
    public static String DeopUser(String name) {
        return String.format("Deopped %s.", name);
    }
}
