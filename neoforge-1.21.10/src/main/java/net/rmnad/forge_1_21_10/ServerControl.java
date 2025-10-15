package net.rmnad.forge_1_21_10;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.server.players.UserWhiteListEntry;
import net.neoforged.fml.ModList;
import net.rmnad.core.Log;
import net.rmnad.core.callbacks.IServerControl;
import net.rmnad.core.logging.LogMessages;
import net.rmnad.core.services.VersionChecker;

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
        server.getPlayerList().getWhiteList().add(new UserWhiteListEntry(new GameProfile(uuid, name)));
    }

    @Override
    public void removeWhitelistPlayer(UUID uuid, String name) {
        // Called when user removed from whitelist
        server.getPlayerList().getWhiteList().remove(new UserWhiteListEntry(new GameProfile(uuid, name)));
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
        server.getPlayerList().getBans().add(new UserBanListEntry(gameProfile, null, null, null, reason));

        // If the player is online, kick them
        ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(gameProfile.getId());
        if (onlinePlayer != null) {
            onlinePlayer.connection.disconnect(Component.translatable("multiplayer.disconnect.banned"));
        }
    }

    @Override
    public void removeBannedPlayer(UUID uuid, String name) {
        // Called when user removed from ban list
        server.getPlayerList().getBans().remove(new UserBanListEntry(new GameProfile(uuid, name)));
    }

    @Override
    public void addBannedIp(String ip, String reason) {
        // Called when IP added to ban list
        server.getPlayerList().getIpBans().add(new IpBanListEntry(ip, null, null, null, reason));

        // If the player is online, kick them
        List<ServerPlayer> onlinePlayers = server.getPlayerList().getPlayersWithAddress(ip);
        for (ServerPlayer onlinePlayer : onlinePlayers) {
            onlinePlayer.connection.disconnect(Component.translatable("multiplayer.disconnect.banned"));
        }
    }

    @Override
    public void removeBannedIp(String ip) {
        // Called when IP removed from ban list
        server.getPlayerList().getIpBans().remove(new IpBanListEntry(ip));
    }

    @Override
    public void checkWhitelistEnabled() {
        // Check if whitelisting is enabled.
        if (!this.server.getPlayerList().isUsingWhitelist()) {
            Log.info(LogMessages.WARN_WHITELIST_NOT_ENABLED);
            this.server.getPlayerList().setUsingWhiteList(true);
        }
    }

    @Override
    public void versionCheck() {
        try {
            // If this fails, let the server continue to start up.
            VersionChecker versionChecker = new VersionChecker();
            var modInfo = ModList.get().getModContainerById("whitelistsync2");
            var minecraftInfo = ModList.get().getModContainerById("minecraft");

            if (modInfo.isPresent() && minecraftInfo.isPresent()) {
                versionChecker.checkVersion(
                        modInfo.get().getModInfo().getVersion(),
                        minecraftInfo.get().getModInfo().getVersion());
            }
        } catch (Exception ignore) {}
    }
}
