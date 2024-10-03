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


    public static String BannedPlayer(String playerName) {
        return String.format("Banned %s in database.", playerName);
    }

    public static String AlreadyBanned(String playerName) {
        return String.format("%s is already banned.", playerName);
    }

    public static String UnbannedPlayer(String playerName) {
        return String.format("Unbanned %s from database.", playerName);
    }

    public static String NotBanned(String playerName) {
        return String.format("%s is not banned.", playerName);
    }

    public static String BannedIP(String ip) {
        return String.format("Banned IP %s in database.", ip);
    }

    public static String AlreadyBannedIP(String ip) {
        return String.format("IP %s is already banned.", ip);
    }

    public static String UnbannedIP(String ip) {
        return String.format("Unbanned IP %s from database.", ip);
    }

    public static String NotBannedIP(String ip) {
        return String.format("IP %s is not banned.", ip);
    }
}
