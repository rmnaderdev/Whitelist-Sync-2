package net.rmnad.callbacks;

import java.util.UUID;

public interface IServerControl {
    void addWhitelistPlayer(UUID uuid, String name);
    void removeWhitelistPlayer(UUID uuid, String name);
    void addOpPlayer(UUID uuid, String name);
    void removeOpPlayer(UUID uuid, String name);
}
