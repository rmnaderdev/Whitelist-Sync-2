package net.rmnad.callbacks;

import java.util.UUID;

public interface IServerControl {
    void addWhitelistPlayer(UUID uuid, String name);
    void removeWhitelistPlayer(UUID uuid, String name);
    void addOpPlayer(UUID uuid, String name);
    void removeOpPlayer(UUID uuid, String name);

    void addBannedPlayer(UUID uuid, String name, String reason);
    void removeBannedPlayer(UUID uuid, String name);

    void addBannedIp(String ip, String reason);
    void removeBannedIp(String ip);

    void checkWhitelistEnabled();
    void versionCheck();
}
