package net.rmnad.forge_1_19_2.commands.whitelist;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

public class WhitelistCommands {

    public WhitelistCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                LiteralArgumentBuilder.<CommandSourceStack>literal("wl")
                        .then(CommandList.register())
                        .then(CommandAdd.register())
                        .then(CommandRemove.register())
                        .then(CommandSync.register())
                        .then(CommandCopyToDatabase.register())
        );
    }
}
