package net.rmnad.callbacks;

import java.util.UUID;

public interface IOnUserOpAdd {
    void call(UUID uuid, String name);
}
