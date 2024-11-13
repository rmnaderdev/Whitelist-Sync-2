package net.rmnad.fabric_1_21;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.rmnad.callbacks.IServerControl;

import java.util.List;
import java.util.UUID;

public class ServerControl implements IServerControl {

    private final MinecraftServer server;

    public ServerControl(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void addWhitelistPlayer(UUID uuid, String name) {
        // Called when user added to whitelist
        server.getPlayerManager().getWhitelist().add(new WhitelistEntry(new GameProfile(uuid, name)));
    }

    @Override
    public void removeWhitelistPlayer(UUID uuid, String name) {
        // Called when user removed from whitelist
        server.getPlayerManager().getWhitelist().remove(new WhitelistEntry(new GameProfile(uuid, name)));
    }

    @Override
    public void addOpPlayer(UUID uuid, String name) {
        // Called when user added to op list
        server.getPlayerManager().getOpList().add(new OperatorEntry(new GameProfile(uuid, name), server.getOpPermissionLevel(), false));
    }

    @Override
    public void removeOpPlayer(UUID uuid, String name) {
        // Called when user removed from op list
        server.getPlayerManager().getOpList().remove(new OperatorEntry(new GameProfile(uuid, name), 0, false));
    }

    @Override
    public void addBannedPlayer(UUID uuid, String name, String reason) {
        // Called when user added to ban list
        GameProfile gameProfile = new GameProfile(uuid, name);
        server.getPlayerManager().getUserBanList().add(new BannedPlayerEntry(gameProfile, null, null, null, reason));

        // If the player is online, kick them
        var onlinePlayer = server.getPlayerManager().getPlayer(gameProfile.getId());
        if (onlinePlayer != null) {
            onlinePlayer.networkHandler.disconnect(Text.translatable("multiplayer.disconnect.banned"));
        }
    }

    @Override
    public void removeBannedPlayer(UUID uuid, String name) {
        // Called when user removed from ban list
        server.getPlayerManager().getUserBanList().remove(new BannedPlayerEntry(new GameProfile(uuid, name)));
    }

    @Override
    public void addBannedIp(String ip, String reason) {
        // Called when IP added to ban list
        server.getPlayerManager().getIpBanList().add(new BannedIpEntry(ip, null, null, null, reason));

        // If the player is online, kick them
        List<ServerPlayerEntity> onlinePlayers = server.getPlayerManager().getPlayersByIp(ip);
        for (ServerPlayerEntity onlinePlayer : onlinePlayers) {
            onlinePlayer.networkHandler.disconnect(Text.translatable("multiplayer.disconnect.banned"));
        }
    }

    @Override
    public void removeBannedIp(String ip) {
        // Called when IP removed from ban list
        server.getPlayerManager().getIpBanList().remove(ip);
    }
}
