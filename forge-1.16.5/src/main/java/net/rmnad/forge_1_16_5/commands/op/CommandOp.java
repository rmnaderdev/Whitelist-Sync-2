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

import java.util.Collection;

public class CommandOp {

    // Name of the command
    private static final String commandName = "op";
    private static final int permissionLevel = 4;

    // Errors
    private static final DynamicCommandExceptionType DB_ERROR = new DynamicCommandExceptionType(name -> {
        return new LiteralMessage(String.format("Error adding %s to the op database, please check console for details.", name));
    });

    // Initial command "checks"
    public static ArgumentBuilder<CommandSource, ?> register() {
        return Commands.literal(commandName)
                .requires(cs -> cs.hasPermission(permissionLevel))
                .then(Commands.argument("players", GameProfileArgument.gameProfile())
                        .suggests((context, suggestionsBuilder) -> {
                            // Get server playerlist
                            PlayerList playerlist = context.getSource().getServer().getPlayerList();


                            return ISuggestionProvider.suggest(playerlist.getPlayers().stream()
                                    // Filter by players in playerlist who are not opped
                                    .filter((playerEntity) -> {
                                        return !playerlist.isOp(playerEntity.getGameProfile());

                                        // Map player names from returned filtered collection
                                    }).map((playerEntity) -> {
                                        return playerEntity.getGameProfile().getName();
                                    }), suggestionsBuilder);
                        })
                        .executes(context -> {
                            Collection<GameProfile> players = GameProfileArgument.getGameProfiles(context, "players");
                            PlayerList playerList = context.getSource().getServer().getPlayerList();
                            int i = 0;

                            for(GameProfile gameProfile : players) {

                                String playerName = gameProfile.getName();

                                if(!playerList.isOp(gameProfile)) {
                                    // Add player to whitelist service
                                    if(WhitelistSync2.whitelistService.addOppedPlayer(gameProfile.getId(), gameProfile.getName())) {
                                        playerList.op(gameProfile);

                                        context.getSource().sendSuccess(new StringTextComponent(String.format("Opped %s in database.", playerName)), true);
                                        ++i;
                                        // Everything is kosher!
                                    } else {
                                        // If something happens with the database stuff
                                        throw DB_ERROR.create(playerName);
                                    }
                                } else {
                                    // Player already whitelisted
                                    context.getSource().sendSuccess(new StringTextComponent(String.format("%s is already opped.", playerName)), true);
                                }
                            }

                            return i;
                        }));
    }
}
