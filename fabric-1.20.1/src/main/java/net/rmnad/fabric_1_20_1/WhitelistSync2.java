package net.rmnad.fabric_1_20_1;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.rmnad.core.Log;
import net.rmnad.core.WhitelistSyncCore;
import net.rmnad.core.logging.LogMessages;

public class WhitelistSync2 implements ModInitializer {
	public static final String MODID = "whitelistsync2";

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> {
			WhitelistSyncCommands.registerCommands(dispatcher);
		}));

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			// Register config
			WhitelistSyncCore.LoadConfig();
			WhitelistSyncCore.SetupWhitelistSync(new ServerControl(server));
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			WhitelistSyncCore.ShutdownWhitelistSync();
		});

		Log.setLogger(new FabricLogger());
		Log.info(LogMessages.HELLO_MESSAGE);
	}
}