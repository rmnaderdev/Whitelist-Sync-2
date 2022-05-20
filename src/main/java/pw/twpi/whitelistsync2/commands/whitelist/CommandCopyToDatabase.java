package pw.twpi.whitelistsync2.commands.whitelist;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;
import pw.twpi.whitelistsync2.WhitelistSync2;
import pw.twpi.whitelistsync2.json.WhitelistedPlayersFileUtilities;

public class CommandCopyToDatabase {
    // Name of the command
    private static final String commandName = "copyServerToDatabase";
    private static final int permissionLevel = 4;

    // Errors
    private static final SimpleCommandExceptionType DB_ERROR = new SimpleCommandExceptionType(new StringTextComponent("Error syncing local whitelist to database, please check console for details."));

    // Initial command "checks"
    public static ArgumentBuilder<CommandSource, ?> register(CommandDispatcher<CommandSource> dispatcher) {
        return Commands.literal(commandName)
                .requires(cs -> cs.hasPermission(permissionLevel))
                .executes(context -> {
                    if(WhitelistSync2.whitelistService.copyLocalWhitelistedPlayersToDatabase(WhitelistedPlayersFileUtilities.getWhitelistedPlayers())) {
                        context.getSource().sendSuccess(new StringTextComponent("Pushed local whitelist to database."), false);
                    } else {
                        throw DB_ERROR.create();
                    }

                    return 0;
                });
    }
}
