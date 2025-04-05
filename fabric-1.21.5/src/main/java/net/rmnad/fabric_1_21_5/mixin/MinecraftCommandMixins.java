package net.rmnad.fabric_1_21_5.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.command.*;
import net.minecraft.text.Text;
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

        @Inject(method = "executeAdd", at = @At("HEAD"), cancellable = true)
        private static void injectExecuteAdd(ServerCommandSource source, Collection<GameProfile> targets, CallbackInfoReturnable<Integer> cir) {
            try {
                for (GameProfile target : targets) {
                    // Custom logic for handling whitelist add
                    Log.debug("[Intercept] Player " + target.getName() + " is being added to the whitelist.");
                    whitelistService.addWhitelistPlayer(target.getId(), target.getName());
                }

            } catch (Exception e) {
                cir.setReturnValue(0);
                Log.error("Error handling whitelist add command: " + e.getMessage());
            }
        }

        @Inject(method = "executeRemove", at = @At("HEAD"), cancellable = true)
        private static void injectExecuteRemove(ServerCommandSource source, Collection<GameProfile> targets, CallbackInfoReturnable<Integer> cir) {
            try {
                for (GameProfile target : targets) {
                    // Custom logic for handling whitelist remove
                    Log.debug("[Intercept] Player " + target.getName() + " is being removed from the whitelist.");
                    whitelistService.removeWhitelistPlayer(target.getId(), target.getName());
                }

            } catch (Exception e) {
                cir.setReturnValue(0);
                Log.error("Error handling whitelist add command: " + e.getMessage());
            }
        }

    }

    @Mixin(OpCommand.class)
    public abstract static class MinecraftOpCommandMixin {

        @Inject(method = "op", at = @At("HEAD"), cancellable = true)
        private static void injectOp(ServerCommandSource source, Collection<GameProfile> targets, CallbackInfoReturnable<Integer> cir) {
            if (!WhitelistSyncCore.CONFIG.syncOpList) {
                return;
            }

            try {
                for (GameProfile target : targets) {
                    // Custom logic for handling op command
                    Log.debug("[Intercept] Player " + target.getName() + " is being opped.");
                    whitelistService.addOppedPlayer(target.getId(), target.getName());
                }

            } catch (Exception e) {
                cir.setReturnValue(0);
                Log.error("Error handling op command: " + e.getMessage());
            }
        }

    }

    @Mixin(DeOpCommand.class)
    public abstract static class MinecraftDeOpCommandMixin {

        @Inject(method = "deop", at = @At("HEAD"), cancellable = true)
        private static void injectDeop(ServerCommandSource source, Collection<GameProfile> targets, CallbackInfoReturnable<Integer> cir) {
            if (!WhitelistSyncCore.CONFIG.syncOpList) {
                return;
            }

            try {
                for (GameProfile target : targets) {
                    // Custom logic for handling deop command
                    Log.debug("[Intercept] Player " + target.getName() + " is being deopped.");
                    whitelistService.removeOppedPlayer(target.getId(), target.getName());
                }

            } catch (Exception e) {
                cir.setReturnValue(0);
                Log.error("Error handling deop command: " + e.getMessage());
            }
        }

    }

    @Mixin(BanCommand.class)
    public abstract static class MinecraftBanCommandMixin {

        @Inject(method = "ban", at = @At("HEAD"), cancellable = true)
        private static void injectBan(ServerCommandSource source, Collection<GameProfile> targets, Text reason, CallbackInfoReturnable<Integer> cir) {
            if (!WhitelistSyncCore.CONFIG.webSyncBannedPlayers || !(whitelistService instanceof WebService)) {
                return;
            }

            try {
                for (GameProfile target : targets) {
                    // Custom logic for handling ban command
                    Log.debug("[Intercept] Player " + target.getName() + " is being banned. Reason: " + (reason != null ? reason : "No reason provided."));
                    whitelistService.addBannedPlayer(target.getId(), target.getName(), reason != null ? reason.getString() : null);
                }

            } catch (Exception e) {
                cir.setReturnValue(0);
                Log.error("Error handling ban command: " + e.getMessage());
            }
        }

    }

    @Mixin(PardonCommand.class)
    public abstract static class MinecraftPardonCommandMixin {

        @Inject(method = "pardon", at = @At("HEAD"), cancellable = true)
        private static void injectPardon(ServerCommandSource source, Collection<GameProfile> targets, CallbackInfoReturnable<Integer> cir) {
            if (!WhitelistSyncCore.CONFIG.webSyncBannedPlayers || !(whitelistService instanceof WebService)) {
                return;
            }

            try {
                for (GameProfile target : targets) {
                    // Custom logic for handling pardon command
                    Log.debug("[Intercept] Player " + target.getName() + " is being unbanned.");
                    whitelistService.removeBannedPlayer(target.getId(), target.getName());
                }

            } catch (Exception e) {
                cir.setReturnValue(0);
                Log.error("Error handling pardon command: " + e.getMessage());
            }
        }

    }

    @Mixin(BanIpCommand.class)
    public abstract static class MinecraftBanIpCommandMixin {

        @Inject(method = "banIp", at = @At("HEAD"), cancellable = true)
        private static void injectBanIp(ServerCommandSource source, String targetIp, Text reason, CallbackInfoReturnable<Integer> cir) {
            if (!WhitelistSyncCore.CONFIG.webSyncBannedIps || !(whitelistService instanceof WebService)) {
                return;
            }

            try {
                Log.debug("[Intercept] IP " + targetIp + " is being banned.");
                whitelistService.addBannedIp(targetIp, reason != null ? reason.getString() : null);
            } catch (Exception e) {
                cir.setReturnValue(0);
                Log.error("Error handling ban-ip command: " + e.getMessage());
            }
        }

    }

    @Mixin(PardonIpCommand.class)
    public abstract static class MinecraftPardonIpCommandMixin {

        @Inject(method = "pardonIp", at = @At("HEAD"), cancellable = true)
        private static void injectPardonIp(ServerCommandSource source, String target, CallbackInfoReturnable<Integer> cir) {
            if (!WhitelistSyncCore.CONFIG.webSyncBannedIps || !(whitelistService instanceof WebService)) {
                return;
            }

            try {
                Log.debug("[Intercept] IP " + target + " is being unbanned.");
                whitelistService.removeBannedIp(target);
            } catch (Exception e) {
                cir.setReturnValue(0);
                Log.error("Error handling pardon-ip command: " + e.getMessage());
            }
        }

    }
}
