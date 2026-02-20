package dev.cosmicmod.client.hud;

import dev.cosmicmod.client.feature.FeatureMenu;
import dev.cosmicmod.client.feature.ServerLock;
import net.minecraft.client.gui.GuiGraphics;

public class HandRescalerHud extends HudModule {
    public HandRescalerHud() {
        super("Hand Rescaler", "Adjust the scale of your main and off hands in first-person view.", true, 0, 0, FeatureMenu.MODS, ServerLock.NONE, false);
    }

    @Override
    public void render(GuiGraphics context, float partialTicks) {
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
    public boolean hasTextShadowSetting() {
        return false;
    }

    @Override
    public boolean hasBorderSetting() {
        return false;
    }

    @Override
    public boolean hasScaleSetting() {
        return false;
    }

    @Override
    public boolean hasFormattingColorSetting() {
        return false;
    }

    @Override
    public boolean hasHandScalerSettings() {
        return true;
    }
}
