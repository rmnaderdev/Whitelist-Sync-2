package net.rmnad.forge_1_21_4;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.rmnad.core.WhitelistSyncCore;
import net.rmnad.core.services.WebService;

public class WhitelistSyncCommands {

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                LiteralArgumentBuilder.<CommandSourceStack>literal("wl")
                        .requires(cs -> cs.hasPermission(3))
                        .then(registerSync())
                        .then(registerPush())
                        .then(registerRestart())
        );
    }

    // Errors
    private static final SimpleCommandExceptionType SYNC_DB_ERROR
            = new SimpleCommandExceptionType(new LiteralMessage("Error syncing whitelist database, please check console for details."));

    private static final SimpleCommandExceptionType PUSH_DB_ERROR
            = new SimpleCommandExceptionType(new LiteralMessage("Error pushing local whitelist to database, please check console for details."));

    // Lambdas for each sync method to reduce code duplication
    private static void syncWhitelist(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (WhitelistSyncCore.whitelistService.pullDatabaseWhitelistToLocal()) {
            context.getSource().sendSuccess(() -> Component.literal("Local whitelist up to date with database."), false);
        } else {
            throw SYNC_DB_ERROR.create();
        }
    }

    private static void syncOps(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (WhitelistSyncCore.whitelistService.pullDatabaseOpsToLocal()) {
            context.getSource().sendSuccess(() -> Component.literal("Local op list up to date with database."), false);
        } else {
            throw SYNC_DB_ERROR.create();
        }
    }

    private static void syncBannedPlayers(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (WhitelistSyncCore.whitelistService.pullDatabaseBannedPlayersToLocal()) {
            context.getSource().sendSuccess(() -> Component.literal("Local banned players up to date with database."), false);
        } else {
            throw SYNC_DB_ERROR.create();
        }
    }

    private static void syncBannedIps(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (WhitelistSyncCore.whitelistService.pullDatabaseBannedIpsToLocal()) {
            context.getSource().sendSuccess(() -> Component.literal("Local banned ips up to date with database."), false);
        } else {
            throw SYNC_DB_ERROR.create();
        }
    }

    // Lambdas for each sync method to reduce code duplication
    private static void pushWhitelist(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (WhitelistSyncCore.whitelistService.pushLocalWhitelistToDatabase()) {
            context.getSource().sendSuccess(() -> Component.literal("Pushed local whitelist to database."), false);
        } else {
            throw PUSH_DB_ERROR.create();
        }
    }

    private static void pushOps(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (WhitelistSyncCore.whitelistService.pushLocalOpsToDatabase()) {
            context.getSource().sendSuccess(() -> Component.literal("Pushed local op list to database."), false);
        } else {
            throw PUSH_DB_ERROR.create();
        }
    }

    private static void pushBannedPlayers(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (WhitelistSyncCore.whitelistService.pushLocalBannedPlayersToDatabase()) {
            context.getSource().sendSuccess(() -> Component.literal("Pushed local banned players to database."), false);
        } else {
            throw PUSH_DB_ERROR.create();
        }
    }

    private static void pushBannedIps(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (WhitelistSyncCore.whitelistService.pushLocalBannedIpsToDatabase()) {
            context.getSource().sendSuccess(() -> Component.literal("Pushed local banned ips to database."), false);
        } else {
            throw PUSH_DB_ERROR.create();
        }
    }


    static ArgumentBuilder<CommandSourceStack, ?> registerSync() {
        var syncBaseCommand = Commands.literal("sync")
            .executes(context -> {
                syncWhitelist(context);

                if (WhitelistSyncCore.CONFIG.syncOpList) {
                    syncOps(context);
                }

                if (WhitelistSyncCore.whitelistService instanceof WebService
                        && WhitelistSyncCore.CONFIG.webSyncBannedPlayers) {
                    syncBannedPlayers(context);
                }

                if (WhitelistSyncCore.whitelistService instanceof WebService
                        && WhitelistSyncCore.CONFIG.webSyncBannedIps) {
                    syncBannedIps(context);
                }

                return 0;
            });

        // Handle just whitelist sync
        syncBaseCommand = syncBaseCommand.then(Commands.literal("whitelist").executes(context -> {
            syncWhitelist(context);
            return 0;
        }));

        // Handle just op list sync
        syncBaseCommand = syncBaseCommand.then(Commands.literal("ops").executes(context -> {
            syncOps(context);
            return 0;
        }));

        // Handle just banned players sync
        syncBaseCommand = syncBaseCommand.then(Commands.literal("banned-players").executes(context -> {
            syncBannedPlayers(context);
            return 0;
        }));

        // Handle just banned ips sync
        syncBaseCommand = syncBaseCommand.then(Commands.literal("banned-ips").executes(context -> {
            syncBannedIps(context);
            return 0;
        }));

        return syncBaseCommand;
    }

    static ArgumentBuilder<CommandSourceStack, ?> registerPush() {
        var pushBaseCommand = Commands.literal("push")
            .executes(context -> {
                pushWhitelist(context);

                if (WhitelistSyncCore.CONFIG.syncOpList) {
                    pushOps(context);
                }

                if (WhitelistSyncCore.whitelistService instanceof WebService
                        && WhitelistSyncCore.CONFIG.webSyncBannedPlayers) {
                    pushBannedPlayers(context);
                }

                if (WhitelistSyncCore.whitelistService instanceof WebService
                        && WhitelistSyncCore.CONFIG.webSyncBannedIps) {
                    pushBannedIps(context);
                }

                return 0;
            });

        // Handle just whitelist push
        pushBaseCommand = pushBaseCommand.then(Commands.literal("whitelist").executes(context -> {
            pushWhitelist(context);
            return 0;
        }));

        // Handle just op list push
        pushBaseCommand = pushBaseCommand.then(Commands.literal("ops").executes(context -> {
            pushOps(context);
            return 0;
        }));

        // Handle just banned players push
        pushBaseCommand = pushBaseCommand.then(Commands.literal("banned-players").executes(context -> {
            pushBannedPlayers(context);
            return 0;
        }));

        // Handle just banned ips push
        pushBaseCommand = pushBaseCommand.then(Commands.literal("banned-ips").executes(context -> {
            pushBannedIps(context);
            return 0;
        }));

        return pushBaseCommand;
    }

    static ArgumentBuilder<CommandSourceStack, ?> registerRestart() {
        return Commands.literal("restart")
                .executes(context -> {
                    WhitelistSyncCore.RestartSyncThread();
                    return 0;
                });
    }
}
