package net.rmnad.callbacks;

import java.util.UUID;

public interface IOnUserOpRemove {
    void call(UUID uuid, String name);
}
