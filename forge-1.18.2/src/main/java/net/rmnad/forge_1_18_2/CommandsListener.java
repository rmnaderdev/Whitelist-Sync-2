package net.rmnad.forge_1_18_2;

import com.google.common.net.InetAddresses;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.rmnad.Log;
import net.rmnad.WhitelistSyncCore;
import net.rmnad.services.BaseService;
import net.rmnad.services.WebService;

import java.util.Collection;

@Mod.EventBusSubscriber(modid = WhitelistSync2.MODID)
public class CommandsListener {

    @SubscribeEvent
    public static void onCommandEvent(CommandEvent event) {
        CommandContext<CommandSourceStack> context = event.getParseResults().getContext().build(event.getParseResults().getReader().getString());
        String command = context.getInput();
        BaseService whitelistService = WhitelistSyncCore.whitelistService;

        // Ignore if player does not have permission
        if (!context.getSource().hasPermission(3))
            return;

        if (command.startsWith("whitelist add")) {
            try {
                Collection<GameProfile> targets = GameProfileArgument.getGameProfiles(context, "targets");

                for (GameProfile target : targets) {
                    // Custom logic for handling whitelist add
                    Log.debug("[Intercept] Player " + target.getName() + " is being added to the whitelist.");
                    whitelistService.addWhitelistPlayer(target.getId(), target.getName());
                }

            } catch (Exception e) {
                event.setCanceled(true);
                Log.error("Error handling whitelist add command: " + e.getMessage());
            }
        }
        else if (command.startsWith("whitelist remove")) {
            try {
                Collection<GameProfile> targets = GameProfileArgument.getGameProfiles(context, "targets");

                for (GameProfile target : targets) {
                    // Custom logic for handling whitelist remove
                    Log.debug("[Intercept] Player " + target.getName() + " is being removed from the whitelist.");
                    whitelistService.removeWhitelistPlayer(target.getId(), target.getName());
                }

            } catch (Exception e) {
                event.setCanceled(true);
                Log.error("Error handling whitelist add command: " + e.getMessage());
            }
        }
        else if (command.startsWith("op ")) {
            if (!WhitelistSyncCore.CONFIG.syncOpList) {
                return;
            }

            try {
                Collection<GameProfile> targets = GameProfileArgument.getGameProfiles(context, "targets");

                for (GameProfile target : targets) {
                    // Custom logic for handling op command
                    Log.debug("[Intercept] Player " + target.getName() + " is being opped.");
                    whitelistService.addOppedPlayer(target.getId(), target.getName());
                }

            } catch (Exception e) {
                event.setCanceled(true);
                Log.error("Error handling op command: " + e.getMessage());
            }
        }
        else if (command.startsWith("deop ")) {
            if (!WhitelistSyncCore.CONFIG.syncOpList) {
                return;
            }

            try {
                Collection<GameProfile> targets = GameProfileArgument.getGameProfiles(context, "targets");

                for (GameProfile target : targets) {
                    // Custom logic for handling deop command
                    Log.debug("[Intercept] Player " + target.getName() + " is being deopped.");
                    whitelistService.removeOppedPlayer(target.getId(), target.getName());
                }

            } catch (Exception e) {
                event.setCanceled(true);
                Log.error("Error handling deop command: " + e.getMessage());
            }
        }
        else if (command.startsWith("ban-ip ")) {
            if (!WhitelistSyncCore.CONFIG.webSyncBannedIps || !(whitelistService instanceof WebService)) {
                return;
            }

            try {
                String target = StringArgumentType.getString(context, "target");
                String reason = null;
                try {
                    reason = MessageArgument.getMessage(context, "reason").getString();
                } catch (IllegalArgumentException e) {
                    // No reason provided
                }

                if (!InetAddresses.isInetAddress(target)) {
                    // Target is player, get player ip address
                    ServerPlayer player = context.getSource().getServer().getPlayerList().getPlayerByName(target);
                    if (player != null) {
                        target = player.getIpAddress();
                    } else {
                        return;
                    }
                }

                Log.debug("[Intercept] IP " + target + " is being banned.");
                whitelistService.addBannedIp(target, reason);
            } catch (Exception e) {
                event.setCanceled(true);
                Log.error("Error handling ban-ip command: " + e.getMessage());
            }
        }
        else if (command.startsWith("pardon-ip ")) {
            if (!WhitelistSyncCore.CONFIG.webSyncBannedIps || !(whitelistService instanceof WebService)) {
                return;
            }

            try {
                String target = StringArgumentType.getString(context, "target");

                Log.debug("[Intercept] IP " + target + " is being unbanned.");
                whitelistService.removeBannedIp(target);
            } catch (Exception e) {
                event.setCanceled(true);
                Log.error("Error handling pardon-ip command: " + e.getMessage());
            }
        }
        else if (command.startsWith("ban ")) {
            if (!WhitelistSyncCore.CONFIG.webSyncBannedPlayers || !(whitelistService instanceof WebService)) {
                return;
            }

            try {
                Collection<GameProfile> targets = GameProfileArgument.getGameProfiles(context, "targets");
                String reason = null;
                try {
                    reason = MessageArgument.getMessage(context, "reason").getString();
                } catch (IllegalArgumentException e) {
                    // No reason provided
                }

                for (GameProfile target : targets) {
                    // Custom logic for handling ban command
                    Log.debug("[Intercept] Player " + target.getName() + " is being banned. Reason: " + (reason != null ? reason : "No reason provided."));
                    whitelistService.addBannedPlayer(target.getId(), target.getName(), reason);
                }

            } catch (Exception e) {
                event.setCanceled(true);
                Log.error("Error handling ban command: " + e.getMessage());
            }
        }
        else if (command.startsWith("pardon ")) {
            if (!WhitelistSyncCore.CONFIG.webSyncBannedPlayers || !(whitelistService instanceof WebService)) {
                return;
            }

            try {
                Collection<GameProfile> targets = GameProfileArgument.getGameProfiles(context, "targets");

                for (GameProfile target : targets) {
                    // Custom logic for handling pardon command
                    Log.debug("[Intercept] Player " + target.getName() + " is being unbanned.");
                    whitelistService.removeBannedPlayer(target.getId(), target.getName());
                }

            } catch (Exception e) {
                event.setCanceled(true);
                Log.error("Error handling pardon command: " + e.getMessage());
            }
        }
    }

}
