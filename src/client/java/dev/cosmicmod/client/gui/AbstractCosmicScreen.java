package dev.cosmicmod.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractCosmicScreen extends Screen {
    protected final Screen parent;
    protected int guiWidth = 450;
    protected int guiHeight = 300;
    protected int left, top;

    private final List<Tab> tabs = new ArrayList<>();
    protected int activeTab = 0;

    protected AbstractCosmicScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
        
        tabs.add(new Tab("MODS", 0));
        tabs.add(new Tab("WAYPOINTS", 1));
        tabs.add(new Tab("FRIENDS", 2));
        tabs.add(new Tab("CASINO", 3));
    }

    @Override
    protected void init() {
        this.left = (this.width - guiWidth) / 2;
        this.top = (this.height - guiHeight) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(guiGraphics);
        
        // Main Background
        renderRoundedRect(guiGraphics, left, top, guiWidth, guiHeight, 0xDD101015);
        
        // Header
        renderHeader(guiGraphics, mouseX, mouseY);
        
        renderContent(guiGraphics, mouseX, mouseY, partialTick);

        // Back button (Bottom Right)
        if (this.parent != null && (!(this.parent instanceof AbstractCosmicScreen) || this.activeTab == -1)) {
            int backX = left + guiWidth - 70;
            int backY = top + guiHeight - 30;
            boolean backHovered = mouseX >= backX && mouseX <= backX + 60 && mouseY >= backY && mouseY <= backY + 20;
            int backColor = backHovered ? 0x66FFFFFF : 0x22FFFFFF;
            renderRoundedRect(guiGraphics, backX, backY, 60, 20, backColor);
            guiGraphics.drawCenteredString(this.font, "Back", backX + 30, backY + 6, 0xFFFFFFFF);
        }
    }

    public static class CosmicButton extends net.minecraft.client.gui.components.Button {
        public CosmicButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = isHoveredOrFocused();
            int bgColor = hovered ? 0x66FFFFFF : 0x22FFFFFF;
            int borderColor = hovered ? 0xAAFFFFFF : 0x44FFFFFF;

            if (!this.active) bgColor = 0x11FFFFFF;

            // Using the parent's renderRoundedRect if possible, or local implementation
            renderRoundedRectLocal(guiGraphics, getX(), getY(), width, height, bgColor);

            // Draw border (simple outline)
            guiGraphics.fill(getX() + 1, getY(), getX() + width - 1, getY() + 1, borderColor);
            guiGraphics.fill(getX() + 1, getY() + height - 1, getX() + width - 1, getY() + height, borderColor);
            guiGraphics.fill(getX(), getY() + 1, getX() + 1, getY() + height - 1, borderColor);
            guiGraphics.fill(getX() + width - 1, getY() + 1, getX() + width, getY() + height - 1, borderColor);

            guiGraphics.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, 0xFFFFFFFF);
        }

        private void renderRoundedRectLocal(GuiGraphics guiGraphics, int x, int y, int w, int h, int color) {
            if (w <= 0 || h <= 0) return;
            guiGraphics.fill(x + 2, y, x + w - 2, y + h, color);
            guiGraphics.fill(x, y + 2, x + 2, y + h - 2, color);
            guiGraphics.fill(x + w - 2, y + 2, x + w, y + h - 2, color);
            if (w >= 4 && h >= 4) {
                guiGraphics.fill(x + 1, y + 1, x + 2, y + 2, color);
                guiGraphics.fill(x + w - 2, y + 1, x + w - 1, y + 2, color);
                guiGraphics.fill(x + 1, y + h - 2, x + 2, y + h - 1, color);
                guiGraphics.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, color);
            }
        }
    }

    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    protected void renderHeader(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Exit Button (Top Right)
        int exitX = left + guiWidth - 30;
        int exitY = top + 10;
        boolean exitHovered = mouseX >= exitX && mouseX <= exitX + 20 && mouseY >= exitY && mouseY <= exitY + 20;
        
        int exitColor = exitHovered ? 0x66FF5555 : 0x22FFFFFF;
        renderRoundedRect(guiGraphics, exitX, exitY, 20, 20, exitColor);
        guiGraphics.drawCenteredString(this.font, "X", exitX + 10, exitY + 6, 0xFFFFFFFF);

        // Screen title
        String subTitle = this.title.getString().toUpperCase();
        guiGraphics.drawString(this.font, subTitle, left + 15, top + 15, 0xFFFFFFFF, false);

        // Tab Bar (Simplified as clickable text)
        int tabX = left + 120;
        for (int i = 0; i < tabs.size(); i++) {
            Tab tab = tabs.get(i);
            int color = (activeTab == i) ? 0xFFFFFFFF : 0x88FFFFFF;
            int textWidth = this.font.width(tab.name);
            boolean hovered = mouseX >= tabX && mouseX <= tabX + textWidth && mouseY >= top + 15 && mouseY <= top + 15 + 10;
            if (hovered && activeTab != i) color = 0xCCFFFFFF;

            guiGraphics.drawString(this.font, tab.name, tabX, top + 15, color, false);
            
            // Underline for active tab
            if (activeTab == i) {
                guiGraphics.fill(tabX, top + 26, tabX + textWidth, top + 27, 0xFFFFFFFF);
            }
            
            tabX += textWidth + 15;
        }

        // Header separator (Below header)
        guiGraphics.fill(left + 10, top + 35, left + guiWidth - 10, top + 36, 0x11FFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Exit button
            int exitX = left + guiWidth - 30;
            int exitY = top + 10;
            if (mouseX >= exitX && mouseX <= exitX + 20 && mouseY >= exitY && mouseY <= exitY + 20) {
                this.minecraft.setScreen(null);
                return true;
            }

            // Back button
            if (this.parent != null && (!(this.parent instanceof AbstractCosmicScreen) || this.activeTab == -1)) {
                int backX = left + guiWidth - 70;
                int backY = top + guiHeight - 30;
                if (mouseX >= backX && mouseX <= backX + 60 && mouseY >= backY && mouseY <= backY + 20) {
                    this.onClose();
                    return true;
                }
            }

            // Tabs
            int tabX = left + 120;
            for (int i = 0; i < tabs.size(); i++) {
                Tab tab = tabs.get(i);
                int textWidth = this.font.width(tab.name);
                if (mouseX >= tabX && mouseX <= tabX + textWidth && mouseY >= top + 15 && mouseY <= top + 15 + 10) {
                    if (activeTab != i) {
                        switchTab(i);
                        return true;
                    }
                }
                tabX += textWidth + 15;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    protected void switchTab(int index) {
        if (this.activeTab == index) return;
        
        this.activeTab = index;
        Screen newScreen = null;
        switch (index) {
            case 0:
                newScreen = new ModsScreen(null);
                break;
            case 1:
                newScreen = new WaypointsScreen(null);
                break;
            case 2:
                newScreen = new FriendsScreen(null);
                break;
            case 3:
                newScreen = new CasinoScreen(null);
                break;
        }
        
        if (newScreen != null) {
            this.minecraft.setScreen(newScreen);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        for (net.minecraft.client.gui.components.events.GuiEventListener listener : this.children()) {
            if (listener.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    protected void renderRoundedRect(GuiGraphics guiGraphics, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) return;
        guiGraphics.fill(x + 2, y, x + w - 2, y + h, color);
        guiGraphics.fill(x, y + 2, x + 2, y + h - 2, color);
        guiGraphics.fill(x + w - 2, y + 2, x + w, y + h - 2, color);
        
        // Corners
        if (w >= 4 && h >= 4) {
            guiGraphics.fill(x + 1, y + 1, x + 2, y + 2, color);
            guiGraphics.fill(x + w - 1, y + 1, x + w - 2, y + 2, color);
            guiGraphics.fill(x + 1, y + h - 1, x + 2, y + h - 2, color);
            guiGraphics.fill(x + w - 1, y + h - 1, x + w - 2, y + h - 2, color);
        }
    }

    protected void renderRoundedOutline(GuiGraphics guiGraphics, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) return;
        // Horizontal lines
        guiGraphics.fill(x + 2, y, x + w - 2, y + 1, color); // Top
        guiGraphics.fill(x + 2, y + h - 1, x + w - 2, y + h, color); // Bottom
        // Vertical lines
        guiGraphics.fill(x, y + 2, x + 1, y + h - 2, color); // Left
        guiGraphics.fill(x + w - 1, y + 2, x + w, y + h - 2, color); // Right
        
        // Corners (to match renderRoundedRect)
        if (w >= 4 && h >= 4) {
            guiGraphics.fill(x + 1, y + 1, x + 2, y + 2, color);
            guiGraphics.fill(x + w - 2, y + 1, x + w - 1, y + 2, color);
            guiGraphics.fill(x + 1, y + h - 2, x + 2, y + h - 1, color);
            guiGraphics.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, color);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    private static class Tab {
        final String name;
        final int id;

        Tab(String name, int id) {
            this.name = name;
            this.id = id;
        }
    }
}
