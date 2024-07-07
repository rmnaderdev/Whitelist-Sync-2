package net.rmnad.forge_1_16_5.commands.op;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;
import net.rmnad.Utilities;
import net.rmnad.forge_1_16_5.WhitelistSync2;

public class CommandList {

    // Name of the command
    private static final String commandName = "list";
    private static final int permissionLevel = 4;

    static ArgumentBuilder<CommandSource, ?> register()
    {
        return Commands.literal(commandName)
                .requires(cs -> cs.hasPermission(permissionLevel))
                .executes(context -> {
                    context.getSource().sendSuccess(new StringTextComponent(Utilities.FormatOppedPlayersOutput(WhitelistSync2.whitelistService.getOppedPlayersFromDatabase())), false);
                    return 0;
                });
    }
}
