package net.rmnad.whitelistsync2.callbacks;

import java.util.UUID;

public interface IOnUserWhitelistAdd {
    void call(UUID uuid, String name);
}
