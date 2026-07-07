package net.rmnad.whitelistsyncmod;

import net.minecraft.server.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.players.*;
import net.rmnad.core.Log;
import net.rmnad.core.callbacks.IServerControl;
import net.rmnad.core.logging.LogMessages;

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
        server.getPlayerList().getWhiteList().add(new UserWhiteListEntry(new NameAndId(uuid, name)));
    }

    @Override
    public void removeWhitelistPlayer(UUID uuid, String name) {
        // Called when user removed from whitelist
        server.getPlayerList().getWhiteList().remove(new NameAndId(uuid, name));
    }

    @Override
    public void addOpPlayer(UUID uuid, String name) {
        // Called when user added to op list
        server.getPlayerList().getOps().add(new ServerOpListEntry(new NameAndId(uuid, name), LevelBasedPermissionSet.forLevel(server.operatorUserPermissions().level()), false));
    }

    @Override
    public void removeOpPlayer(UUID uuid, String name) {
        // Called when user removed from op list
        server.getPlayerList().getOps().remove(new NameAndId(uuid, name));
    }

    @Override
    public void addBannedPlayer(UUID uuid, String name, String reason) {
        // Called when user added to ban list
        NameAndId player = new NameAndId(uuid, name);
        server.getPlayerList().getBans().add(new UserBanListEntry(player, null, null, null, reason));

        // If the player is online, kick them
        var onlinePlayer = server.getPlayerList().getPlayer(player.id());
        if (onlinePlayer != null) {
            onlinePlayer.disconnect();
        }
    }

    @Override
    public void removeBannedPlayer(UUID uuid, String name) {
        // Called when user removed from ban list
        server.getPlayerList().getBans().remove(new NameAndId(uuid, name));
    }

    @Override
    public void addBannedIp(String ip, String reason) {
        // Called when IP added to ban list
        server.getPlayerList().getIpBans().add(new IpBanListEntry(ip, null, null, null, reason));

        // If the player is online, kick them
        List<ServerPlayer> onlinePlayers = server.getPlayerList().getPlayersWithAddress(ip);
        for (ServerPlayer onlinePlayer : onlinePlayers) {
            onlinePlayer.disconnect();
        }
    }

    @Override
    public void removeBannedIp(String ip) {
        // Called when IP removed from ban list
        server.getPlayerList().getIpBans().remove(ip);
    }

    @Override
    public void checkWhitelistEnabled() {
        if (!server.getPlayerList().isUsingWhitelist()) {
            Log.info(LogMessages.WARN_WHITELIST_NOT_ENABLED);
            server.setUsingWhitelist(true);
        }
    }

    @Override
    public void versionCheck() {
        // TODO: Implement version check
//        var modInfo = FabricLoader.getInstance().getModContainer(WhitelistSync2.MODID);
//
//        var minecraftInfo = FabricLoader.getInstance().getModContainer("minecraft");
//
//        try {
//            // If this fails, let the server continue to start up.
//            VersionChecker versionChecker = new VersionChecker();
//            if (modInfo.isPresent() && minecraftInfo.isPresent()) {
//                versionChecker.checkVersion(
//                        modInfo.get().getMetadata().getVersion().getFriendlyString(),
//                        minecraftInfo.get().getMetadata().getVersion().getFriendlyString());
//            }
//        } catch (Exception ignore) {}
    }
}
