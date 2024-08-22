package net.rmnad.forge_1_18_2.commands.whitelist;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.rmnad.forge_1_18_2.WhitelistSync2;

public class CommandSync {
    // Name of the command
    private static final String commandName = "sync";
    private static final int permissionLevel = 4;

    // Errors
    private static final SimpleCommandExceptionType DB_ERROR = new SimpleCommandExceptionType(new TextComponent("Error syncing whitelist database, please check console for details."));

    static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal(commandName)
                .requires(cs -> cs.hasPermission(permissionLevel))
                .executes(context -> {
                    if(WhitelistSync2.whitelistService.pullDatabaseWhitelistToLocal()) {
                        context.getSource().sendSuccess(new TextComponent("Local whitelist up to date with database."), false);
                    } else {
                        throw DB_ERROR.create();
                    }

                    return 0;
                });
    }

}
