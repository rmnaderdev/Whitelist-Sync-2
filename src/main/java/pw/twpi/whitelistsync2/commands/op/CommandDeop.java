package pw.twpi.whitelistsync2.commands.op;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.players.PlayerList;
import pw.twpi.whitelistsync2.WhitelistSync2;

import java.util.Collection;

public class CommandDeop {

    // Name of the command
    private static final String commandName = "deop";
    private static final int permissionLevel = 4;

    // Errors
    private static final DynamicCommandExceptionType DB_ERROR = new DynamicCommandExceptionType(name -> {
        return new LiteralMessage(String.format("Error removing %s from the op database, please check console for details.", name));
    });

    static ArgumentBuilder<CommandSourceStack, ?> register()
    {
        return Commands.literal(commandName)
                .requires(cs -> cs.hasPermission(permissionLevel))
                .then(Commands.argument("players", GameProfileArgument.gameProfile())
                        .suggests((context, suggestionsBuilder) -> {
                            return SharedSuggestionProvider.suggest(context.getSource().getServer().getPlayerList().getOpNames(), suggestionsBuilder);
                        })
                        .executes(context -> {
                            Collection<GameProfile> players = GameProfileArgument.getGameProfiles(context, "players");
                            PlayerList playerList = context.getSource().getServer().getPlayerList();

                            int i = 0;

                            for (GameProfile gameProfile : players) {

                                String playerName = gameProfile.getName();

                                if (playerList.isOp(gameProfile)) {
                                    if(WhitelistSync2.whitelistService.removeOppedPlayer(gameProfile)) {
                                        playerList.deop(gameProfile);
                                        context.getSource().sendSuccess(new TextComponent(String.format("Deopped %s from database.", playerName)), true);
                                        ++i;
                                        // Everything is kosher
                                    } else {
                                        // If something happens with the database stuff
                                        throw DB_ERROR.create(playerName);
                                    }
                                } else {
                                    // Player is not whitelisted
                                    context.getSource().sendSuccess(new TextComponent(String.format("%s is not opped.", playerName)), true);
                                }
                            }

                            if (i > 0) {
                                context.getSource().getServer().kickUnlistedPlayers(context.getSource());
                            }
                            return i;
                        }));
    }
}
