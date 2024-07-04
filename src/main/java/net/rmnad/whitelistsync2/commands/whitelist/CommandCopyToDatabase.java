package net.rmnad.whitelistsync2.commands.whitelist;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.rmnad.whitelistsync2.WhitelistSync2;
import net.rmnad.whitelistsync2.json.WhitelistedPlayersFileUtilities;

public class CommandCopyToDatabase {
    // Name of the command
    private static final String commandName = "copyServerToDatabase";
    private static final int permissionLevel = 4;

    // Errors
    private static final SimpleCommandExceptionType DB_ERROR = new SimpleCommandExceptionType(Component.literal("Error syncing local whitelist to database, please check console for details."));

    static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal(commandName)
                .requires(cs -> cs.hasPermission(permissionLevel))
                .executes(context -> {
                    if(WhitelistSync2.whitelistService.copyLocalWhitelistedPlayersToDatabase(WhitelistedPlayersFileUtilities.getWhitelistedPlayers())) {
                        context.getSource().sendSuccess(Component.literal("Pushed local whitelist to database."), false);
                    } else {
                        throw DB_ERROR.create();
                    }

                    return 0;
                });
    }
}
