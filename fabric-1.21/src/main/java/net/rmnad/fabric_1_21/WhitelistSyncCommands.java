package net.rmnad.fabric_1_21;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.rmnad.services.WebService;

public class WhitelistSyncCommands {

    public WhitelistSyncCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
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
        if (WhitelistSync2.whitelistService.pullDatabaseWhitelistToLocal()) {
            context.getSource().sendFeedback(() -> Text.literal("Local whitelist up to date with database."), false);
        } else {
            throw SYNC_DB_ERROR.create();
        }
    }

    private static void syncOps(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        if (WhitelistSync2.whitelistService.pullDatabaseOpsToLocal()) {
            context.getSource().sendFeedback(() -> Text.literal("Local op list up to date with database."), false);
        } else {
            throw SYNC_DB_ERROR.create();
        }
    }

    private static void syncBannedPlayers(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        if (WhitelistSync2.whitelistService.pullDatabaseBannedPlayersToLocal()) {
            context.getSource().sendFeedback(() -> Text.literal("Local banned players up to date with database."), false);
        } else {
            throw SYNC_DB_ERROR.create();
        }
    }

    private static void syncBannedIps(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        if (WhitelistSync2.whitelistService.pullDatabaseBannedIpsToLocal()) {
            context.getSource().sendFeedback(() -> Text.literal("Local banned ips up to date with database."), false);
        } else {
            throw SYNC_DB_ERROR.create();
        }
    }

    // Lambdas for each sync method to reduce code duplication
    private static void pushWhitelist(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        if (WhitelistSync2.whitelistService.pushLocalWhitelistToDatabase()) {
            context.getSource().sendFeedback(() -> Text.literal("Pushed local whitelist to database."), false);
        } else {
            throw PUSH_DB_ERROR.create();
        }
    }

    private static void pushOps(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        if (WhitelistSync2.whitelistService.pushLocalOpsToDatabase()) {
            context.getSource().sendFeedback(() -> Text.literal("Pushed local op list to database."), false);
        } else {
            throw PUSH_DB_ERROR.create();
        }
    }

    private static void pushBannedPlayers(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        if (WhitelistSync2.whitelistService.pushLocalBannedPlayersToDatabase()) {
            context.getSource().sendFeedback(() -> Text.literal("Pushed local banned players to database."), false);
        } else {
            throw PUSH_DB_ERROR.create();
        }
    }

    private static void pushBannedIps(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        if (WhitelistSync2.whitelistService.pushLocalBannedIpsToDatabase()) {
            context.getSource().sendFeedback(() -> Text.literal("Pushed local banned ips to database."), false);
        } else {
            throw PUSH_DB_ERROR.create();
        }
    }


    static ArgumentBuilder<ServerCommandSource, ?> registerSync() {
        var syncBaseCommand = CommandManager.literal("sync")
            .executes(context -> {
                syncWhitelist(context);

                if (WhitelistSync2.CONFIG.syncOpList()) {
                    syncOps(context);
                }

                if (WhitelistSync2.whitelistService instanceof WebService
                        && WhitelistSync2.CONFIG.webSyncBannedPlayers()) {
                    syncBannedPlayers(context);
                }

                if (WhitelistSync2.whitelistService instanceof WebService
                        && WhitelistSync2.CONFIG.webSyncBannedIps()) {
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
        syncBaseCommand = syncBaseCommand.then(CommandManager.literal("op").executes(context -> {
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

                if (WhitelistSync2.CONFIG.syncOpList()) {
                    pushOps(context);
                }

                if (WhitelistSync2.whitelistService instanceof WebService
                        && WhitelistSync2.CONFIG.webSyncBannedPlayers()) {
                    pushBannedPlayers(context);
                }

                if (WhitelistSync2.whitelistService instanceof WebService
                        && WhitelistSync2.CONFIG.webSyncBannedIps()) {
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
        pushBaseCommand = pushBaseCommand.then(CommandManager.literal("op").executes(context -> {
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
                    WhitelistSync2.RestartSyncThread();
                    return 0;
                });
    }
}
