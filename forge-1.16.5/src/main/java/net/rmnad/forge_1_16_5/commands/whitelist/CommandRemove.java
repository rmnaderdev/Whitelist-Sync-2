package net.rmnad.forge_1_16_5.commands.whitelist;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.GameProfileArgument;
import net.minecraft.server.management.WhiteList;
import net.minecraft.server.management.WhitelistEntry;
import net.minecraft.util.text.StringTextComponent;
import net.rmnad.forge_1_16_5.WhitelistSync2;
import net.rmnad.logging.CommandMessages;

import java.util.Collection;

public class CommandRemove {
    // Name of the command
    private static final String commandName = "remove";
    private static final int permissionLevel = 4;

    // Errors
    private static final DynamicCommandExceptionType DB_ERROR = new DynamicCommandExceptionType(name -> {
        return new LiteralMessage(String.format("Error removing %s from the whitelist database, please check console for details.", name));
    });

    static ArgumentBuilder<CommandSource, ?> register() {
        return Commands.literal(commandName)
                .requires(cs -> cs.hasPermission(permissionLevel))
                .then(Commands.argument("players", GameProfileArgument.gameProfile())
                    .suggests((context, suggestionsBuilder) -> {
                        return ISuggestionProvider.suggest(context.getSource().getServer().getPlayerList().getWhiteList().getUserList(), suggestionsBuilder);
                    })
                    .executes(context -> {
                        Collection<GameProfile> players = GameProfileArgument.getGameProfiles(context, "players");
                        WhiteList whiteList = context.getSource().getServer().getPlayerList().getWhiteList();

                        int i = 0;

                        for (GameProfile gameProfile : players) {

                            String playerName = gameProfile.getName();

                            if (whiteList.isWhiteListed(gameProfile)) {
                                if(WhitelistSync2.whitelistService.removeWhitelistPlayer(gameProfile.getId(), gameProfile.getName())) {
                                    WhitelistEntry whitelistentry = new WhitelistEntry(gameProfile);
                                    whiteList.remove(whitelistentry);
                                    context.getSource().sendSuccess(new StringTextComponent(CommandMessages.RemovedFromWhitelist(playerName)), true);
                                    ++i;
                                    // Everything is kosher
                                } else {
                                    // If something happens with the database stuff
                                    throw DB_ERROR.create(playerName);
                                }
                            } else {
                                // Player is not whitelisted
                                context.getSource().sendSuccess(new StringTextComponent(CommandMessages.NotWhitelisted(playerName)), true);
                            }
                        }

                        if (i > 0) {
                            context.getSource().getServer().kickUnlistedPlayers(context.getSource());
                        }
                        return i;
                    }));
    }

}
