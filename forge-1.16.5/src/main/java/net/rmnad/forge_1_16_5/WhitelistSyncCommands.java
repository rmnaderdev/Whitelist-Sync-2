package net.rmnad.forge_1_16_5;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;

public class WhitelistSyncCommands {

    public WhitelistSyncCommands(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
                LiteralArgumentBuilder.<CommandSource>literal("wl")
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


    static ArgumentBuilder<CommandSource, ?> registerSync() {
        return Commands.literal("sync")
                .executes(context -> {
                    if (WhitelistSync2.whitelistService.pullDatabaseWhitelistToLocal()) {
                        context.getSource().sendSuccess(new StringTextComponent("Local whitelist up to date with database."), false);
                    } else {
                        throw SYNC_DB_ERROR.create();
                    }

                    return 0;
                });
    }

    static ArgumentBuilder<CommandSource, ?> registerPush() {
        return Commands.literal("push")
                .executes(context -> {
                    if (WhitelistSync2.whitelistService.pushLocalWhitelistToDatabase()) {
                        context.getSource().sendSuccess(new StringTextComponent("Pushed local whitelist to database."), false);
                    } else {
                        throw PUSH_DB_ERROR.create();
                    }

                    return 0;
                });
    }
}
