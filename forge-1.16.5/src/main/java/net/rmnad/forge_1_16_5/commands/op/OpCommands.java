package net.rmnad.forge_1_16_5.commands.op;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;

public class OpCommands {
    public OpCommands(CommandDispatcher<CommandSource> dispatcher)
    {
        dispatcher.register(
                LiteralArgumentBuilder.<CommandSource>literal("wlop")
                        .then(CommandList.register())
                        .then(CommandOp.register())
                        .then(CommandDeop.register())
                        .then(CommandSync.register())
                        .then(CommandPush.register())
        );
    }
}
