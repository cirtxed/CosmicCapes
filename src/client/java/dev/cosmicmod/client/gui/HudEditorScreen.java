package dev.cosmicmod.client.gui;

import dev.cosmicmod.client.hud.HudManager;
import dev.cosmicmod.client.hud.HudModule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class HudEditorScreen extends Screen {
    private final Screen parent;
    private HudModule draggingModule = null;
    private float dragStartX, dragStartY;
    private float initialModuleX, initialModuleY;

    public HudEditorScreen(Screen parent) {
        super(Component.literal("HUD Editor"));
        this.parent = parent;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render a dark semi-transparent background to show we are in a screen
        guiGraphics.fill(0, 0, this.width, this.height, 0x44000000);
        
        // Split screen into 4 quadrants (center lines)
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        guiGraphics.fill(centerX, 0, centerX + 1, this.height, 0x33FFFFFF); // Vertical line
        guiGraphics.fill(0, centerY, this.width, centerY + 1, 0x33FFFFFF); // Horizontal line
        
        List<HudModule> modules = HudManager.getModules();
        for (HudModule module : modules) {
            if (module.isEnabled() && module.isMoveable()) {
                // Draw a highlight/border around the module
                float x = module.getX();
                float y = module.getY();
                float w = module.getWidth();
                float h = module.getHeight();
                
                if (w == 0) w = 50; // Fallback for modules with no size
                if (h == 0) h = 20;

                int borderColor = (module == draggingModule) ? 0xFFFFBB00 : 0xAAFFFFFF;
                
                // Draw module preview
                module.render(guiGraphics, partialTick);
                
                // Draw example section if it has one or is a major module
                if (w > 0 && h > 0) {
                    renderModuleExample(guiGraphics, module, x, y, w, h);
                }
                
                // Draw border over it
                guiGraphics.fill((int)x - 1, (int)y - 1, (int)(x + w + 1), (int)y, borderColor); // Top
                guiGraphics.fill((int)x - 1, (int)(y + h), (int)(x + w + 1), (int)(y + h + 1), borderColor); // Bottom
                guiGraphics.fill((int)x - 1, (int)y, (int)x, (int)(y + h), borderColor); // Left
                guiGraphics.fill((int)(x + w), (int)y, (int)(x + w + 1), (int)(y + h), borderColor); // Right
                
                // Draw label
                guiGraphics.drawString(this.font, module.getName(), (int)x, (int)y - 10, 0xFFFFFFFF, true);
            }
        }

        guiGraphics.drawCenteredString(this.font, "DRAG HUD ITEMS TO MOVE THEM", this.width / 2, 10, 0xFFBBBBBB);
        guiGraphics.drawCenteredString(this.font, "ESC TO SAVE AND EXIT", this.width / 2, 22, 0xFFBBBBBB);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderModuleExample(GuiGraphics guiGraphics, HudModule module, float x, float y, float w, float h) {
        String customLabel = module.getSettings().label;
        String title = (customLabel != null && !customLabel.isEmpty()) ? customLabel.replace("&", "§") : "";

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        float scale = module.getSettings().scale;
        guiGraphics.pose().scale(scale, scale, 1.0f);

        switch (module.getName()) {
            case "Satchel Hud":
                if (module.getWidth() < 5) { // Likely empty
                    if (title.isEmpty()) title = "§c§lSatchels";
                    guiGraphics.drawString(font, title, 2, 2, 0xFFFFFF, true);
                    guiGraphics.drawString(font, "    §8Coal §7: §f1,234 / 5,000", 2, 14, 0xFFFFFF, true);
                }
                break;
            case "Player List":
                if (module.getWidth() < 5) {
                    if (title.isEmpty()) title = "§bPlayers (2):";
                    guiGraphics.drawString(font, title, 2, 2, 0xFFFFFF, true);
                    guiGraphics.drawString(font, "- Player1", 2, 14, 0x55FF55, true);
                    guiGraphics.drawString(font, "- Player2", 2, 24, 0xFFFFFF, true);
                }
                break;
            case "Clue Scroll Hud":
                if (module.getWidth() < 5) {
                    if (title.isEmpty()) title = "§6§lClue Scroll";
                    guiGraphics.drawString(font, title, 2, 2, 0xFFFFFF, true);
                    guiGraphics.drawString(font, "§e* §fMining (15/50)", 6, 12, 0xFFFFFF, true);
                }
                break;
            case "Cooldown Hud":
                if (module.getWidth() < 5) {
                    guiGraphics.drawString(font, "§eCooldowns", 2, 2, 0xFFFFFF, true);
                    guiGraphics.drawString(font, "§fPearl: §a4.2s", 2, 14, 0xFFFFFF, true);
                }
                break;
        }

        guiGraphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            List<HudModule> modules = HudManager.getModules();
            // Iterate in reverse to select the one on top
            for (int i = modules.size() - 1; i >= 0; i--) {
                HudModule module = modules.get(i);
                if (module.isEnabled() && module.isMoveable()) {
                    float x = module.getX();
                    float y = module.getY();
                    float w = module.getWidth();
                    float h = module.getHeight();
                    
                    if (w == 0) w = 50;
                    if (h == 0) h = 20;

                    if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
                        draggingModule = module;
                        dragStartX = (float) mouseX;
                        dragStartY = (float) mouseY;
                        initialModuleX = module.getX();
                        initialModuleY = module.getY();
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingModule = null;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && draggingModule != null) {
            float deltaX = (float) mouseX - dragStartX;
            float deltaY = (float) mouseY - dragStartY;
            
            float newX = initialModuleX + deltaX;
            float newY = initialModuleY + deltaY;
            
            // Snap to screen edges?
            if (newX < 5) newX = 0;
            if (newY < 5) newY = 0;
            if (newX + draggingModule.getWidth() > this.width - 5) newX = this.width - draggingModule.getWidth();
            if (newY + draggingModule.getHeight() > this.height - 5) newY = this.height - draggingModule.getHeight();
            
            // Snap to center lines (quadrants)
            float centerX = this.width / 2.0f;
            float centerY = this.height / 2.0f;
            
            // Snap left edge to center
            if (Math.abs(newX - centerX) < 5) newX = centerX;
            // Snap right edge to center
            if (Math.abs((newX + draggingModule.getWidth()) - centerX) < 5) newX = centerX - draggingModule.getWidth();
            // Snap center X to center
            if (Math.abs((newX + draggingModule.getWidth() / 2.0f) - centerX) < 5) newX = centerX - draggingModule.getWidth() / 2.0f;
            
            // Snap top edge to center
            if (Math.abs(newY - centerY) < 5) newY = centerY;
            // Snap bottom edge to center
            if (Math.abs((newY + draggingModule.getHeight()) - centerY) < 5) newY = centerY - draggingModule.getHeight();
            // Snap center Y to center
            if (Math.abs((newY + draggingModule.getHeight() / 2.0f) - centerY) < 5) newY = centerY - draggingModule.getHeight() / 2.0f;

            draggingModule.setX(newX);
            draggingModule.setY(newY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
