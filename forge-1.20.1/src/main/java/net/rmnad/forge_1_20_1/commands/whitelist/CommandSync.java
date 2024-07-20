package net.rmnad.forge_1_20_1.commands.whitelist;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.UserWhiteListEntry;
import net.rmnad.forge_1_20_1.WhitelistSync2;
import net.rmnad.core.json.WhitelistedPlayersFileReader;

public class CommandSync {
    // Name of the command
    private static final String commandName = "sync";
    private static final int permissionLevel = 4;

    // Errors
    private static final SimpleCommandExceptionType DB_ERROR = new SimpleCommandExceptionType(Component.literal("Error syncing whitelist database, please check console for details."));

    static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal(commandName)
                .requires(cs -> cs.hasPermission(permissionLevel))
                .executes(context -> {

                    boolean status = WhitelistSync2.whitelistService.pullDatabaseWhitelistToLocal(
                            WhitelistedPlayersFileReader.getWhitelistedPlayers(WhitelistSync2.SERVER_FILEPATH),
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
                        context.getSource().sendSuccess(() -> Component.literal("Local whitelist up to date with database."), false);
                    } else {
                        throw DB_ERROR.create();
                    }

                    return 0;
                });
    }

}
