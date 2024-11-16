package net.rmnad.forge_1_16_5;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.rmnad.Log;
import net.rmnad.WhitelistSyncCore;
import net.rmnad.config.WhitelistSyncConfig;
import net.rmnad.logging.LogMessages;

@Mod(WhitelistSync2.MODID)
public class WhitelistSync2
{
    public static final String MODID = "whitelistsync2";

    public WhitelistSync2() {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(CommandsListener.class);
        Log.setLogger(new ForgeLogger());
        Log.info(LogMessages.HELLO_MESSAGE);

        // Register config
        WhitelistSyncCore.LoadConfig();
    }

    // Command Registration
    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event){
        WhitelistSyncCommands.registerCommands(event.getDispatcher());

        if(WhitelistSyncConfig.Config.isSyncOpList()) {
            Log.info(LogMessages.OP_SYNC_ENABLED);
        } else {
            Log.info(LogMessages.OP_SYNC_DISABLED);
        }
    }

    // Server Started Event
    @SubscribeEvent
    public void onServerStarted(FMLServerStartedEvent event) {
        WhitelistSyncCore.SetupWhitelistSync(new ServerControl(event.getServer()));
    }

    @SubscribeEvent
    public void onServerStopping(FMLServerStoppingEvent event) {
        WhitelistSyncCore.ShutdownWhitelistSync();
    }
}
