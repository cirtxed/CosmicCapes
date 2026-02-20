package dev.cosmicmod.client.hud;

import dev.cosmicmod.client.feature.FeatureMenu;
import dev.cosmicmod.client.feature.ServerLock;
import net.minecraft.client.gui.GuiGraphics;

public class SearchHud extends HudModule {

    public SearchHud() {
        super("Search Bar", "Adds a search bar to inventories to find items easily.", true, 0, 0, FeatureMenu.MODS, ServerLock.NONE, false);
    }

    @Override
    public void render(GuiGraphics context, float partialTicks) {
        // The search bar is rendered in InventoryMixin.
        // This module serves as a toggle and configuration source.
        this.width = 0;
        this.height = 0;
    }

    @Override
    public boolean hasScaleSetting() {
        return false;
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
    public boolean hasTextShadowSetting() {
        return false;
    }
}
