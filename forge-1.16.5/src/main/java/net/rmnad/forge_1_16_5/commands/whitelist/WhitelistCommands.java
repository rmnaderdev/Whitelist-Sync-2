package net.rmnad.forge_1_16_5.commands.whitelist;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;

public class WhitelistCommands {

    public WhitelistCommands(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
                LiteralArgumentBuilder.<CommandSource>literal("wl")
                        .then(CommandList.register())
                        .then(CommandAdd.register())
                        .then(CommandRemove.register())
                        .then(CommandSync.register())
                        .then(CommandPush.register())
        );
    }
}
