package net.rmnad.fabric_1_21_1;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.rmnad.core.WhitelistSyncCore;
import net.rmnad.core.services.WebService;

public class WhitelistSyncCommands {

    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("wl")
                        .requires(source -> source.hasPermissionLevel(3))
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
    private static void syncWhitelist(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        if (WhitelistSyncCore.whitelistService.pullDatabaseWhitelistToLocal()) {
            context.getSource().sendFeedback(() -> Text.literal("Local whitelist up to date with database."), false);
        } else {
            throw SYNC_DB_ERROR.create();
        }
    }

    private static void syncOps(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        if (WhitelistSyncCore.whitelistService.pullDatabaseOpsToLocal()) {
            context.getSource().sendFeedback(() -> Text.literal("Local op list up to date with database."), false);
        } else {
            throw SYNC_DB_ERROR.create();
        }
    }

    private static void syncBannedPlayers(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        if (WhitelistSyncCore.whitelistService.pullDatabaseBannedPlayersToLocal()) {
            context.getSource().sendFeedback(() -> Text.literal("Local banned players up to date with database."), false);
        } else {
            throw SYNC_DB_ERROR.create();
        }
    }

    private static void syncBannedIps(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        if (WhitelistSyncCore.whitelistService.pullDatabaseBannedIpsToLocal()) {
            context.getSource().sendFeedback(() -> Text.literal("Local banned ips up to date with database."), false);
        } else {
            throw SYNC_DB_ERROR.create();
        }
    }

    // Lambdas for each sync method to reduce code duplication
    private static void pushWhitelist(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        if (WhitelistSyncCore.whitelistService.pushLocalWhitelistToDatabase()) {
            context.getSource().sendFeedback(() -> Text.literal("Pushed local whitelist to database."), false);
        } else {
            throw PUSH_DB_ERROR.create();
        }
    }

    private static void pushOps(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        if (WhitelistSyncCore.whitelistService.pushLocalOpsToDatabase()) {
            context.getSource().sendFeedback(() -> Text.literal("Pushed local op list to database."), false);
        } else {
            throw PUSH_DB_ERROR.create();
        }
    }

    private static void pushBannedPlayers(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        if (WhitelistSyncCore.whitelistService.pushLocalBannedPlayersToDatabase()) {
            context.getSource().sendFeedback(() -> Text.literal("Pushed local banned players to database."), false);
        } else {
            throw PUSH_DB_ERROR.create();
        }
    }

    private static void pushBannedIps(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        if (WhitelistSyncCore.whitelistService.pushLocalBannedIpsToDatabase()) {
            context.getSource().sendFeedback(() -> Text.literal("Pushed local banned ips to database."), false);
        } else {
            throw PUSH_DB_ERROR.create();
        }
    }


    static ArgumentBuilder<ServerCommandSource, ?> registerSync() {
        var syncBaseCommand = CommandManager.literal("sync")
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
        syncBaseCommand = syncBaseCommand.then(CommandManager.literal("whitelist").executes(context -> {
            syncWhitelist(context);
            return 0;
        }));

        // Handle just op list sync
        syncBaseCommand = syncBaseCommand.then(CommandManager.literal("ops").executes(context -> {
            syncOps(context);
            return 0;
        }));

        // Handle just banned players sync
        syncBaseCommand = syncBaseCommand.then(CommandManager.literal("banned-players").executes(context -> {
            syncBannedPlayers(context);
            return 0;
        }));

        // Handle just banned ips sync
        syncBaseCommand = syncBaseCommand.then(CommandManager.literal("banned-ips").executes(context -> {
            syncBannedIps(context);
            return 0;
        }));

        return syncBaseCommand;
    }

    static ArgumentBuilder<ServerCommandSource, ?> registerPush() {
        var pushBaseCommand = CommandManager.literal("push")
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
        pushBaseCommand = pushBaseCommand.then(CommandManager.literal("whitelist").executes(context -> {
            pushWhitelist(context);
            return 0;
        }));

        // Handle just op list push
        pushBaseCommand = pushBaseCommand.then(CommandManager.literal("ops").executes(context -> {
            pushOps(context);
            return 0;
        }));

        // Handle just banned players push
        pushBaseCommand = pushBaseCommand.then(CommandManager.literal("banned-players").executes(context -> {
            pushBannedPlayers(context);
            return 0;
        }));

        // Handle just banned ips push
        pushBaseCommand = pushBaseCommand.then(CommandManager.literal("banned-ips").executes(context -> {
            pushBannedIps(context);
            return 0;
        }));

        return pushBaseCommand;
    }

    static ArgumentBuilder<ServerCommandSource, ?> registerRestart() {
        return CommandManager.literal("restart")
                .executes(context -> {
                    WhitelistSyncCore.RestartSyncThread();
                    return 0;
                });
    }
}
