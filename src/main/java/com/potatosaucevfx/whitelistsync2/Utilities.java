package com.potatosaucevfx.whitelistsync2;

import com.potatosaucevfx.whitelistsync2.services.BaseService;
import com.potatosaucevfx.whitelistsync2.services.SyncThread;
import java.util.ArrayList;
import net.minecraft.server.MinecraftServer;

/**
 * Utility class to help keep main class clean
 * @author Richard Nader, Jr. <nader1rm@cmich.edu>
 */
public class Utilities {
    
    public static void StartSyncThread(MinecraftServer server, BaseService service) {
        Thread sync = new Thread(new SyncThread(server, service));
        sync.start();
        WhitelistSync2.logger.info("Sync Thread Started!");
    }
    
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
