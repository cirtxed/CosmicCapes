package dev.cosmicmod.client.util;

import java.util.UUID;

public interface PlayerUUIDHolder {
    void cosmicmod$setUUID(UUID uuid);
    UUID cosmicmod$getUUID();
    void cosmicmod$setLocalPlayer(boolean local);
    boolean cosmicmod$isLocalPlayer();
}
