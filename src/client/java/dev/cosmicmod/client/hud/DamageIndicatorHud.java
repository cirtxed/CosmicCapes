package dev.cosmicmod.client.hud;

import dev.cosmicmod.client.feature.FeatureMenu;
import dev.cosmicmod.client.feature.ServerLock;
import net.minecraft.client.gui.GuiGraphics;

public class DamageIndicatorHud extends HudModule {
    public DamageIndicatorHud() {
        super("Damage Indicator", "Displays floating damage numbers over entities when hit.", true, 0, 0, FeatureMenu.MODS, ServerLock.NONE, false);
    }

    @Override
    public void render(GuiGraphics context, float partialTicks) {
        // This is a dummy module to provide a way to toggle DamageIndicatorManager via HudManager
        // It doesn't render anything on the HUD itself (screen space), but controls the 3D world rendering.
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
}
