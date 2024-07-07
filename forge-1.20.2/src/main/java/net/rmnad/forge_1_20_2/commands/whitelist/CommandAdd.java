package net.rmnad.forge_1_20_2.commands.whitelist;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserWhiteList;
import net.minecraft.server.players.UserWhiteListEntry;
import net.rmnad.forge_1_20_2.WhitelistSync2;

import java.util.Collection;

public class CommandAdd {
    // Name of the command
    private static final String commandName = "add";
    private static final int permissionLevel = 4;

    // Errors
    private static final DynamicCommandExceptionType DB_ERROR = new DynamicCommandExceptionType(name -> {
        return new LiteralMessage(String.format("Error adding %s to the whitelist database, please check console for details.", name));
    });

    static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal(commandName)
                .requires(cs -> cs.hasPermission(permissionLevel))
                .then(Commands.argument("players", GameProfileArgument.gameProfile())
                    .suggests((context, suggestionsBuilder) -> {
                        // Get server playerlist
                        PlayerList playerlist = context.getSource().getServer().getPlayerList();


                        return SharedSuggestionProvider.suggest(playerlist.getPlayers().stream()
                        // Filter by players in playerlist who are not whitelisted
                        .filter((playerEntity) -> {
                            return !playerlist.getWhiteList().isWhiteListed(playerEntity.getGameProfile());

                        // Map player names from returned filtered collection
                        }).map((playerEntity) -> {
                            return playerEntity.getGameProfile().getName();
                        }), suggestionsBuilder);
                    })
                .executes(context -> {
                    Collection<GameProfile> players = GameProfileArgument.getGameProfiles(context, "players");
                    UserWhiteList whiteList = context.getSource().getServer().getPlayerList().getWhiteList();

                    int i = 0;

                    for(GameProfile gameProfile : players) {

                        String playerName = gameProfile.getName();

                        if(!whiteList.isWhiteListed(gameProfile)) {
                            // Add player to whitelist service
                            if(WhitelistSync2.whitelistService.addWhitelistPlayer(gameProfile.getId(), gameProfile.getName())) {
                                UserWhiteListEntry whitelistentry = new UserWhiteListEntry(gameProfile);
                                whiteList.add(whitelistentry);

                                context.getSource().sendSuccess(() -> Component.literal(String.format("Added %s to whitelist database.", playerName)), true);
                                ++i;
                                // Everything is kosher!
                            } else {
                                // If something happens with the database stuff
                                throw DB_ERROR.create(playerName);
                            }
                        } else {
                            // Player already whitelisted
                            context.getSource().sendSuccess(() -> Component.literal(String.format("%s is already whitelisted.", playerName)), true);
                        }
                    }

                    return i;
                }));
    }
}
