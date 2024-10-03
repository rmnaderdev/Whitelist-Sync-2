package net.rmnad.forge_1_19;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class WhitelistSyncCommands {

    public WhitelistSyncCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                LiteralArgumentBuilder.<CommandSourceStack>literal("wl")
                        .requires(cs -> cs.hasPermission(3))
                        .then(registerSync())
                        .then(registerPush())
        );
    }

    // Errors
    private static final SimpleCommandExceptionType SYNC_DB_ERROR
            = new SimpleCommandExceptionType(new LiteralMessage("Error syncing whitelist database, please check console for details."));

    private static final SimpleCommandExceptionType PUSH_DB_ERROR
            = new SimpleCommandExceptionType(new LiteralMessage("Error pushing local whitelist to database, please check console for details."));


    static ArgumentBuilder<CommandSourceStack, ?> registerSync() {
        return Commands.literal("sync")
                .executes(context -> {
                    if (WhitelistSync2.whitelistService.pullDatabaseWhitelistToLocal()) {
                        context.getSource().sendSuccess(Component.literal("Local whitelist up to date with database."), false);
                    } else {
                        throw SYNC_DB_ERROR.create();
                    }

                    if (Config.COMMON.SYNC_OP_LIST.get()) {
                        if (WhitelistSync2.whitelistService.pullDatabaseOpsToLocal()) {
                            context.getSource().sendSuccess(Component.literal("Local op list up to date with database."), false);
                        } else {
                            throw SYNC_DB_ERROR.create();
                        }
                    }

                    if (Config.COMMON.WEB_SYNC_BANNED_PLAYERS.get() && Config.COMMON.DATABASE_MODE.get() == Config.Common.DatabaseMode.WEB) {
                        if (WhitelistSync2.whitelistService.pullDatabaseBannedPlayersToLocal()) {
                            context.getSource().sendSuccess(Component.literal("Local banned players up to date with database."), false);
                        } else {
                            throw SYNC_DB_ERROR.create();
                        }
                    }

                    if (Config.COMMON.WEB_SYNC_BANNED_IPS.get() && Config.COMMON.DATABASE_MODE.get() == Config.Common.DatabaseMode.WEB) {
                        if (WhitelistSync2.whitelistService.pullDatabaseBannedIpsToLocal()) {
                            context.getSource().sendSuccess(Component.literal("Local banned ips up to date with database."), false);
                        } else {
                            throw SYNC_DB_ERROR.create();
                        }
                    }

                    return 0;
                });
    }

    static ArgumentBuilder<CommandSourceStack, ?> registerPush() {
        return Commands.literal("push")
                .executes(context -> {
                    if (WhitelistSync2.whitelistService.pushLocalWhitelistToDatabase()) {
                        context.getSource().sendSuccess(Component.literal("Pushed local whitelist to database."), false);
                    } else {
                        throw PUSH_DB_ERROR.create();
                    }

                    if (Config.COMMON.SYNC_OP_LIST.get()) {
                        if (WhitelistSync2.whitelistService.pushLocalOpsToDatabase()) {
                            context.getSource().sendSuccess(Component.literal("Pushed local op list to database."), false);
                        } else {
                            throw SYNC_DB_ERROR.create();
                        }
                    }

                    if (Config.COMMON.WEB_SYNC_BANNED_PLAYERS.get() && Config.COMMON.DATABASE_MODE.get() == Config.Common.DatabaseMode.WEB) {
                        if (WhitelistSync2.whitelistService.pushLocalBannedPlayersToDatabase()) {
                            context.getSource().sendSuccess(Component.literal("Pushed local banned players to database."), false);
                        } else {
                            throw SYNC_DB_ERROR.create();
                        }
                    }

                    if (Config.COMMON.WEB_SYNC_BANNED_IPS.get() && Config.COMMON.DATABASE_MODE.get() == Config.Common.DatabaseMode.WEB) {
                        if (WhitelistSync2.whitelistService.pushLocalBannedIpsToDatabase()) {
                            context.getSource().sendSuccess(Component.literal("Pushed local banned ips to database."), false);
                        } else {
                            throw SYNC_DB_ERROR.create();
                        }
                    }

                    return 0;
                });
    }
}
