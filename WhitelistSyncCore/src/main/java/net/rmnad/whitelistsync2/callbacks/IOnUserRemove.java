package net.rmnad.whitelistsync2.callbacks;

import java.util.UUID;

public interface IOnUserRemove {
    void call(UUID uuid, String name);
}
