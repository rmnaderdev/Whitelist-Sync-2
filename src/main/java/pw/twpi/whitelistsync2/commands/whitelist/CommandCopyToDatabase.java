package pw.twpi.whitelistsync2.commands.whitelist;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import pw.twpi.whitelistsync2.WhitelistSync2;

public class CommandCopyToDatabase {
    // Name of the command
    private static final String commandName = "copyServerToDatabase";
    private static final int permissionLevel = 4;

    // Errors
    private static final SimpleCommandExceptionType DB_ERROR = new SimpleCommandExceptionType(new TextComponent("Error syncing local whitelist to database, please check console for details."));

    static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal(commandName)
                .requires(cs -> cs.hasPermission(permissionLevel))
                .executes(context -> {
                    if(WhitelistSync2.whitelistService.copyLocalWhitelistedPlayersToDatabase()) {
                        context.getSource().sendSuccess(new TextComponent("Pushed local whitelist to database."), false);
                    } else {
                        throw DB_ERROR.create();
                    }

                    return 0;
                });
    }
}
