package net.rmnad.forge_1_21;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.UserWhiteListEntry;
import net.rmnad.callbacks.IServerControl;

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
}
