package net.rmnad.forge_1_18_2.commands.op;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.rmnad.forge_1_18_2.WhitelistSync2;
import net.rmnad.core.json.OppedPlayersFileReader;

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

                    boolean status = WhitelistSync2.whitelistService.pullDatabaseOpsToLocal(
                            OppedPlayersFileReader.getOppedPlayers(WhitelistSync2.SERVER_FILEPATH),
                            (uuid, name)->{
                                // Called when user added to op list
                                // TODO: Add level and bypassesPlayerLimit
                                context.getSource().getServer().getPlayerList().op(new GameProfile(uuid, name));
                            },
                            (uuid, name) -> {
                                // Called when user removed from op list
                                context.getSource().getServer().getPlayerList().deop(new GameProfile(uuid, name));
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
