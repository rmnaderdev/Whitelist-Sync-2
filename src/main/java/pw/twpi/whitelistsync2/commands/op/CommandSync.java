package pw.twpi.whitelistsync2.commands.op;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.players.UserWhiteListEntry;
import pw.twpi.whitelistsync2.WhitelistSync2;
import pw.twpi.whitelistsync2.json.WhitelistedPlayersFileUtilities;

public class CommandSync {
    // Name of the command
    private static final String commandName = "sync";
    private static final int permissionLevel = 4;

    // Errors
    private static final SimpleCommandExceptionType DB_ERROR
            = new SimpleCommandExceptionType(new TextComponent("Error syncing op database, please check console for details."));

    public static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal(commandName)
                .requires(cs -> cs.hasPermission(permissionLevel))
                .executes(context -> {

                    boolean status = WhitelistSync2.whitelistService.copyDatabaseWhitelistedPlayersToLocal(
                            WhitelistedPlayersFileUtilities.getWhitelistedPlayers(),
                            (uuid, name)->{
                                // Called when user added to whitelist
                                context.getSource().getServer().getPlayerList().getWhiteList().add(new UserWhiteListEntry(new GameProfile(uuid, name)));
                            },
                            (uuid, name) -> {
                                // Called when user removed from whitelist
                                context.getSource().getServer().getPlayerList().getWhiteList().remove(new UserWhiteListEntry(new GameProfile(uuid, name)));
                            }
                    );
                    
                    if(status) {
                        context.getSource().sendSuccess(new TextComponent("Local op list up to date with database."), false);
                    } else {
                        throw DB_ERROR.create();
                    }
                    return 0;
                });
    }
}
