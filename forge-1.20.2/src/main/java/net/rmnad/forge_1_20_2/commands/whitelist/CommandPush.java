package net.rmnad.forge_1_20_2.commands.whitelist;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.rmnad.forge_1_20_2.WhitelistSync2;
import net.rmnad.json.WhitelistedPlayersFileReader;

public class CommandPush {
    // Name of the command
    private static final String commandName = "push";
    private static final int permissionLevel = 4;

    // Errors
    private static final SimpleCommandExceptionType DB_ERROR = new SimpleCommandExceptionType(Component.literal("Error syncing local whitelist to database, please check console for details."));

    static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal(commandName)
                .requires(cs -> cs.hasPermission(permissionLevel))
                .executes(context -> {
                    if(WhitelistSync2.whitelistService.pushLocalWhitelistToDatabase(WhitelistedPlayersFileReader.getWhitelistedPlayers(WhitelistSync2.SERVER_FILEPATH))) {
                        context.getSource().sendSuccess(() -> Component.literal("Pushed local whitelist to database."), false);
                    } else {
                        throw DB_ERROR.create();
                    }

                    return 0;
                });
    }
}
