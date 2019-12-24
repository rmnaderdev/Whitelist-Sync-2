package pw.twpi.whitelistsync2;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pw.twpi.whitelistsync2.commands.op.OpCommands;
import pw.twpi.whitelistsync2.commands.whitelist.WhitelistCommands;
import pw.twpi.whitelistsync2.services.BaseService;
import pw.twpi.whitelistsync2.services.MySqlService;
import pw.twpi.whitelistsync2.services.SqLiteService;
import pw.twpi.whitelistsync2.services.SyncThread;

@Mod(WhitelistSync2.MODID)
public class WhitelistSync2
{
    public static final String MODID = "whitelistsync2";
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    public static String SERVER_FILEPATH;

    // Database Service
    public static BaseService whitelistService;

    public WhitelistSync2() {
        // Register config
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SERVER_CONFIG);

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);

        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("Hello from Whitelist Sync 2!");
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        LOGGER.info("Hello again Minecraft");
        LOGGER.info("Setting up databases...");

        if (Config.DATABASE_MODE.get() == Config.DatabaseMode.SQLITE) {
            whitelistService = new SqLiteService();
        } else if (Config.DATABASE_MODE.get() == Config.DatabaseMode.MYSQL) {
            whitelistService = new MySqlService();
        } else {
            throw new RuntimeException("Please check what WHITELIST_MODE is set in the config"
                    + "and make sure it is set to a supported mode.");
        }

        LOGGER.info("Database setup!");
    }

    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        // Load config
        Config.loadConfig(Config.SERVER_CONFIG, FMLPaths.CONFIGDIR.get().resolve("whitelistSync.toml"));


        // Server filepath
        SERVER_FILEPATH = event.getServer().getDataDirectory().getPath();

        LOGGER.info("----------------------------------------------");
        LOGGER.info("---------------WHITELIST SYNC 2---------------");
        LOGGER.info("----------------------------------------------");

        // Check if whitelisting is enabled.
        if (!event.getServer().getPlayerList().isWhiteListEnabled()) {
            LOGGER.info("Oh no! I see whitelisting isn't enabled in the server properties. "
                    + "I assume this is not intentional, I'll enable it for you!");
            event.getServer().getPlayerList().setWhiteListEnabled(true);
        }

        LOGGER.info("Starting Sync Thread...");
        StartSyncThread(event.getServer(), whitelistService);

        LOGGER.info("Registering commands...");
        WhitelistCommands.register(event.getCommandDispatcher());

        if(Config.SYNC_OP_LIST.get()) {
            LOGGER.info("OP Sync is enabled");
            OpCommands.register(event.getCommandDispatcher());
        } else {
            LOGGER.info("OP Sync is disabled");
        }

        LOGGER.info("----------------------------------------------");
        LOGGER.info("----------------------------------------------");
        LOGGER.info("----------------------------------------------");
    }

    public void StartSyncThread(MinecraftServer server, BaseService service) {
        Thread sync = new Thread(new SyncThread(server, service));
        sync.start();
        LOGGER.info("Sync Thread Started!");
    }
}
