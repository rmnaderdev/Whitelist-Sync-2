package net.rmnad.fabric_1_21.mixins;

import com.google.common.net.InetAddresses;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.command.argument.MessageArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.rmnad.Log;
import net.rmnad.fabric_1_21.WhitelistSync2;
import net.rmnad.services.BaseService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(CommandManager.class)
public class MinecraftCommandMixin {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    private void onCommandExecute(ParseResults<ServerCommandSource> parseResults, String x, CallbackInfo ci) {
        var context = parseResults.getContext().build(parseResults.getReader().getString());
        String command = context.getInput();
        BaseService whitelistService = WhitelistSync2.whitelistService;

        // Ignore if player does not have permission
        if (!context.getSource().hasPermissionLevel(3))
            return;

        if (command.startsWith("whitelist add")) {
            try {
                Collection<GameProfile> targets = GameProfileArgumentType.getProfileArgument(context, "targets");

                for (GameProfile target : targets) {
                    // Custom logic for handling whitelist add
                    Log.debug("[Intercept] Player " + target.getName() + " is being added to the whitelist.");
                    whitelistService.addWhitelistPlayer(target.getId(), target.getName());
                }

            } catch (Exception e) {
                ci.cancel();
                Log.error("Error handling whitelist add command: " + e.getMessage());
            }
        }
        else if (command.startsWith("whitelist remove")) {
            try {
                Collection<GameProfile> targets = GameProfileArgumentType.getProfileArgument(context, "targets");

                for (GameProfile target : targets) {
                    // Custom logic for handling whitelist remove
                    Log.debug("[Intercept] Player " + target.getName() + " is being removed from the whitelist.");
                    whitelistService.removeWhitelistPlayer(target.getId(), target.getName());
                }

            } catch (Exception e) {
                ci.cancel();
                Log.error("Error handling whitelist add command: " + e.getMessage());
            }
        }
        else if (command.startsWith("op ")) {
//            if (!Config.COMMON.SYNC_OP_LIST.get()) {
//                return;
//            }

            try {
                Collection<GameProfile> targets = GameProfileArgumentType.getProfileArgument(context, "targets");

                for (GameProfile target : targets) {
                    // Custom logic for handling op command
                    Log.debug("[Intercept] Player " + target.getName() + " is being opped.");
                    whitelistService.addOppedPlayer(target.getId(), target.getName());
                }

            } catch (Exception e) {
                ci.cancel();
                Log.error("Error handling op command: " + e.getMessage());
            }
        }
        else if (command.startsWith("deop ")) {
//            if (!Config.COMMON.SYNC_OP_LIST.get()) {
//                return;
//            }

            try {
                Collection<GameProfile> targets = GameProfileArgumentType.getProfileArgument(context, "targets");

                for (GameProfile target : targets) {
                    // Custom logic for handling deop command
                    Log.debug("[Intercept] Player " + target.getName() + " is being deopped.");
                    whitelistService.removeOppedPlayer(target.getId(), target.getName());
                }

            } catch (Exception e) {
                ci.cancel();
                Log.error("Error handling deop command: " + e.getMessage());
            }
        }
        else if (command.startsWith("ban-ip ")) {
//            if (!Config.COMMON.WEB_SYNC_BANNED_IPS.get() || !(whitelistService instanceof WebService)) {
//                return;
//            }

            try {
                String target = StringArgumentType.getString(context, "target");
                String reason = null;
                try {
                    reason = MessageArgumentType.getMessage(context, "reason").getString();
                } catch (IllegalArgumentException e) {
                    // No reason provided
                }

                if (!InetAddresses.isInetAddress(target)) {
                    // Target is player, get player ip address
                    ServerPlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(target);
                    if (player != null) {
                        target = player.getIp();
                    } else {
                        return;
                    }
                }

                Log.debug("[Intercept] IP " + target + " is being banned.");
                whitelistService.addBannedIp(target, reason);
            } catch (Exception e) {
                ci.cancel();
                Log.error("Error handling ban-ip command: " + e.getMessage());
            }
        }
        else if (command.startsWith("pardon-ip ")) {
//            if (!Config.COMMON.WEB_SYNC_BANNED_IPS.get() || !(whitelistService instanceof WebService)) {
//                return;
//            }

            try {
                String target = StringArgumentType.getString(context, "target");

                Log.debug("[Intercept] IP " + target + " is being unbanned.");
                whitelistService.removeBannedIp(target);
            } catch (Exception e) {
                ci.cancel();
                Log.error("Error handling pardon-ip command: " + e.getMessage());
            }
        }
        else if (command.startsWith("ban ")) {
//            if (!Config.COMMON.WEB_SYNC_BANNED_PLAYERS.get() || !(whitelistService instanceof WebService)) {
//                return;
//            }

            try {
                Collection<GameProfile> targets = GameProfileArgumentType.getProfileArgument(context, "targets");
                String reason = null;
                try {
                    reason = MessageArgumentType.getMessage(context, "reason").getString();
                } catch (IllegalArgumentException e) {
                    // No reason provided
                }

                for (GameProfile target : targets) {
                    // Custom logic for handling ban command
                    Log.debug("[Intercept] Player " + target.getName() + " is being banned. Reason: " + (reason != null ? reason : "No reason provided."));
                    whitelistService.addBannedPlayer(target.getId(), target.getName(), reason);
                }

            } catch (Exception e) {
                ci.cancel();
                Log.error("Error handling ban command: " + e.getMessage());
            }
        }
        else if (command.startsWith("pardon ")) {
//            if (!Config.COMMON.WEB_SYNC_BANNED_PLAYERS.get() || !(whitelistService instanceof WebService)) {
//                return;
//            }

            try {
                Collection<GameProfile> targets = GameProfileArgumentType.getProfileArgument(context, "targets");

                for (GameProfile target : targets) {
                    // Custom logic for handling pardon command
                    Log.debug("[Intercept] Player " + target.getName() + " is being unbanned.");
                    whitelistService.removeBannedPlayer(target.getId(), target.getName());
                }

            } catch (Exception e) {
                ci.cancel();
                Log.error("Error handling pardon command: " + e.getMessage());
            }
        }
    }
}
