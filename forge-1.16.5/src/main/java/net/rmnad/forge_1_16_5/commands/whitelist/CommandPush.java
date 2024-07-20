package net.rmnad.forge_1_16_5.commands.whitelist;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;
import net.rmnad.forge_1_16_5.WhitelistSync2;
import net.rmnad.core.json.WhitelistedPlayersFileReader;

public class CommandPush {
    // Name of the command
    private static final String commandName = "push";
    private static final int permissionLevel = 4;

    // Errors
    private static final SimpleCommandExceptionType DB_ERROR = new SimpleCommandExceptionType(new StringTextComponent("Error syncing local whitelist to database, please check console for details."));

    static ArgumentBuilder<CommandSource, ?> register() {
        return Commands.literal(commandName)
                .requires(cs -> cs.hasPermission(permissionLevel))
                .executes(context -> {
                    if(WhitelistSync2.whitelistService.pushLocalWhitelistToDatabase(WhitelistedPlayersFileReader.getWhitelistedPlayers(WhitelistSync2.SERVER_FILEPATH))) {
                        context.getSource().sendSuccess(new StringTextComponent("Pushed local whitelist to database."), false);
                    } else {
                        throw DB_ERROR.create();
                    }

                    return 0;
                });
    }
}
