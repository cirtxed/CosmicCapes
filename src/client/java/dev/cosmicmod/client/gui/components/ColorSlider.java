package dev.cosmicmod.client.gui.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;

public class ColorSlider extends AbstractWidget {
    private float red;
    private float green;
    private float blue;
    private final Consumer<Integer> onColorChange;

    private final Slider redSlider;
    private final Slider greenSlider;
    private final Slider blueSlider;

    public ColorSlider(int x, int y, int width, int height, int initialColor, Consumer<Integer> onColorChange) {
        super(x, y, width, height, Component.empty());
        this.red = ((initialColor >> 16) & 0xFF) / 255.0f;
        this.green = ((initialColor >> 8) & 0xFF) / 255.0f;
        this.blue = (initialColor & 0xFF) / 255.0f;
        this.onColorChange = onColorChange;

        int sliderHeight = 20;
        this.redSlider = new Slider(x, y, width - 30, sliderHeight, Component.literal("R"), this.red, val -> {
            this.red = val.floatValue();
            this.notifyChange();
        });
        this.greenSlider = new Slider(x, y + 22, width - 30, sliderHeight, Component.literal("G"), this.green, val -> {
            this.green = val.floatValue();
            this.notifyChange();
        });
        this.blueSlider = new Slider(x, y + 44, width - 30, sliderHeight, Component.literal("B"), this.blue, val -> {
            this.blue = val.floatValue();
            this.notifyChange();
        });
    }

    private void notifyChange() {
        if (this.onColorChange != null) {
            this.onColorChange.accept(this.getSelectedColor());
        }
    }

    public int getSelectedColor() {
        int r = (int) (this.red * 255.0f);
        int g = (int) (this.green * 255.0f);
        int b = (int) (this.blue * 255.0f);
        return (r << 16) | (g << 8) | b;
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        this.redSlider.setX(x);
        this.greenSlider.setX(x);
        this.blueSlider.setX(x);
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        this.redSlider.setY(y);
        this.greenSlider.setY(y + 22);
        this.blueSlider.setY(y + 44);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.redSlider.render(guiGraphics, mouseX, mouseY, partialTick);
        this.greenSlider.render(guiGraphics, mouseX, mouseY, partialTick);
        this.blueSlider.render(guiGraphics, mouseX, mouseY, partialTick);

        // Preview
        int previewSize = 44 + 20; // Matches total height
        int previewX = this.getX() + this.width - 25;
        int previewY = this.getY();
        
        guiGraphics.fill(previewX - 1, previewY - 1, previewX + 25 + 1, previewY + previewSize + 1, 0xFFFFFFFF);
        guiGraphics.fill(previewX, previewY, previewX + 25, previewY + previewSize, 0xFF000000 | getSelectedColor());
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        this.redSlider.mouseMoved(mouseX, mouseY);
        this.greenSlider.mouseMoved(mouseX, mouseY);
        this.blueSlider.mouseMoved(mouseX, mouseY);
    }

    private Slider focusedSlider;

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.redSlider.mouseClicked(mouseX, mouseY, button)) {
            this.focusedSlider = this.redSlider;
            return true;
        }
        if (this.greenSlider.mouseClicked(mouseX, mouseY, button)) {
            this.focusedSlider = this.greenSlider;
            return true;
        }
        if (this.blueSlider.mouseClicked(mouseX, mouseY, button)) {
            this.focusedSlider = this.blueSlider;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.redSlider.mouseReleased(mouseX, mouseY, button);
        this.greenSlider.mouseReleased(mouseX, mouseY, button);
        this.blueSlider.mouseReleased(mouseX, mouseY, button);
        this.focusedSlider = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.focusedSlider != null) return this.focusedSlider.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    private static class Slider extends AbstractSliderButton {
        private final Consumer<Double> onChange;
        private final Component prefix;

        public Slider(int x, int y, int width, int height, Component prefix, double initialValue, Consumer<Double> onChange) {
            super(x, y, width, height, Component.empty(), initialValue);
            this.prefix = prefix;
            this.onChange = onChange;
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.literal(this.prefix.getString() + ": " + (int) (this.value * 255.0)));
        }

        @Override
        protected void applyValue() {
            this.onChange.accept(this.value);
        }
    }
}
