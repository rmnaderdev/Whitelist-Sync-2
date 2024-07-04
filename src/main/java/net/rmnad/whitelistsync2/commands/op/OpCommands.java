package net.rmnad.whitelistsync2.commands.op;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

public class OpCommands {
    public OpCommands(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(
                LiteralArgumentBuilder.<CommandSourceStack>literal("wlop")
                        .then(CommandList.register())
                        .then(CommandOp.register())
                        .then(CommandDeop.register())
                        .then(CommandSync.register())
                        .then(CommandCopyToDatabase.register())
        );
    }
}
