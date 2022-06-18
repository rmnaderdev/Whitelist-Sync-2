package pw.twpi.whitelistsync2.commands.op;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import pw.twpi.whitelistsync2.WhitelistSync2;
import pw.twpi.whitelistsync2.json.OppedPlayersFileUtilities;

public class CommandSync {
    // Name of the command
    private static final String commandName = "sync";
    private static final int permissionLevel = 4;

    // Errors
    private static final SimpleCommandExceptionType DB_ERROR
            = new SimpleCommandExceptionType(Component.literal("Error syncing op database, please check console for details."));

    public static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal(commandName)
                .requires(cs -> cs.hasPermission(permissionLevel))
                .executes(context -> {

                    boolean status = WhitelistSync2.whitelistService.copyDatabaseOppedPlayersToLocal(
                            OppedPlayersFileUtilities.getOppedPlayers(),
                            (uuid, name)->{
                                // Called when user added to op list
                                context.getSource().getServer().getPlayerList().op(new GameProfile(uuid, name));
                            },
                            (uuid, name) -> {
                                // Called when user removed from op list
                                context.getSource().getServer().getPlayerList().deop(new GameProfile(uuid, name));
                            }
                    );
                    
                    if(status) {
                        context.getSource().sendSuccess(Component.literal("Local op list up to date with database."), false);
                    } else {
                        throw DB_ERROR.create();
                    }
                    return 0;
                });
    }
}
