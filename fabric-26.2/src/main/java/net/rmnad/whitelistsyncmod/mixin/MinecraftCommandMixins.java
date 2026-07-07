package net.rmnad.whitelistsyncmod.mixin;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.*;
import net.minecraft.server.players.NameAndId;
import net.rmnad.core.Log;
import net.rmnad.core.WhitelistSyncCore;
import net.rmnad.core.services.WebService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

import static net.rmnad.core.WhitelistSyncCore.whitelistService;

public class MinecraftCommandMixins {

    @Mixin(WhitelistCommand.class)
    public abstract static class MinecraftWhitelistCommandMixin {

        @Inject(method = "addPlayers", at = @At("HEAD"), cancellable = true)
        private static void injectExecuteAdd(CommandSourceStack source, Collection<NameAndId> targets, CallbackInfoReturnable<Integer> cir) {
            try {
                for (var target : targets) {
                    // Custom logic for handling whitelist add
                    Log.debug("[Intercept] Player " + target.name() + " is being added to the whitelist.");
                    whitelistService.addWhitelistPlayer(target.id(), target.name());
                }

            } catch (Exception e) {
                cir.setReturnValue(0);
                Log.error("Error handling whitelist add command: " + e.getMessage());
            }
        }

        @Inject(method = "removePlayers", at = @At("HEAD"), cancellable = true)
        private static void injectExecuteRemove(CommandSourceStack source, Collection<NameAndId> targets, CallbackInfoReturnable<Integer> cir) {
            try {
                for (var target : targets) {
                    // Custom logic for handling whitelist remove
                    Log.debug("[Intercept] Player " + target.name() + " is being removed from the whitelist.");
                    whitelistService.removeWhitelistPlayer(target.id(), target.name());
                }

            } catch (Exception e) {
                cir.setReturnValue(0);
                Log.error("Error handling whitelist add command: " + e.getMessage());
            }
        }

    }

    @Mixin(OpCommand.class)
    public abstract static class MinecraftOpCommandMixin {

        @Inject(method = "opPlayers", at = @At("HEAD"), cancellable = true)
        private static void injectOp(CommandSourceStack source, Collection<NameAndId> targets, CallbackInfoReturnable<Integer> cir) {
            if (!WhitelistSyncCore.CONFIG.syncOpList) {
                return;
            }

            try {
                for (var target : targets) {
                    // Custom logic for handling op command
                    Log.debug("[Intercept] Player " + target.name() + " is being opped.");
                    whitelistService.addOppedPlayer(target.id(), target.name());
                }

            } catch (Exception e) {
                cir.setReturnValue(0);
                Log.error("Error handling op command: " + e.getMessage());
            }
        }

    }

    @Mixin(DeOpCommands.class)
    public abstract static class MinecraftDeOpCommandMixin {

        @Inject(method = "deopPlayers", at = @At("HEAD"), cancellable = true)
        private static void injectDeop(CommandSourceStack source, Collection<NameAndId> players, CallbackInfoReturnable<Integer> cir) {
            if (!WhitelistSyncCore.CONFIG.syncOpList) {
                return;
            }

            try {
                for (var player : players) {
                    // Custom logic for handling deop command
                    Log.debug("[Intercept] Player " + player.name() + " is being deopped.");
                    whitelistService.removeOppedPlayer(player.id(), player.name());
                }

            } catch (Exception e) {
                cir.setReturnValue(0);
                Log.error("Error handling deop command: " + e.getMessage());
            }
        }

    }

    @Mixin(BanPlayerCommands.class)
    public abstract static class MinecraftBanCommandMixin {

        @Inject(method = "banPlayers", at = @At("HEAD"), cancellable = true)
        private static void injectBan(CommandSourceStack source, Collection<NameAndId> players, Component reason, CallbackInfoReturnable<Integer> cir) {
            if (!WhitelistSyncCore.CONFIG.webSyncBannedPlayers || !(whitelistService instanceof WebService)) {
                return;
            }

            try {
                for (var player : players) {
                    // Custom logic for handling ban command
                    Log.debug("[Intercept] Player " + player.name() + " is being banned. Reason: " + (reason != null ? reason : "No reason provided."));
                    whitelistService.addBannedPlayer(player.id(), player.name(), reason != null ? reason.getString() : null);
                }

            } catch (Exception e) {
                cir.setReturnValue(0);
                Log.error("Error handling ban command: " + e.getMessage());
            }
        }

    }

    @Mixin(PardonCommand.class)
    public abstract static class MinecraftPardonCommandMixin {

        @Inject(method = "pardonPlayers", at = @At("HEAD"), cancellable = true)
        private static void injectPardon(CommandSourceStack source, Collection<NameAndId> players, CallbackInfoReturnable<Integer> cir) {
            if (!WhitelistSyncCore.CONFIG.webSyncBannedPlayers || !(whitelistService instanceof WebService)) {
                return;
            }

            try {
                for (var player : players) {
                    // Custom logic for handling pardon command
                    Log.debug("[Intercept] Player " + player.name() + " is being unbanned.");
                    whitelistService.removeBannedPlayer(player.id(), player.name());
                }

            } catch (Exception e) {
                cir.setReturnValue(0);
                Log.error("Error handling pardon command: " + e.getMessage());
            }
        }

    }

    @Mixin(BanIpCommands.class)
    public abstract static class MinecraftBanIpCommandMixin {

        @Inject(method = "banIp", at = @At("HEAD"), cancellable = true)
        private static void injectBanIp(CommandSourceStack source, String ip, Component reason, CallbackInfoReturnable<Integer> cir) {
            if (!WhitelistSyncCore.CONFIG.webSyncBannedIps || !(whitelistService instanceof WebService)) {
                return;
            }

            try {
                Log.debug("[Intercept] IP " + ip + " is being banned.");
                whitelistService.addBannedIp(ip, reason != null ? reason.getString() : null);
            } catch (Exception e) {
                cir.setReturnValue(0);
                Log.error("Error handling ban-ip command: " + e.getMessage());
            }
        }

    }

    @Mixin(PardonIpCommand.class)
    public abstract static class MinecraftPardonIpCommandMixin {

        @Inject(method = "unban", at = @At("HEAD"), cancellable = true)
        private static void injectPardonIp(CommandSourceStack source, String ip, CallbackInfoReturnable<Integer> cir) {
            if (!WhitelistSyncCore.CONFIG.webSyncBannedIps || !(whitelistService instanceof WebService)) {
                return;
            }

            try {
                Log.debug("[Intercept] IP " + ip + " is being unbanned.");
                whitelistService.removeBannedIp(ip);
            } catch (Exception e) {
                cir.setReturnValue(0);
                Log.error("Error handling pardon-ip command: " + e.getMessage());
            }
        }

    }
}
