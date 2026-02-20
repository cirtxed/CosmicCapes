package dev.cosmicmod.client.feature;

import dev.cosmicmod.client.ChatStateManager;

public enum ServerLock {
    NONE,
    COSMIC_SKY,
    COSMIC_PRISONS;

    public boolean isLocked() {
        return switch (this) {
            case NONE -> false;
            case COSMIC_SKY -> !ChatStateManager.isCosmicSky();
            case COSMIC_PRISONS -> !ChatStateManager.isCosmicPrisons();
        };
    }
}
