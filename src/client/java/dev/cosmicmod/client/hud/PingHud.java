package dev.cosmicmod.client.hud;

import dev.cosmicmod.client.feature.FeatureMenu;
import dev.cosmicmod.client.feature.ServerLock;
import net.minecraft.client.gui.GuiGraphics;

public class PingHud extends HudModule {
    public PingHud() {
        super("Pings", "Allows you to ping locations for your teammates.", true, 0, 0, FeatureMenu.MODS, ServerLock.NONE, false);
    }

    @Override
    public void render(GuiGraphics context, float partialTicks) {
        // Pings are rendered in the world and handled by WaypointManager
        // This module doesn't add anything to the player's HUD, so we ensure no width/height
        this.width = 0;
        this.height = 0;
    }

    @Override
    public boolean hasBackgroundSetting() {
        return false;
    }

    @Override
    public boolean hasShowBackgroundSetting() {
        return false;
    }

    @Override
    public boolean hasBorderSetting() {
        return false;
    }

    @Override
    public boolean hasColorSetting() {
        return false;
    }

    @Override
    public boolean hasTextShadowSetting() {
        return false;
    }

    @Override
    public boolean hasScaleSetting() {
        return false;
    }

    @Override
    public boolean hasKeybindSetting() {
        return true;
    }

    public boolean hasPingDurationSetting() {
        return true;
    }
}
