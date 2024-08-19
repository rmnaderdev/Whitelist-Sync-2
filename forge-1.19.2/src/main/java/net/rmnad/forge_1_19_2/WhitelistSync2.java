package net.rmnad.forge_1_19_2;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.rmnad.Log;
import net.rmnad.logging.LogMessages;
import net.rmnad.services.*;
import net.rmnad.forge_1_19_2.commands.op.OpCommands;
import net.rmnad.forge_1_19_2.commands.whitelist.WhitelistCommands;

@Mod(WhitelistSync2.MODID)
public class WhitelistSync2
{
    public static final String MODID = "whitelistsync2";

    // Database Service
    public static BaseService whitelistService;

    public static WhitelistPollingThread pollingThread;
    public static WhitelistSocketThread socketThread;

    public WhitelistSync2() {
        // Register config
        Config.register(ModLoadingContext.get());
        MinecraftForge.EVENT_BUS.register(this);
        Log.setLogger(new ForgeLogger());
        Log.info(LogMessages.HELLO_MESSAGE);
    }

    // Command Registration
    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event){
        new WhitelistCommands(event.getDispatcher());

        if(Config.COMMON.SYNC_OP_LIST.get()) {
            Log.info(LogMessages.OP_SYNC_ENABLED);
            new OpCommands(event.getDispatcher());
        } else {
            Log.info(LogMessages.OP_SYNC_DISABLED);
        }
    }

    // Server Started Event
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        WhitelistSync2.SetupWhitelistSync(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        if(pollingThread != null) {
            pollingThread.interrupt();
        }

        if(socketThread != null) {
            socketThread.interrupt();
        }
    }

    public static void SetupWhitelistSync(MinecraftServer server) {
        Log.verbose = Config.COMMON.VERBOSE_LOGGING.get();

        boolean errorOnSetup = false;

        LogMessages.ShowModStartupHeaderMessage();

        switch (Config.COMMON.DATABASE_MODE.get()) {
            case SQLITE:
                whitelistService = new SqLiteService(Config.COMMON.SQLITE_DATABASE_PATH.get(), Config.COMMON.SYNC_OP_LIST.get());
                break;
            case MYSQL:
                whitelistService = new MySqlService(
                    Config.COMMON.MYSQL_DB_NAME.get(),
                    Config.COMMON.MYSQL_IP.get(),
                    Config.COMMON.MYSQL_PORT.get(),
                    Config.COMMON.MYSQL_USERNAME.get(),
                    Config.COMMON.MYSQL_PASSWORD.get(),
                    Config.COMMON.SYNC_OP_LIST.get()
                );
                break;
            case WEB:
                whitelistService = new WebService(Config.COMMON.WEB_API_HOST.get(), Config.COMMON.WEB_API_KEY.get(), Config.COMMON.SYNC_OP_LIST.get());
                break;
            default:
                Log.error(LogMessages.ERROR_WHITELIST_MODE);
                errorOnSetup = true;
                break;
        }

        if (!errorOnSetup) {
            if (whitelistService.initializeDatabase()) {
                // Database is setup!
                // Check if whitelisting is enabled.
                if (!server.getPlayerList().isUsingWhitelist()) {
                    Log.info(LogMessages.WARN_WHITELIST_NOT_ENABLED);
                    server.getPlayerList().setUsingWhiteList(true);
                }
            } else {
                errorOnSetup = true;
            }
        }

        if (whitelistService instanceof WebService) {
            socketThread = new WhitelistSocketThread(
                    (WebService) whitelistService,
                    errorOnSetup,
                    ((uuid, name) -> {
                        // Called when user added to whitelist
                        server.getPlayerList().getWhiteList().add(new UserWhiteListEntry(new GameProfile(uuid, name)));
                    }), ((uuid, name) -> {
                        // Called when user removed from whitelist
                        server.getPlayerList().getWhiteList().remove(new UserWhiteListEntry(new GameProfile(uuid, name)));
                    }), ((uuid, name) -> {
                        // Called when user added to op list
                        server.getPlayerList().op(new GameProfile(uuid, name));
                    }), ((uuid, name) -> {
                        // Called when user removed from op list
                        server.getPlayerList().deop(new GameProfile(uuid, name));
                    })
            );

            socketThread.start();
        } else {
            pollingThread = new WhitelistPollingThread(
                    whitelistService,
                    Config.COMMON.SYNC_OP_LIST.get(),
                    errorOnSetup,
                    server.getServerDirectory().getPath(),
                    Config.COMMON.SYNC_TIMER.get(),
                    ((uuid, name) -> {
                        // Called when user added to whitelist
                        server.getPlayerList().getWhiteList().add(new UserWhiteListEntry(new GameProfile(uuid, name)));
                    }), ((uuid, name) -> {
                        // Called when user removed from whitelist
                        server.getPlayerList().getWhiteList().remove(new UserWhiteListEntry(new GameProfile(uuid, name)));
                    }), ((uuid, name) -> {
                        // Called when user added to op list
                        server.getPlayerList().op(new GameProfile(uuid, name));
                    }), ((uuid, name) -> {
                        // Called when user removed from op list
                        server.getPlayerList().deop(new GameProfile(uuid, name));
                    })
            );
            pollingThread.start();
        }

        LogMessages.ShowModStartupFooterMessage();
    }
}
