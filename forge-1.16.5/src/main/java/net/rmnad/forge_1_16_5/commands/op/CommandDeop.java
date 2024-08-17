package net.rmnad.forge_1_16_5.commands.op;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.GameProfileArgument;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.text.StringTextComponent;
import net.rmnad.forge_1_16_5.WhitelistSync2;
import net.rmnad.logging.CommandMessages;

import java.util.Collection;

public class CommandDeop {

    // Name of the command
    private static final String commandName = "deop";
    private static final int permissionLevel = 4;

    // Errors
    private static final DynamicCommandExceptionType DB_ERROR = new DynamicCommandExceptionType(name -> {
        return new LiteralMessage(String.format("Error removing %s from the op database, please check console for details.", name));
    });

    static ArgumentBuilder<CommandSource, ?> register()
    {
        return Commands.literal(commandName)
                .requires(cs -> cs.hasPermission(permissionLevel))
                .then(Commands.argument("players", GameProfileArgument.gameProfile())
                        .suggests((context, suggestionsBuilder) -> {
                            return ISuggestionProvider.suggest(context.getSource().getServer().getPlayerList().getOpNames(), suggestionsBuilder);
                        })
                        .executes(context -> {
                            Collection<GameProfile> players = GameProfileArgument.getGameProfiles(context, "players");
                            PlayerList playerList = context.getSource().getServer().getPlayerList();

                            int i = 0;

                            for (GameProfile gameProfile : players) {

                                String playerName = gameProfile.getName();

                                if (playerList.isOp(gameProfile)) {
                                    if(WhitelistSync2.whitelistService.removeOppedPlayer(gameProfile.getId(), gameProfile.getName())) {
                                        playerList.deop(gameProfile);
                                        context.getSource().sendSuccess(new StringTextComponent(CommandMessages.RemovedFromOpList(playerName)), true);
                                        ++i;
                                        // Everything is kosher
                                    } else {
                                        // If something happens with the database stuff
                                        throw DB_ERROR.create(playerName);
                                    }
                                } else {
                                    // Player is not whitelisted
                                    context.getSource().sendSuccess(new StringTextComponent(CommandMessages.NotInOpList(playerName)), true);
                                }
                            }

                            if (i > 0) {
                                context.getSource().getServer().kickUnlistedPlayers(context.getSource());
                            }
                            return i;
                        }));
    }
}
