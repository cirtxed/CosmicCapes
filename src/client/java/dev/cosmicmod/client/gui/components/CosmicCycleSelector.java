package dev.cosmicmod.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

public class CosmicCycleSelector extends AbstractWidget {
    private final List<String> options;
    private int index;
    private final Consumer<String> onChanged;

    public CosmicCycleSelector(int x, int y, int width, int height, List<String> options, String initialValue, Consumer<String> onChanged) {
        super(x, y, width, height, Component.empty());
        this.options = options;
        this.index = options.indexOf(initialValue);
        if (this.index == -1) this.index = 0;
        this.onChanged = onChanged;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Minecraft client = Minecraft.getInstance();
        int color = 0xFFAAAAAA;
        
        // Left arrow
        guiGraphics.drawString(client.font, "<", getX(), getY() + (height - 8) / 2, color);
        
        // Value
        String value = options.get(index);
        int valueWidth = client.font.width(value);
        guiGraphics.drawString(client.font, value, getX() + (width - valueWidth) / 2, getY() + (height - 8) / 2, 0xFFFFFFFF);
        
        // Right arrow
        guiGraphics.drawString(client.font, ">", getX() + width - client.font.width(">"), getY() + (height - 8) / 2, color);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (mouseX < getX() + width / 2.0) {
            index--;
            if (index < 0) index = options.size() - 1;
        } else {
            index++;
            if (index >= options.size()) index = 0;
        }
        
        if (onChanged != null) {
            onChanged.accept(options.get(index));
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}
