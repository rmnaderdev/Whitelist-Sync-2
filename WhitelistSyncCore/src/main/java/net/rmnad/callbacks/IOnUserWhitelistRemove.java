package net.rmnad.callbacks;

import java.util.UUID;

public interface IOnUserWhitelistRemove {
    void call(UUID uuid, String name);
}
