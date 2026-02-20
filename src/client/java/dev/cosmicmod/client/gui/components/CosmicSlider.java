package dev.cosmicmod.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class CosmicSlider extends AbstractSliderButton {
    private final Consumer<Double> onChange;
    private final double min;
    private final double max;
    private final String suffix;
    private final int decimalPlaces;

    public CosmicSlider(int x, int y, int width, int height, double min, double max, double initialValue, String suffix, int decimalPlaces, Consumer<Double> onChange) {
        super(x, y, width, height, Component.empty(), (initialValue - min) / (max - min));
        this.min = min;
        this.max = max;
        this.suffix = suffix;
        this.decimalPlaces = decimalPlaces;
        this.onChange = onChange;
        this.updateMessage();
    }

    @Override
    protected void updateMessage() {
        double val = min + (value * (max - min));
        String format = "%." + decimalPlaces + "f";
        this.setMessage(Component.literal(String.format(format, val) + suffix));
    }

    @Override
    protected void applyValue() {
        double val = min + (value * (max - min));
        this.onChange.accept(val);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Track
        int trackHeight = 2;
        int trackY = getY() + (height - trackHeight) / 2;
        guiGraphics.fill(getX(), trackY, getX() + width, trackY + trackHeight, 0xFF333333);

        // Knob
        int knobSize = 8;
        int knobX = getX() + (int) (value * (width - knobSize));
        int knobY = getY() + (height - knobSize) / 2;
        
        // Render rounded knob (blue color from screenshot)
        renderRoundedRect(guiGraphics, knobX, knobY, knobSize, knobSize, 0xFF66AAFF);

        // Value text (top right of slider area)
        Minecraft client = Minecraft.getInstance();
        String msg = getMessage().getString();
        int textWidth = client.font.width(msg);
        guiGraphics.drawString(client.font, msg, getX() + width - textWidth, getY() - 10, 0xFFAAAAAA, false);
    }

    private void renderRoundedRect(GuiGraphics guiGraphics, int x, int y, int w, int h, int color) {
        guiGraphics.fill(x + 1, y, x + w - 1, y + h, color);
        guiGraphics.fill(x, y + 1, x + 1, y + h - 1, color);
        guiGraphics.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }
}
