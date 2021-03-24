package pw.twpi.whitelistsync2.commands.op;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;
import pw.twpi.whitelistsync2.WhitelistSync2;

public class CommandCopyToDatabase implements Command<CommandSource> {
    // !!!!!!!!!!!!!!Make sure you change this to this class!!!!!!!!!!!!!!
    private static final CommandCopyToDatabase CMD = new CommandCopyToDatabase();

    // Name of the command
    private static final String commandName = "copyServerToDatabase";
    private static final int permissionLevel = 4;

    // Errors
    private static final SimpleCommandExceptionType DB_ERROR
            = new SimpleCommandExceptionType(new StringTextComponent("Error syncing local op list to database, please check console for details."));


    // Initial command "checks"
    public static ArgumentBuilder<CommandSource, ?> register(CommandDispatcher<CommandSource> dispatcher) {
        return Commands.literal(commandName)
                .requires(cs -> cs.hasPermission(permissionLevel))
                .executes(CMD);
    }

    // Command action
    @Override
    public int run(CommandContext<CommandSource> context) throws CommandSyntaxException {
        if(WhitelistSync2.whitelistService.copyLocalOppedPlayersToDatabase()) {
            context.getSource().sendSuccess(new StringTextComponent("Pushed local op list to database."), false);
        } else {
            throw DB_ERROR.create();
        }

        return 0;
    }
}
