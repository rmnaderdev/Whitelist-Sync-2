package net.rmnad.whitelistsynclib.callbacks;

import java.util.UUID;

public interface IOnUserRemove {
    void call(UUID uuid, String name);
}
