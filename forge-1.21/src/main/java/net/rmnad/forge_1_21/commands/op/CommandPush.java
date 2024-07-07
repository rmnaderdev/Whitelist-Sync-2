package net.rmnad.forge_1_21.commands.op;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.rmnad.forge_1_21.WhitelistSync2;
import net.rmnad.json.OppedPlayersFileReader;

public class CommandPush {

    // Name of the command
    private static final String commandName = "push";
    private static final int permissionLevel = 4;

    // Errors
    private static final SimpleCommandExceptionType DB_ERROR
            = new SimpleCommandExceptionType(new LiteralMessage("Error syncing local op list to database, please check console for details."));

    static ArgumentBuilder<CommandSourceStack, ?> register()
    {
        return Commands.literal(commandName)
                .requires(cs -> cs.hasPermission(permissionLevel))
                .executes(context -> {
                    if(WhitelistSync2.whitelistService.pushLocalOpsToDatabase(OppedPlayersFileReader.getOppedPlayers(WhitelistSync2.SERVER_FILEPATH))) {
                        context.getSource().sendSuccess(() -> Component.literal("Pushed local op list to database."), false);
                    } else {
                        throw DB_ERROR.create();
                    }

                    return 0;
                });
    }
}
