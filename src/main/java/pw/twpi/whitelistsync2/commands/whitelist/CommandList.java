package pw.twpi.whitelistsync2.commands.whitelist;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;
import pw.twpi.whitelistsync2.Utilities;
import pw.twpi.whitelistsync2.WhitelistSync2;

public class CommandList {
    // Name of the command
    private static final String commandName = "list";
    private static final int permissionLevel = 4;

    // Initial command "checks"
    public static ArgumentBuilder<CommandSource, ?> register(CommandDispatcher<CommandSource> dispatcher) {
        return Commands.literal(commandName)
                .requires(cs -> cs.hasPermission(permissionLevel))
                .executes(context -> {
                    context.getSource().sendSuccess(new StringTextComponent(Utilities.FormatWhitelistedPlayersOutput(WhitelistSync2.whitelistService.getWhitelistedPlayersFromDatabase())), false);
                    return 0;
                });
    }
}
