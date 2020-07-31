package pw.twpi.whitelistsync2;

import java.util.ArrayList;
import net.minecraft.server.MinecraftServer;
import pw.twpi.whitelistsync2.models.OppedPlayer;
import pw.twpi.whitelistsync2.models.WhitelistedPlayer;

/**
 * Utility class to help keep main class clean
 * @author Richard Nader, Jr. <rmnader@svsu.edu>
 */
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
    
}
