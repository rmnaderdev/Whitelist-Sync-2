package pw.twpi.whitelistsync2.commands.whitelist;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.GameProfileArgument;
import net.minecraft.server.management.PlayerList;
import net.minecraft.server.management.WhiteList;
import net.minecraft.server.management.WhitelistEntry;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponentUtils;
import net.minecraft.util.text.TranslationTextComponent;
import pw.twpi.whitelistsync2.WhitelistSync2;

import java.util.Collection;

public class CommandRemove implements Command<CommandSource> {
    // !!!!!!!!!!!!!!Make sure you change this to this class!!!!!!!!!!!!!!
    private static final CommandRemove CMD = new CommandRemove();

    // Name of the command
    private static final String commandName = "remove";
    private static final int permissionLevel = 4;

    // Errors
    private static final DynamicCommandExceptionType DB_ERROR = new DynamicCommandExceptionType(name -> {
        return new LiteralMessage(String.format("Error removing %s from the whitelist database, please check console for details.", name));
    });

    // Initial command "checks"
    public static ArgumentBuilder<CommandSource, ?> register(CommandDispatcher<CommandSource> dispatcher) {
        return Commands.literal(commandName)
                .requires(cs -> cs.hasPermissionLevel(permissionLevel))
                .then(Commands.argument("players", new GameProfileArgument().gameProfile())
                    .suggests((context, suggestionsBuilder) -> {
                        return ISuggestionProvider.suggest(context.getSource().getServer().getPlayerList().getWhitelistedPlayerNames(), suggestionsBuilder);
                    })
                    .executes(CMD));
    }

    // Command action
    @Override
    public int run(CommandContext<CommandSource> context) throws CommandSyntaxException {
        Collection<GameProfile> players = GameProfileArgument.getGameProfiles(context, "players");
        WhiteList whiteList = context.getSource().getServer().getPlayerList().getWhitelistedPlayers();

        int i = 0;

        for (GameProfile gameProfile : players) {

            String playerName = TextComponentUtils.getDisplayName(gameProfile).getString();

            if (whiteList.isWhitelisted(gameProfile)) {
                if(WhitelistSync2.whitelistService.removeWhitelistPlayer(gameProfile)) {
                    WhitelistEntry whitelistentry = new WhitelistEntry(gameProfile);
                    whiteList.removeEntry(whitelistentry);
                    context.getSource().sendFeedback(new StringTextComponent(String.format("Removed %s from whitelist database.", playerName)), true);
                    ++i;
                    // Everything is kosher
                } else {
                    // If something happens with the database stuff
                    throw DB_ERROR.create(playerName);
                }
            } else {
                // Player is not whitelisted
                context.getSource().sendFeedback(new StringTextComponent(String.format("%s is not whitelisted.", playerName)), true);
            }
        }

        if (i > 0) {
            context.getSource().getServer().kickPlayersNotWhitelisted(context.getSource());
        }
        return i;
    }
}
