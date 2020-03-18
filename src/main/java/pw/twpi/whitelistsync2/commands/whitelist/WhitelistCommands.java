package pw.twpi.whitelistsync2.commands.whitelist;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import pw.twpi.whitelistsync2.WhitelistSync2;

public class WhitelistCommands {
    public static void register(final CommandDispatcher<CommandSource> dispatcher) {
        // Register wl commands
        LiteralCommandNode<CommandSource> cmdWl = dispatcher.register(
                Commands.literal("wl")
                    .then(CommandList.register(dispatcher))
                    .then(CommandAdd.register(dispatcher))
                    .then(CommandRemove.register(dispatcher))
                    .then(CommandSync.register(dispatcher))
                    .then(CommandCopyToDatabase.register(dispatcher))
        );

        // Allow "whitelistsync2" as an alias
        dispatcher.register(Commands.literal(WhitelistSync2.MODID).redirect(cmdWl));
    }
}
