package net.rmnad.callbacks;

import java.util.UUID;

public interface IOnUserWhitelistAdd {
    void call(UUID uuid, String name);
}
