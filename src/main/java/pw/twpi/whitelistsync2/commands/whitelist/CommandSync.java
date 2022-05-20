package pw.twpi.whitelistsync2.commands.whitelist;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.server.management.WhitelistEntry;
import net.minecraft.util.text.StringTextComponent;
import pw.twpi.whitelistsync2.WhitelistSync2;
import pw.twpi.whitelistsync2.json.WhitelistedPlayersFileUtilities;

public class CommandSync {
    // Name of the command
    private static final String commandName = "sync";
    private static final int permissionLevel = 4;

    // Errors
    private static final SimpleCommandExceptionType DB_ERROR = new SimpleCommandExceptionType(new StringTextComponent("Error syncing whitelist database, please check console for details."));

    // Initial command "checks"
    public static ArgumentBuilder<CommandSource, ?> register(CommandDispatcher<CommandSource> dispatcher) {
        return Commands.literal(commandName)
                .requires(cs -> cs.hasPermission(permissionLevel))
                .executes(context -> {
                    boolean status = WhitelistSync2.whitelistService.copyDatabaseWhitelistedPlayersToLocal(
                            WhitelistedPlayersFileUtilities.getWhitelistedPlayers(),
                            (uuid, name)->{
                                // Called when user added to whitelist
                                context.getSource().getServer().getPlayerList().getWhiteList().add(new WhitelistEntry(new GameProfile(uuid, name)));
                            },
                            (uuid, name) -> {
                                // Called when user removed from whitelist
                                context.getSource().getServer().getPlayerList().getWhiteList().remove(new WhitelistEntry(new GameProfile(uuid, name)));
                            }
                    );

                    if(status) {
                        context.getSource().sendSuccess(new StringTextComponent("Local whitelist up to date with database."), false);
                    } else {
                        throw DB_ERROR.create();
                    }

                    return 0;
                });
    }
}
