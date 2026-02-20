package dev.cosmicmod.client.gui.casino;

import net.minecraft.client.gui.GuiGraphics;

public interface ICasinoGame {
    void init();
    void tick();
    void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick);
    boolean mouseClicked(double mouseX, double mouseY, int button);
    boolean keyPressed(int keyCode, int scanCode, int modifiers);
    String getName();
}
