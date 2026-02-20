package dev.cosmicmod.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class CosmicToggleButton extends AbstractWidget {
    private boolean enabled;
    private String cosmicTooltip;
    private final Consumer<Boolean> onChanged;
    private final String label;

    public CosmicToggleButton(int x, int y, int width, int height, boolean initialState, Consumer<Boolean> onChanged) {
        this(x, y, width, height, "", initialState, onChanged);
    }

    public CosmicToggleButton(int x, int y, int width, int height, String label, boolean initialState, Consumer<Boolean> onChanged) {
        super(x, y, width, height, Component.empty());
        this.label = label;
        this.enabled = initialState;
        this.onChanged = onChanged;
        
        // Auto-adjust width if label is provided
        if (!label.isEmpty()) {
            this.width = Minecraft.getInstance().font.width(label) + 50; // Label + Toggle + Padding
        }
    }

    public void setCosmicTooltip(String tooltip) {
        this.cosmicTooltip = tooltip;
    }

    public String getCosmicTooltip() {
        return cosmicTooltip;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Minecraft client = Minecraft.getInstance();
        int color = enabled ? 0xFF00FF00 : 0xFFFF0000; // Bright Green for ON, Bright Red for OFF
        int trackColor = enabled ? 0x44005500 : 0x44550000;
        String text = enabled ? "ON" : "OFF";
        
        int toggleWidth = 34;
        int toggleHeight = 14;
        int toggleX = getX() + width - toggleWidth - 5;
        int toggleY = getY() + (height - toggleHeight) / 2;

        // Draw Label if it exists
        if (!label.isEmpty()) {
            guiGraphics.drawString(client.font, label, getX() + 5, getY() + (height - 8) / 2, 0xFFAAAAAA, false);
        } else {
            // If no label, the whole button is the toggle
            toggleX = getX();
            toggleY = getY();
            toggleWidth = width;
            toggleHeight = height;
        }

        // Background track
        renderRoundedRect(guiGraphics, toggleX, toggleY, toggleWidth, toggleHeight, 0xAA000000);
        renderRoundedRect(guiGraphics, toggleX + 1, toggleY + 1, toggleWidth - 2, toggleHeight - 2, trackColor);
        
        // The toggle part (the sliding background)
        int thumbWidth = toggleWidth / 2 - 2;
        int thumbHeight = toggleHeight - 4;
        int thumbX = enabled ? toggleX + toggleWidth - thumbWidth - 2 : toggleX + 2;
        int thumbY = toggleY + 2;
        
        renderRoundedRect(guiGraphics, thumbX, thumbY, thumbWidth, thumbHeight, color);
        
        // Draw the text (centered in the thumb)
        int textX = thumbX + (thumbWidth - client.font.width(text)) / 2;
        int textY = thumbY + (thumbHeight - 8) / 2 + 1;
        guiGraphics.drawString(client.font, text, textX, textY, 0xFFFFFFFF, true);
    }

    private void renderRoundedRect(GuiGraphics guiGraphics, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) return;
        guiGraphics.fill(x + 1, y, x + w - 1, y + h, color);
        guiGraphics.fill(x, y + 1, x + 1, y + h - 1, color);
        guiGraphics.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.enabled = !this.enabled;
        if (this.onChanged != null) {
            this.onChanged.accept(this.enabled);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}
