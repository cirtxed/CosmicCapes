package dev.cosmicmod.client.gui;

import dev.cosmicmod.client.CosmicConfig;
import dev.cosmicmod.client.gui.components.CosmicToggleButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class CosmicConfigScreen extends AbstractCosmicScreen {

    public CosmicConfigScreen(Screen parent) {
        super(Component.literal("Settings"), parent);
        this.activeTab = 1; // Settings tab
    }

    @Override
    protected void init() {
        super.init();
        CosmicConfig config = CosmicConfig.getInstance();
        
        int col1X = left + 25;
        int currentY = top + 55; 
        int spacing = 18;
        int toggleHeight = 15;

        // --- COSMETICS ---
        currentY += 12; 
        
        addToggle(col1X, currentY, toggleHeight, "Orbital Orbs", config.showOrbs, "Toggle the orbital orbs around your player.", val -> { config.showOrbs = val; CosmicConfig.save(); });
        currentY += spacing;

        addToggle(col1X, currentY, toggleHeight, "Diamond Chain", config.showDiamondChain, "Toggle the diamond chain cosmetic.", val -> { config.showDiamondChain = val; CosmicConfig.save(); });
        currentY += spacing;

        addToggle(col1X, currentY, toggleHeight, "Player Auras", config.showAuras, "Toggle the particle aura around your player.", val -> { config.showAuras = val; CosmicConfig.save(); });
        currentY += spacing;

        addToggle(col1X, currentY, toggleHeight, "Item Textures", !config.itemTextureOverrides.isEmpty(), "Toggle the custom item texture overrides.", val -> {
            if (!val) {
                config.itemTextureOverrides.clear();
            } else {
                config.itemTextureOverrides.put("Energy", "minecraft:item/emerald");
                config.itemTextureOverrides.put("Trinket", "minecraft:item/nether_star");
            }
            CosmicConfig.save();
        });
        currentY += spacing;

        // --- GENERAL ---
        currentY += 25;
        addToggle(col1X, currentY, toggleHeight, "Show Waypoints", dev.cosmicmod.client.CosmicmodClient.getWaypointHud().isEnabled(), "Global toggle for waypoint visibility in the world.", val -> { dev.cosmicmod.client.CosmicmodClient.getWaypointHud().setEnabled(val); });
        currentY += spacing;

        addToggle(col1X, currentY, toggleHeight, "Mining + Right Click", config.allowMiningWhileRightClicking, "Allows you to mine while holding right-click.", val -> { config.allowMiningWhileRightClicking = val; CosmicConfig.save(); });
        currentY += spacing;
    }

    private void addToggle(int x, int y, int h, String label, boolean state, String tooltip, java.util.function.Consumer<Boolean> consumer) {
        CosmicToggleButton toggle = new CosmicToggleButton(x, y, 100, h, label, state, consumer);
        toggle.setCosmicTooltip(tooltip);
        this.addRenderableWidget(toggle);
    }

    private void addSlider(int x, int y, int w, int h, String label, float initialValue, java.util.function.Consumer<Double> consumer) {
        this.addRenderableWidget(new net.minecraft.client.gui.components.AbstractSliderButton(x, y, w, h, Component.literal(label + ": " + (int)(initialValue * 100) + "%"), initialValue) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(label + ": " + (int)(this.value * 100) + "%"));
            }

            @Override
            protected void applyValue() {
                consumer.accept(this.value);
            }
        });
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        int col1X = left + 25;
        int currentY = top + 55; 
        int spacing = 18;

        int headerColor = 0xFF777777;

        // COSMETICS
        guiGraphics.drawString(this.font, "COSMETICS", col1X, currentY, headerColor, false);
        currentY += 12;
        // Toggles now render their own labels
        
        // GENERAL
        currentY += 10 + 25;
        guiGraphics.drawString(this.font, "GENERAL", col1X, currentY, headerColor, false);
    }
}
