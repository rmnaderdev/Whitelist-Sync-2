package net.rmnad.core.callbacks;

import java.util.UUID;

public interface IOnUserOpRemove {
    void call(UUID uuid, String name);
}
