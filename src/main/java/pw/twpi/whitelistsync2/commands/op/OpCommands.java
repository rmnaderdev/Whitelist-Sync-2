package pw.twpi.whitelistsync2.commands.op;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import pw.twpi.whitelistsync2.WhitelistSync2;

public class OpCommands {
    public static void register(final CommandDispatcher<CommandSource> dispatcher) {
        // Register wl commands
        LiteralCommandNode<CommandSource> cmdWl = dispatcher.register(
                Commands.literal("wlop")
                    .then(CommandList.register(dispatcher))
                    .then(CommandOp.register(dispatcher))
                    .then(CommandDeop.register(dispatcher))
                    .then(CommandSync.register(dispatcher))
                    .then(CommandCopyToDatabase.register(dispatcher))
        );

    }
}
