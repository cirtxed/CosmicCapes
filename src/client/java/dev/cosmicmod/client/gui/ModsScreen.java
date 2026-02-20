package dev.cosmicmod.client.gui;

import dev.cosmicmod.client.feature.FeatureMenu;
import dev.cosmicmod.client.hud.HudManager;
import dev.cosmicmod.client.hud.HudModule;
import dev.cosmicmod.client.util.ItemOverlayUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.stream.Collectors;

public class ModsScreen extends AbstractCosmicScreen {
    private final List<HudModule> modules;

    public ModsScreen(Screen parent) {
        super(Component.literal("Mods"), parent);
        this.activeTab = 0; // Mods tab
        this.modules = HudManager.getModules().stream()
                .filter(m -> m.getMenu() == FeatureMenu.MODS && !(m instanceof dev.cosmicmod.client.hud.NotificationHud))
                .sorted(java.util.Comparator.comparing(HudModule::getName))
                .collect(Collectors.toList());
    }

    @Override
    protected void init() {
        super.init();
        refreshButtons();
    }

    private void refreshButtons() {
        this.clearWidgets();
        
        int startX = left + 25;
        int startY = top + 55;
        int buttonWidth = 125;
        int buttonHeight = 40;
        int spacingX = 10;
        int spacingY = 15;
        
        // Add Move HUD Items button (no longer scrolls)
        int moveButtonX = left + guiWidth - 115;
        int moveButtonY = top + 15; // Move to header area near tabs
        this.addRenderableWidget(new AbstractCosmicScreen.CosmicButton(moveButtonX, moveButtonY, 80, 15, Component.literal("EDIT HUD"), (button) -> {
            this.minecraft.setScreen(new HudEditorScreen(this));
        }));
        
        for (int i = 0; i < modules.size(); i++) {
            HudModule module = modules.get(i);
            int x = startX + (i % 3) * (buttonWidth + spacingX);
            int y = startY + (i / 3) * (buttonHeight + spacingY);
            
            this.addRenderableWidget(new ModButton(x, y, buttonWidth, buttonHeight, module, (button) -> {
                this.minecraft.setScreen(new ModSettingsScreen(this, module));
            }));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Draw title
        int titleY = top + 42;
        guiGraphics.drawString(this.font, "HUD MODULES", left + 20, titleY, 0x777777, false);
    }

    private class ModButton extends AbstractWidget {
        private final HudModule module;
        private final java.util.function.Consumer<ModButton> onClick;

        public ModButton(int x, int y, int width, int height, HudModule module, java.util.function.Consumer<ModButton> onClick) {
            super(x, y, width, height, Component.literal(module.getName()));
            this.module = module;
            this.onClick = onClick;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.isHoveredOrFocused();
            boolean locked = module.getServerLock().isLocked();
            int bgColor = locked ? 0x44FF5555 : (hovered ? 0x44FFFFFF : 0x22FFFFFF);
            int borderColor = locked ? 0xAAFF5555 : (hovered ? 0xAAFFFFFF : 0x44FFFFFF);

            renderRoundedRect(guiGraphics, getX(), getY(), width, height, bgColor);
            // Draw border
            renderRoundedOutline(guiGraphics, (int)getX(), (int)getY(), width, height, borderColor);

            int textColor = locked ? 0xFFFFAAAA : (hovered ? 0xFFFFFFFF : 0xFFAAAAAA);
            guiGraphics.drawCenteredString(font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, textColor);

            if (locked) {
                guiGraphics.drawCenteredString(font, "LOCKED", getX() + width / 2, getY() + height - 10, 0xFFFF5555);
            } else {
                String status = module.isEnabled() ? "§aENABLED" : "§cDISABLED";
                guiGraphics.drawCenteredString(font, status, getX() + width / 2, getY() + height - 10, 0xFFFFFFFF);
            }
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if (!module.getServerLock().isLocked()) {
                onClick.accept(this);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
    }
}
