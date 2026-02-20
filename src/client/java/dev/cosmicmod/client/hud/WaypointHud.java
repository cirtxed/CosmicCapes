package dev.cosmicmod.client.hud;

import dev.cosmicmod.client.feature.FeatureMenu;
import dev.cosmicmod.client.feature.ServerLock;
import net.minecraft.client.gui.GuiGraphics;

public class WaypointHud extends HudModule {
    public WaypointHud() {
        super("Waypoints", "Allows you to create and see waypoints in the world.", true, 0, 0, FeatureMenu.MODS, ServerLock.NONE, false);
    }

    @Override
    public void render(GuiGraphics context, float partialTicks) {
        // Waypoints are rendered in the world and handled by WaypointManager
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

    public boolean hasMeteorSetting() {
        return true;
    }
}
