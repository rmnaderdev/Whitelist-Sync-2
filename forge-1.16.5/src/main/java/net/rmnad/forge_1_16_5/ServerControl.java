package net.rmnad.forge_1_16_5;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.IPBanEntry;
import net.minecraft.server.management.ProfileBanEntry;
import net.minecraft.server.management.WhitelistEntry;
import net.minecraft.util.text.TranslationTextComponent;
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
        server.getPlayerList().getWhiteList().add(new WhitelistEntry(new GameProfile(uuid, name)));
    }

    @Override
    public void removeWhitelistPlayer(UUID uuid, String name) {
        // Called when user removed from whitelist
        server.getPlayerList().getWhiteList().remove(new WhitelistEntry(new GameProfile(uuid, name)));
    }

    @Override
    public void addOpPlayer(UUID uuid, String name) {
        // Called when user added to op list
        server.getPlayerList().op(new GameProfile(uuid, name));
    }

    @Override
    public void removeOpPlayer(UUID uuid, String name) {
        // Called when user removed from op list
        server.getPlayerList().deop(new GameProfile(uuid, name));
    }

    @Override
    public void addBannedPlayer(UUID uuid, String name, String reason) {
        // Called when user added to ban list
        GameProfile gameProfile = new GameProfile(uuid, name);
        server.getPlayerList().getBans().add(new ProfileBanEntry(gameProfile, null, null, null, reason));

        // If the player is online, kick them
        ServerPlayerEntity onlinePlayer = server.getPlayerList().getPlayer(gameProfile.getId());
        if (onlinePlayer != null) {
            onlinePlayer.connection.disconnect(new TranslationTextComponent("multiplayer.disconnect.banned"));
        }
    }

    @Override
    public void removeBannedPlayer(UUID uuid, String name) {
        // Called when user removed from ban list
        server.getPlayerList().getBans().remove(new ProfileBanEntry(new GameProfile(uuid, name)));
    }

    @Override
    public void addBannedIp(String ip, String reason) {
        // Called when IP added to ban list
        server.getPlayerList().getIpBans().add(new IPBanEntry(ip, null, null, null, reason));

        // If the player is online, kick them
        List<ServerPlayerEntity> onlinePlayers = server.getPlayerList().getPlayersWithAddress(ip);
        for (ServerPlayerEntity onlinePlayer : onlinePlayers) {
            onlinePlayer.connection.disconnect(new TranslationTextComponent("multiplayer.disconnect.banned"));
        }
    }

    @Override
    public void removeBannedIp(String ip) {
        // Called when IP removed from ban list
        server.getPlayerList().getIpBans().remove(new IPBanEntry(ip));
    }
}
