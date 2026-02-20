package dev.cosmicmod.client.gui;

import dev.cosmicmod.client.CosmicConfig;
import dev.cosmicmod.client.gui.components.ColorSlider;
import dev.cosmicmod.client.gui.components.CosmicSlider;
import dev.cosmicmod.client.gui.components.CosmicToggleButton;
import dev.cosmicmod.client.hud.HudModule;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ModSettingsScreen extends AbstractCosmicScreen {
    private final HudModule module;
    private double scrollAmount;
    private int contentHeight;
    private EditBox labelEdit;

    public ModSettingsScreen(Screen parent, HudModule module) {
        super(Component.literal(module.getName()), parent);
        this.module = module;
        this.activeTab = -1;
    }

    @Override
    protected void init() {
        super.init();
        CosmicConfig.HudSettings settings = module.getSettings();
        if (settings == null) return;

        int startX = left + 25;
        int currentY = top + 50;
        int widgetWidth = 40;
        int widgetHeight = 20;

        // Mod description
        if (!module.getDescription().isEmpty()) {
            currentY += 15;
        }

        currentY += 10; // Space after header line

        int scrollY = (int) (currentY - scrollAmount);

        // Master Enable Toggle
        this.addRenderableWidget(new CosmicToggleButton(startX, scrollY, 150, widgetHeight, "Enabled", module.isEnabled(), (value) -> {
            module.setEnabled(value);
        }));
        currentY += 25;
        scrollY += 25;

        // Show Label Toggle
        if (module.getName().equals("Satchel Hud") || module.getName().equals("Player List") || module.getName().equals("Clue Scroll Hud")) {
            this.addRenderableWidget(new CosmicToggleButton(startX, scrollY, 150, widgetHeight, "Show Label", settings.showLabel, (value) -> {
                settings.showLabel = value;
                CosmicConfig.save();
                this.rebuildWidgets();
            }));
            currentY += 25;
            scrollY += 25;

            if (settings.showLabel) {
                labelEdit = new EditBox(font, startX + 95, scrollY, 135, 20, Component.literal("Custom Label"));
                labelEdit.setValue(settings.label != null ? settings.label : "");
                labelEdit.setResponder(value -> {
                    settings.label = value;
                    CosmicConfig.save();
                });
                this.addRenderableWidget(labelEdit);
                currentY += 30;
                scrollY += 30;
            }
        }

        // Scale
        if (module.hasScaleSetting()) {
            this.addRenderableWidget(new CosmicSlider(startX + 80, scrollY, 150, 20, 0.5, 2.0, settings.scale, "", 2, (value) -> {
                settings.scale = value.floatValue();
                CosmicConfig.save();
            }));
            currentY += 30;
            scrollY += 30;
        }

        // Mod specific: Reverse Order
        if (module.hasReverseOrderSetting()) {
            this.addRenderableWidget(new CosmicToggleButton(startX, scrollY, 150, widgetHeight, "Reverse Order", settings.reverseOrder, (value) -> {
                settings.reverseOrder = value;
                CosmicConfig.save();
            }));
            currentY += 25;
            scrollY += 25;
        }

        // GENERAL section
        currentY += 5;
        scrollY += 5;

        if (module.hasTextShadowSetting()) {
            this.addRenderableWidget(new CosmicToggleButton(startX, scrollY, 150, widgetHeight, "Text Shadow", settings.textShadow, (value) -> {
                settings.textShadow = value;
                CosmicConfig.save();
            }));
            currentY += 25;
            scrollY += 25;
        }

        if (module.hasShowBackgroundSetting()) {
            this.addRenderableWidget(new CosmicToggleButton(startX, scrollY, 150, widgetHeight, "Show Background", settings.showBackground, (value) -> {
                settings.showBackground = value;
                CosmicConfig.save();
                this.rebuildWidgets(); // Re-init to show/hide nested settings
            }));
            
            if (settings.showBackground) {
                int nestedX = startX + 15;
                int nestedY = currentY + 25; // Adjusted spacing
                int nestedScrollY = scrollY + 25;
                
                if (module.hasBorderSetting()) {
                    this.addRenderableWidget(new CosmicToggleButton(nestedX, nestedScrollY, 135, widgetHeight, "Border", settings.showBorder, (value) -> {
                        settings.showBorder = value;
                        CosmicConfig.save();
                    }));
                    nestedY += 25;
                    nestedScrollY += 25;
                }

                if (module.hasBackgroundSetting()) {
                    // ColorSlider doesn't use the label yet, keep it as is for now or add label support
                    this.addRenderableWidget(new ColorSlider(nestedX + 80, nestedScrollY, 150, 66, settings.backgroundColor, (color) -> {
                        settings.backgroundColor = 0x6F000000 | (color & 0xFFFFFF);
                        CosmicConfig.save();
                    }));
                    nestedY += 75;
                    nestedScrollY += 75;
                }
                
                currentY = nestedY;
                scrollY = nestedScrollY;
            } else {
                currentY += 25;
                scrollY += 25;
            }
        }

        // COLOR section
        currentY += 10;
        scrollY += 10;

        if (module.hasFormattingColorSetting()) {
            this.addRenderableWidget(new ColorSlider(startX + 80, scrollY, 150, 66, settings.formattingColor, (color) -> {
                settings.formattingColor = 0xFF000000 | (color & 0xFFFFFF);
                CosmicConfig.save();
            }));
            currentY += 75;
            scrollY += 75;
        }


        if (module.hasKeybindSetting()) {
            if (module.getName().equals("Waypoints")) {
                // Waypoint Visibility Keybind
                KeyMapping toggleKey = dev.cosmicmod.client.CosmicmodClient.toggleWaypointsKey;
                this.addRenderableWidget(new AbstractCosmicScreen.CosmicButton(startX + 80, scrollY, 130, 20, Component.literal("Visibility: " + toggleKey.getTranslatedKeyMessage().getString()), (button) -> {
                    this.minecraft.setScreen(new KeybindChangeScreen(this, toggleKey));
                }));
                currentY += 25;
                scrollY += 25;

                // Create Waypoint Keybind
                KeyMapping createKey = dev.cosmicmod.client.CosmicmodClient.waypointKey;
                this.addRenderableWidget(new AbstractCosmicScreen.CosmicButton(startX + 80, scrollY, 130, 20, Component.literal("Create: " + createKey.getTranslatedKeyMessage().getString()), (button) -> {
                    this.minecraft.setScreen(new KeybindChangeScreen(this, createKey));
                }));
                currentY += 25;
                scrollY += 25;
            } else {
                KeyMapping keyMapping = null;
                if (module.getName().equals("Pings")) {
                    keyMapping = dev.cosmicmod.client.CosmicmodClient.pingKey;
                }

                if (keyMapping != null) {
                    final KeyMapping finalKey = keyMapping;
                    this.addRenderableWidget(new AbstractCosmicScreen.CosmicButton(startX + 80, scrollY, 130, 20, Component.literal(finalKey.getTranslatedKeyMessage().getString()), (button) -> {
                        this.minecraft.setScreen(new KeybindChangeScreen(this, finalKey));
                    }));
                    currentY += 25;
                    scrollY += 25;
                }
            }
        }

        if (module.hasPingDurationSetting()) {
            this.addRenderableWidget(new CosmicSlider(startX + 85, scrollY + 5, 125, 20, 10.0, 60.0, (double) CosmicConfig.getInstance().pingDuration, "s", 0, (value) -> {
                CosmicConfig.getInstance().pingDuration = value.intValue();
                CosmicConfig.save();
            }));
            currentY += 35;
            scrollY += 35;
        }

        if (module.hasPlayerListSettings()) {
            CosmicConfig config = CosmicConfig.getInstance();
            
            // Ignore Friends
            this.addRenderableWidget(new CosmicToggleButton(startX, scrollY, 150, widgetHeight, "Ignore Friends", config.playerListIgnoreFriends, (value) -> {
                config.playerListIgnoreFriends = value;
                CosmicConfig.save();
            }));
            currentY += 25;
            scrollY += 25;

            // Highlight Friends
            this.addRenderableWidget(new CosmicToggleButton(startX, scrollY, 150, widgetHeight, "Highlight Friends", config.playerListHighlightFriends, (value) -> {
                config.playerListHighlightFriends = value;
                CosmicConfig.save();
            }));
            currentY += 25;
            scrollY += 25;

            // Ignore Self
            this.addRenderableWidget(new CosmicToggleButton(startX, scrollY, 150, widgetHeight, "Ignore Self", config.playerListIgnoreSelf, (value) -> {
                config.playerListIgnoreSelf = value;
                CosmicConfig.save();
            }));
            currentY += 25;
            scrollY += 25;

            // Highlight Self
            this.addRenderableWidget(new CosmicToggleButton(startX, scrollY, 150, widgetHeight, "Highlight Self", config.playerListHighlightSelf, (value) -> {
                config.playerListHighlightSelf = value;
                CosmicConfig.save();
            }));
            currentY += 25;
            scrollY += 25;
        }

        if (module.hasHandScalerSettings()) {
            CosmicConfig config = CosmicConfig.getInstance();

            this.addRenderableWidget(new CosmicSlider(startX + 85, scrollY + 5, 125, 20, 0.1, 2.0, (double) config.mainHandScale, "x (Main Hand)", 2, (value) -> {
                config.mainHandScale = value.floatValue();
                CosmicConfig.save();
            }));
            currentY += 35;
            scrollY += 35;

            this.addRenderableWidget(new CosmicSlider(startX + 85, scrollY + 5, 125, 20, 0.1, 2.0, (double) config.offHandScale, "x (Off Hand)", 2, (value) -> {
                config.offHandScale = value.floatValue();
                CosmicConfig.save();
            }));
            currentY += 35;
            scrollY += 35;
        }

        if (module instanceof dev.cosmicmod.client.hud.WaypointHud waypointHud && waypointHud.hasMeteorSetting()) {
            this.addRenderableWidget(new CosmicToggleButton(startX, scrollY, 150, widgetHeight, "Meteor Waypoints", settings.showMeteorWaypoints, (value) -> {
                settings.showMeteorWaypoints = value;
                CosmicConfig.save();
            }));
            currentY += 25;
            scrollY += 25;

            this.addRenderableWidget(new CosmicSlider(startX + 85, scrollY + 5, 125, 20, 10.0, 60.0, (double) settings.meteorDuration, "s", 0, (value) -> {
                settings.meteorDuration = value.intValue();
                CosmicConfig.save();
            }));
            currentY += 35;
            scrollY += 35;
        }

        this.contentHeight = currentY - (top + 50);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int viewHeight = guiHeight - 80;
        if (contentHeight > viewHeight) {
            double oldScroll = scrollAmount;
            scrollAmount = Math.max(0, Math.min(scrollAmount - scrollY * 20, contentHeight - viewHeight));
            if (oldScroll != scrollAmount) {
                this.rebuildWidgets();
            }
        }
        return true;
    }

    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        CosmicConfig.HudSettings settings = module.getSettings();
        if (settings == null) {
            super.renderContent(guiGraphics, mouseX, mouseY, partialTick);
            return;
        }

        // Scissor for content
        int scissorX = left + 5;
        int scissorY = top + 40;
        int scissorW = guiWidth - 10;
        int scissorH = guiHeight - 80;

        // Clip labels and widgets
        guiGraphics.enableScissor(scissorX, scissorY, scissorX + scissorW, scissorY + scissorH);

        int startX = left + 25;
        int currentY = top + 45;
        int drawY = (int) (currentY - scrollAmount);

        // Mod description
        if (!module.getDescription().isEmpty()) {
            guiGraphics.drawString(this.font, module.getDescription(), startX, drawY, 0xFFAAAAAA, false);
            drawY += 15;
        }

        // Header line
        guiGraphics.fill(startX, drawY, left + guiWidth - 25, drawY + 1, 0x11FFFFFF);
        drawY += 10;

        // Toggles now draw their own labels
        drawY += 25;

        // Show Label Label
        if (module.getName().equals("Satchel Hud") || module.getName().equals("Player List") || module.getName().equals("Clue Scroll Hud")) {
            if (settings.showLabel) {
                guiGraphics.drawString(this.font, "Custom Label", startX + 10, drawY + 5, 0xFFAAAAAA, false);
                drawY += 30;
            }
        }

        // Scale Label
        if (module.hasScaleSetting()) {
            guiGraphics.drawString(this.font, "Scale", startX, drawY + 5, 0xFFAAAAAA, false);
            drawY += 30;
        }

        // Reverse Order
        if (module.hasReverseOrderSetting()) {
            drawY += 25;
        }

        // GENERAL section
        drawY += 5;

        if (module.hasTextShadowSetting()) {
            drawY += 25;
        }

        if (module.hasShowBackgroundSetting()) {
            if (settings.showBackground) {
                int nestedX = startX + 15;
                int nestedY = drawY + 25;

                if (module.hasBorderSetting()) {
                    nestedY += 25;
                }

                if (module.hasBackgroundSetting()) {
                    guiGraphics.drawString(this.font, "Background Color", nestedX, nestedY + 5, 0xFFAAAAAA, false);
                    nestedY += 70;
                }
                
                drawY = nestedY;
            } else {
                drawY += 25;
            }
        }

        // COLOR section
        drawY += 10;

        if (module.hasFormattingColorSetting()) {
            guiGraphics.drawString(this.font, "Text Color", startX, drawY + 5, 0xFFAAAAAA, false);
            drawY += 70;
        }

        if (module.hasTintSetting()) {
            guiGraphics.drawString(this.font, "Tint Color", startX, drawY + 5, 0xFFAAAAAA, false);
            drawY += 70;
        }

        if (module.hasKeybindSetting()) {
            guiGraphics.drawString(this.font, "Keybind", startX, drawY + 5, 0xFFAAAAAA, false);
            drawY += 25;
        }

        if (module.hasPingDurationSetting()) {
            guiGraphics.drawString(this.font, "Ping Duration", startX, drawY + 5, 0xFFAAAAAA, false);
            drawY += 30;
        }

        if (module.hasHandScalerSettings()) {
            guiGraphics.drawString(this.font, "Main Hand Scale", startX, drawY + 5, 0xFFAAAAAA, false);
            drawY += 30;
            guiGraphics.drawString(this.font, "Off Hand Scale", startX, drawY + 5, 0xFFAAAAAA, false);
            drawY += 30;
        }

        if (module instanceof dev.cosmicmod.client.hud.WaypointHud waypointHud && waypointHud.hasMeteorSetting()) {
            drawY += 25;
            guiGraphics.drawString(this.font, "Meteor Duration", startX, drawY + 5, 0xFFAAAAAA, false);
            drawY += 30;
        }

        if (module.hasPlayerListSettings()) {
            drawY += 25 * 4;
        }

        super.renderContent(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.disableScissor();

        // Optional: Draw a scrollbar
        int viewHeight = guiHeight - 80;
        if (contentHeight > viewHeight) {
            int scrollbarHeight = (int) ((viewHeight / (double) contentHeight) * viewHeight);
            int scrollbarY = scissorY + (int) ((scrollAmount / (double) (contentHeight - viewHeight)) * (viewHeight - scrollbarHeight));
            int scrollbarX = left + guiWidth - 8;
            guiGraphics.fill(scrollbarX, scissorY, scrollbarX + 4, scissorY + viewHeight, 0x22FFFFFF);
            guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + 4, scrollbarY + scrollbarHeight, 0x88FFFFFF);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private static class KeybindChangeScreen extends AbstractCosmicScreen {
        private final KeyMapping keyMapping;

        public KeybindChangeScreen(Screen parent, KeyMapping keyMapping) {
            super(Component.literal("CHANGE KEYBIND"), parent);
            this.keyMapping = keyMapping;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            super.render(guiGraphics, mouseX, mouseY, partialTick);
            guiGraphics.drawCenteredString(this.font, "Press any key to bind to " + keyMapping.getName(), width / 2, height / 2, 0xFFFFFF);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == 256) { // ESC
                this.onClose();
                return true;
            }
            keyMapping.setKey(com.mojang.blaze3d.platform.InputConstants.getKey(keyCode, scanCode));
            net.minecraft.client.Minecraft.getInstance().options.save();
            this.onClose();
            return true;
        }
    }
}
