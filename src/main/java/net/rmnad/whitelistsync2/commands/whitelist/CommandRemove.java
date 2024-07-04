package net.rmnad.whitelistsync2.commands.whitelist;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.UserWhiteList;
import net.minecraft.server.players.UserWhiteListEntry;
import net.rmnad.whitelistsync2.WhitelistSync2;

import java.util.Collection;

public class CommandRemove {
    // Name of the command
    private static final String commandName = "remove";
    private static final int permissionLevel = 4;

    // Errors
    private static final DynamicCommandExceptionType DB_ERROR = new DynamicCommandExceptionType(name -> {
        return new LiteralMessage(String.format("Error removing %s from the whitelist database, please check console for details.", name));
    });

    static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal(commandName)
                .requires(cs -> cs.hasPermission(permissionLevel))
                .then(Commands.argument("players", GameProfileArgument.gameProfile())
                    .suggests((context, suggestionsBuilder) -> {
                        return SharedSuggestionProvider.suggest(context.getSource().getServer().getPlayerList().getWhiteList().getUserList(), suggestionsBuilder);
                    })
                    .executes(context -> {
                        Collection<GameProfile> players = GameProfileArgument.getGameProfiles(context, "players");
                        UserWhiteList whiteList = context.getSource().getServer().getPlayerList().getWhiteList();

                        int i = 0;

                        for (GameProfile gameProfile : players) {

                            String playerName = gameProfile.getName();

                            if (whiteList.isWhiteListed(gameProfile)) {
                                if(WhitelistSync2.whitelistService.removeWhitelistPlayer(gameProfile.getId(), gameProfile.getName())) {
                                    UserWhiteListEntry whitelistentry = new UserWhiteListEntry(gameProfile);
                                    whiteList.remove(whitelistentry);
                                    context.getSource().sendSuccess(Component.literal(String.format("Removed %s from whitelist database.", playerName)), true);
                                    ++i;
                                    // Everything is kosher
                                } else {
                                    // If something happens with the database stuff
                                    throw DB_ERROR.create(playerName);
                                }
                            } else {
                                // Player is not whitelisted
                                context.getSource().sendSuccess(Component.literal(String.format("%s is not whitelisted.", playerName)), true);
                            }
                        }

                        if (i > 0) {
                            context.getSource().getServer().kickUnlistedPlayers(context.getSource());
                        }
                        return i;
                    }));
    }

}
