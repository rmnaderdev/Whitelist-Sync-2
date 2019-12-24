package pw.twpi.whitelistsync2;

import java.util.ArrayList;

public class Utilities {

    public static String FormatOpUsersOutput(ArrayList<String> names) {
        String outstr = "";

        if(names.isEmpty()) {
            outstr = "Op list is empty";
        } else {
            for(int i = 0; i < names.size(); i++) {

                if(i % 5 == 0 && i != 0) {
                    outstr += "\n";
                }

                if(i == names.size() - 1) {
                    outstr += names.get(i);
                } else {
                    outstr += names.get(i) + ", ";
                }

            }
        }

        return outstr;
    }

    public static String FormatWhitelistUsersOutput(ArrayList<String> names) {
        String outstr = "";

        if(names.isEmpty()) {
            outstr = "Whitelist is empty";
        } else {
            for(int i = 0; i < names.size(); i++) {

                if(i % 5 == 0 && i != 0) {
                    outstr += "\n";
                }

                if(i == names.size() - 1) {
                    outstr += names.get(i);
                } else {
                    outstr += names.get(i) + ", ";
                }

            }
        }

        return outstr;
    }

}
