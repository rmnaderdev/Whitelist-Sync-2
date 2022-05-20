package pw.twpi.whitelistsync2.commands.whitelist;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;

public class WhitelistCommands {
    public WhitelistCommands(final CommandDispatcher<CommandSource> dispatcher) {
        // Register wl commands
        LiteralCommandNode<CommandSource> cmdWl = dispatcher.register(
                Commands.literal("wl")
                    .then(CommandList.register(dispatcher))
                    .then(CommandAdd.register(dispatcher))
                    .then(CommandRemove.register(dispatcher))
                    .then(CommandSync.register(dispatcher))
                    .then(CommandCopyToDatabase.register(dispatcher))
        );
    }
}
