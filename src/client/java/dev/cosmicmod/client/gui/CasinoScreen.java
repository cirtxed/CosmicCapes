package dev.cosmicmod.client.gui;

import dev.cosmicmod.client.CosmicConfig;
import dev.cosmicmod.client.gui.casino.ICasinoGame;
import dev.cosmicmod.client.gui.casino.SlotsGame;
import dev.cosmicmod.client.gui.casino.BlackjackGame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class CasinoScreen extends AbstractCosmicScreen {
    private GameMode selectedMode = GameMode.SLOTS;
    private final List<GameModeButton> modeButtons = new ArrayList<>();
    private ICasinoGame currentGame;

    public CasinoScreen(Screen parent) {
        super(Component.literal("Casino"), parent);
        this.activeTab = 3;
        switchGame(selectedMode);
    }

    public int getLeft() { return left; }
    public int getTop() { return top; }
    public int getGuiWidth() { return guiWidth; }
    public int getGuiHeight() { return guiHeight; }

    public void renderRoundedRectPublic(GuiGraphics guiGraphics, int x, int y, int w, int h, int color) {
        renderRoundedRect(guiGraphics, x, y, w, h, color);
    }

    public String formatNumberPublic(long number) {
        return String.format("%,d", number);
    }

    private void switchGame(GameMode mode) {
        selectedMode = mode;
        switch (mode) {
            case SLOTS:
                currentGame = new SlotsGame(this);
                break;
            case BLACKJACK:
                currentGame = new BlackjackGame(this);
                break;
            default:
                currentGame = null;
                break;
        }
        if (currentGame != null) {
            currentGame.init();
        }
    }

    @Override
    protected void init() {
        super.init();
        modeButtons.clear();

        int buttonWidth = 80;
        int buttonHeight = 20;
        int startX = left + 15;
        int startY = top + 65;

        for (GameMode mode : GameMode.values()) {
            modeButtons.add(new GameModeButton(startX, startY, buttonWidth, buttonHeight, mode));
            startY += buttonHeight + 10;
        }

        if (currentGame != null) {
            currentGame.init();
        }
    }

    @Override
    public void tick() {
        if (currentGame != null) {
            currentGame.tick();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Sidebar separator
        guiGraphics.fill(left + 105, top + 45, left + 106, top + guiHeight - 15, 0x11FFFFFF);

        // Coin Display
        String coinsText = "Coins: " + formatNumberPublic(CosmicConfig.getInstance().casinoCoins);
        guiGraphics.drawString(this.font, coinsText, left + 15, top + 45, 0xFFFFFF00, false);

        // Render Sidebar Buttons
        for (GameModeButton button : modeButtons) {
            button.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        // Render Game Area
        int areaX = left + 115;
        int areaY = top + 45;
        int areaWidth = guiWidth - 130;
        int areaHeight = guiHeight - 60;

        if (currentGame != null) {
            guiGraphics.drawString(this.font, currentGame.getName(), areaX, areaY, 0xFFFFFFFF, false);
            currentGame.render(guiGraphics, mouseX, mouseY, partialTick);
        } else {
            guiGraphics.drawString(this.font, selectedMode.name, areaX, areaY, 0xFFFFFFFF, false);
            guiGraphics.drawCenteredString(this.font, selectedMode.name + " Coming Soon...", areaX + areaWidth / 2, areaY + areaHeight / 2, 0x555555);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (GameModeButton btn : modeButtons) {
                if (btn.isHovered((int) mouseX, (int) mouseY)) {
                    if (selectedMode != btn.mode) {
                        switchGame(btn.mode);
                    }
                    return true;
                }
            }

            if (currentGame != null && currentGame.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        } else if (button == 1) {
            if (currentGame != null && currentGame.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (currentGame != null && currentGame.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private enum GameMode {
        MINES("Mines"),
        SLOTS("Slots"),
        BLACKJACK("Blackjack");

        final String name;
        GameMode(String name) { this.name = name; }
    }

    private class GameModeButton {
        int x, y, w, h;
        GameMode mode;

        GameModeButton(int x, int y, int w, int h, GameMode mode) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.mode = mode;
        }

        void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = isHovered(mouseX, mouseY);
            boolean selected = selectedMode == mode;
            
            int bgColor = selected ? 0x66FFFFFF : (hovered ? 0x44FFFFFF : 0x22FFFFFF);
            renderRoundedRect(guiGraphics, x, y, w, h, bgColor);
            
            int textColor = selected ? 0xFFFFFFFF : (hovered ? 0xCCFFFFFF : 0x88FFFFFF);
            guiGraphics.drawCenteredString(font, mode.name, x + w / 2, y + (h - 8) / 2, textColor);
        }

        boolean isHovered(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        }
    }

    public static class SlotButton {
        public int x, y, w, h;
        public String text;
        public Runnable action;
        public java.util.function.Supplier<Boolean> isGolden;

        public SlotButton(int x, int y, int w, int h, String text, Runnable action) {
            this(x, y, w, h, text, action, () -> false);
        }

        public SlotButton(int x, int y, int w, int h, String text, Runnable action, java.util.function.Supplier<Boolean> isGolden) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.text = text;
            this.action = action;
            this.isGolden = isGolden;
        }

        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = isHovered(mouseX, mouseY);
            int bgColor = hovered ? 0x66FFFFFF : 0x22FFFFFF;
            
            if (isGolden.get()) {
                bgColor = hovered ? 0xCCFFD700 : 0x88FFD700; // Golden color (FFD700)
            }
            
            guiGraphics.fill(x + 2, y, x + w - 2, y + h, bgColor);
            guiGraphics.fill(x, y + 2, x + 2, y + h - 2, bgColor);
            guiGraphics.fill(x + w - 2, y + 2, x + w, y + h - 2, bgColor);
            if (w >= 4 && h >= 4) {
                guiGraphics.fill(x + 1, y + 1, x + 2, y + 2, bgColor);
                guiGraphics.fill(x + w - 2, y + 1, x + w - 1, y + 2, bgColor);
                guiGraphics.fill(x + 1, y + h - 2, x + 2, y + h - 1, bgColor);
                guiGraphics.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, bgColor);
            }

            guiGraphics.drawCenteredString(Minecraft.getInstance().font, text, x + w / 2, y + (h - 8) / 2, 0xFFFFFFFF);
        }

        public boolean isHovered(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        }
    }

    public static class AutoSpinButton extends SlotButton {
        public AutoSpinButton(int x, int y, int w, int h, String text, Runnable action) {
            super(x, y, w, h, text, action);
        }
    }
}
