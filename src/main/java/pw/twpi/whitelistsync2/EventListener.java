package pw.twpi.whitelistsync2;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import pw.twpi.whitelistsync2.commands.op.OpCommands;
import pw.twpi.whitelistsync2.commands.whitelist.WhitelistCommands;

public class EventListener {

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event){
        new WhitelistCommands(event.getDispatcher());

        if(Config.SYNC_OP_LIST.get()) {
            WhitelistSync2.LOGGER.info("Opped Player Sync is enabled");
            new OpCommands(event.getDispatcher());
        } else {
            WhitelistSync2.LOGGER.info("Opped Player Sync is disabled");
        }
    }

    @SubscribeEvent
    public void onServerStarted(FMLServerStartedEvent event) {
        WhitelistSync2.SetupWhitelistSync(event.getServer());
    }
}
