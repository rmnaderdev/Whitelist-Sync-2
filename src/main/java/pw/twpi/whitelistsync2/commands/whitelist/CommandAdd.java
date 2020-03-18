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

public class CommandAdd implements Command<CommandSource> {
    // !!!!!!!!!!!!!!Make sure you change this to this class!!!!!!!!!!!!!!
    private static final CommandAdd CMD = new CommandAdd();

    // Name of the command
    private static final String commandName = "add";
    private static final int permissionLevel = 4;

    // Errors
    private static final DynamicCommandExceptionType DB_ERROR = new DynamicCommandExceptionType(name -> {
        return new LiteralMessage(String.format("Error adding %s to the whitelist database, please check console for details.", name));
    });

    // Initial command "checks"
    public static ArgumentBuilder<CommandSource, ?> register(CommandDispatcher<CommandSource> dispatcher) {
        return Commands.literal(commandName)
                .requires(cs -> cs.hasPermissionLevel(permissionLevel))
                .then(Commands.argument("players", new GameProfileArgument().gameProfile())
                    .suggests((context, suggestionsBuilder) -> {
                        // Get server playerlist
                        PlayerList playerlist = context.getSource().getServer().getPlayerList();


                        return ISuggestionProvider.suggest(playerlist.getPlayers().stream()
                        // Filter by players in playerlist who are not whitelisted
                        .filter((playerEntity) -> {
                            return !playerlist.getWhitelistedPlayers().isWhitelisted(playerEntity.getGameProfile());

                        // Map player names from returned filtered collection
                        }).map((playerEntity) -> {
                            return playerEntity.getGameProfile().getName();
                        }), suggestionsBuilder);
                    })
                .executes(CMD));
    }

    // Command action
    @Override
    public int run(CommandContext<CommandSource> context) throws CommandSyntaxException {
        Collection<GameProfile> players = GameProfileArgument.getGameProfiles(context, "players");
        WhiteList whiteList = context.getSource().getServer().getPlayerList().getWhitelistedPlayers();

        int i = 0;

        for(GameProfile gameProfile : players) {

            String playerName = TextComponentUtils.getDisplayName(gameProfile).getString();

            if(!whiteList.isWhitelisted(gameProfile)) {
                // Add player to whitelist service
                if(WhitelistSync2.whitelistService.addPlayerToDatabaseWhitelist(gameProfile)) {
                    WhitelistEntry whitelistentry = new WhitelistEntry(gameProfile);
                    whiteList.addEntry(whitelistentry);

                    context.getSource().sendFeedback(new StringTextComponent(String.format("Added %s to whitelist database.", playerName)), true);
                    ++i;
                    // Everything is kosher!
                } else {
                    // If something happens with the database stuff
                    throw DB_ERROR.create(playerName);
                }
            } else {
                // Player already whitelisted
                context.getSource().sendFeedback(new StringTextComponent(String.format("%s is already whitelisted.", playerName)), true);
            }
        }

        return i;
    }
}
