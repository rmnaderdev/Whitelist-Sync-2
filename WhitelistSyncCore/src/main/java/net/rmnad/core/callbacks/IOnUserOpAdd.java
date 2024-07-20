package net.rmnad.core.callbacks;

import java.util.UUID;

public interface IOnUserOpAdd {
    void call(UUID uuid, String name);
}
