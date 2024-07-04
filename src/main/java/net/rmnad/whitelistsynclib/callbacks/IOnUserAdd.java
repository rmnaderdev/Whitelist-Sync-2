package net.rmnad.whitelistsynclib.callbacks;

import java.util.UUID;

public interface IOnUserAdd {
    void call(UUID uuid, String name);
}
