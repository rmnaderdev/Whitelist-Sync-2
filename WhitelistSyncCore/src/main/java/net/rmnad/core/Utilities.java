package net.rmnad.core;

import net.rmnad.core.models.BannedPlayer;
import net.rmnad.core.models.OppedPlayer;
import net.rmnad.core.models.WhitelistedPlayer;

import java.util.ArrayList;

public class Utilities {

    public static String FormatOppedPlayersOutput(ArrayList<OppedPlayer> oppedPlayers) {
        String outstr = "";

        if(oppedPlayers.isEmpty()) {
            outstr = "Op list is empty";
        } else {
            for(int i = 0; i < oppedPlayers.size(); i++) {

                if(i % 5 == 0 && i != 0) {
                    outstr += "\n";
                }

                if(i == oppedPlayers.size() - 1) {
                    outstr += oppedPlayers.get(i).getName();
                } else {
                    outstr += oppedPlayers.get(i).getName() + ", ";
                }

            }
        }

        return outstr;
    }

    public static String FormatWhitelistedPlayersOutput(ArrayList<WhitelistedPlayer> whitelistedPlayers) {
        String outstr = "";

        if(whitelistedPlayers.isEmpty()) {
            outstr = "Whitelist is empty";
        } else {
            for(int i = 0; i < whitelistedPlayers.size(); i++) {

                if(i % 5 == 0 && i != 0) {
                    outstr += "\n";
                }

                if(i == whitelistedPlayers.size() - 1) {
                    outstr += whitelistedPlayers.get(i).getName();
                } else {
                    outstr += whitelistedPlayers.get(i).getName() + ", ";
                }

            }
        }

        return outstr;
    }

    public static String FormatBannedPlayersOutput(ArrayList<BannedPlayer> bannedPlayers) {
        String outstr = "";

        if(bannedPlayers.isEmpty()) {
            outstr = "Ban list is empty";
        } else {
            for(int i = 0; i < bannedPlayers.size(); i++) {

                if(i % 5 == 0 && i != 0) {
                    outstr += "\n";
                }

                if(i == bannedPlayers.size() - 1) {
                    outstr += bannedPlayers.get(i).getName();
                } else {
                    outstr += bannedPlayers.get(i).getName() + ", ";
                }

            }
        }

        return outstr;
    }

    public static String FormatBannedIPsOutput(ArrayList<String> bannedIPs) {
        String outstr = "";

        if(bannedIPs.isEmpty()) {
            outstr = "Ban list is empty";
        } else {
            for(int i = 0; i < bannedIPs.size(); i++) {

                if(i % 5 == 0 && i != 0) {
                    outstr += "\n";
                }

                if(i == bannedIPs.size() - 1) {
                    outstr += bannedIPs.get(i);
                } else {
                    outstr += bannedIPs.get(i) + ", ";
                }

            }
        }

        return outstr;
    }

}
