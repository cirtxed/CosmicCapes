package dev.cosmicmod.client.hud;

import dev.cosmicmod.client.SearchManager;
import dev.cosmicmod.client.feature.FeatureMenu;
import dev.cosmicmod.client.feature.ServerLock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ItemOverlayHud extends HudModule {

    public ItemOverlayHud() {
        super("Item Overlay", "Shows extra info about the held item (Energy, Charges, Value).", true, 5, 200, FeatureMenu.MODS, ServerLock.NONE, false);
    }

    @Override
    public void render(GuiGraphics context, float partialTicks) {
        // This module no longer draws on the HUD directly.
        // It serves as a toggle and configuration source for item-level overlays
        // rendered in InventoryMixin and GuiMixin.
        this.width = 0;
        this.height = 0;
    }

    @Override
    public boolean hasFormattingColorSetting() {
        return true;
    }

    @Override
    public boolean hasTintSetting() {
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
