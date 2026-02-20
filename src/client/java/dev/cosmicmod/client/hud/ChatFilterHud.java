package dev.cosmicmod.client.hud;

import dev.cosmicmod.client.CosmicConfig;
import net.minecraft.client.gui.GuiGraphics;

public class ChatFilterHud extends HudModule {
    public ChatFilterHud() {
        super("Chat Filter", "Condenses lootbox messages in chat", true, 0, 0, dev.cosmicmod.client.feature.FeatureMenu.MODS, dev.cosmicmod.client.feature.ServerLock.NONE, false);
    }

    @Override
    public void render(GuiGraphics context, float partialTicks) {
        // This module doesn't have a HUD element
    }

    @Override
    public boolean isEnabled() {
        return CosmicConfig.getInstance().chatFilterEnabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        CosmicConfig.getInstance().chatFilterEnabled = enabled;
        CosmicConfig.save();
    }

    @Override
    public boolean hasShowBackgroundSetting() {
        return false;
    }

    @Override
    public boolean hasBackgroundSetting() {
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
    public boolean hasTextShadowSetting() {
        return false;
    }
}
