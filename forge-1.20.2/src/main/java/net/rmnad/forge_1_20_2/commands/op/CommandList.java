package net.rmnad.forge_1_20_2.commands.op;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.rmnad.Utilities;
import net.rmnad.forge_1_20_2.WhitelistSync2;

public class CommandList {

    // Name of the command
    private static final String commandName = "list";
    private static final int permissionLevel = 4;

    static ArgumentBuilder<CommandSourceStack, ?> register()
    {
        return Commands.literal(commandName)
                .requires(cs -> cs.hasPermission(permissionLevel))
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal(Utilities.FormatOppedPlayersOutput(WhitelistSync2.whitelistService.getOppedPlayersFromDatabase())), false);
                    return 0;
                });
    }
}
