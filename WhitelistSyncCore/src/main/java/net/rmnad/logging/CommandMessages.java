package net.rmnad.logging;

public class CommandMessages {

    public static String AddedToWhitelist(String playerName) {
        return String.format("Added %s to whitelist database.", playerName);
    }

    public static String AlreadyWhitelist(String playerName) {
        return String.format("%s is already whitelisted.", playerName);
    }

    public static String RemovedFromWhitelist(String playerName) {
        return String.format("Removed %s from whitelist database.", playerName);
    }

    public static String NotWhitelisted(String playerName) {
        return String.format("%s is not whitelisted.", playerName);
    }


    public static String AddedToOpList(String playerName) {
        return String.format("Opped %s in database.", playerName);
    }

    public static String AlreadyInOpList(String playerName) {
        return String.format("%s is already opped.", playerName);
    }

    public static String RemovedFromOpList(String playerName) {
        return String.format("Deopped %s from database.", playerName);
    }

    public static String NotInOpList(String playerName) {
        return String.format("%s is not opped.", playerName);
    }
}
