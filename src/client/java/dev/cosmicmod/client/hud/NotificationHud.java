package dev.cosmicmod.client.hud;

import dev.cosmicmod.client.feature.FeatureMenu;
import dev.cosmicmod.client.feature.ServerLock;
import dev.cosmicmod.client.notification.Notification;
import dev.cosmicmod.client.notification.NotificationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.util.List;

public class NotificationHud extends HudModule {
    public NotificationHud() {
        super("Notifications", "Displays notifications in the bottom right.", true, 0, 0, FeatureMenu.MODS, ServerLock.NONE);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void render(GuiGraphics context, float partialTicks) {
        Minecraft client = Minecraft.getInstance();
        List<Notification> notifications = NotificationManager.getNotifications();
        if (notifications.isEmpty()) return;

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        
        int currentY = screenHeight - 5;
        int padding = 5;
        int iconSize = 16;
        int maxWidth = 150;

        for (int i = notifications.size() - 1; i >= 0; i--) {
            Notification notification = notifications.get(i);
            
            List<FormattedCharSequence> lines = client.font.split(Component.literal(notification.getText()), maxWidth);
            int textWidth = 0;
            for (FormattedCharSequence line : lines) {
                textWidth = Math.max(textWidth, client.font.width(line));
            }

            int contentWidth = textWidth;
            if (!notification.getIcon().isEmpty()) {
                contentWidth += iconSize + padding;
            }
            
            int boxWidth = contentWidth + (padding * 2);
            int boxHeight = Math.max(iconSize, lines.size() * client.font.lineHeight) + (padding * 2);
            
            int x = screenWidth - boxWidth - 5;
            int y = currentY - boxHeight;

            // Draw background
            context.fill(x, y, x + boxWidth, y + boxHeight, 0x90000000);
            
            // Draw border
            context.fill(x, y, x + boxWidth, y + 1, 0xFF000000); // Top
            context.fill(x, y + boxHeight - 1, x + boxWidth, y + boxHeight, 0xFF000000); // Bottom
            context.fill(x, y, x + 1, y + boxHeight, 0xFF000000); // Left
            context.fill(x + boxWidth - 1, y, x + boxWidth, y + boxHeight, 0xFF000000); // Right

            int textX = x + padding;
            if (!notification.getIcon().isEmpty()) {
                context.renderItem(notification.getIcon(), x + padding, y + (boxHeight / 2) - (iconSize / 2));
                textX += iconSize + padding;
            }

            int textY = y + padding;
            if (lines.size() * client.font.lineHeight < iconSize) {
                textY = y + (boxHeight / 2) - (lines.size() * client.font.lineHeight / 2);
            }

            for (FormattedCharSequence line : lines) {
                context.drawString(client.font, line, textX, textY, 0xFFFFFFFF);
                textY += client.font.lineHeight;
            }

            // Draw progress bar at the bottom
            int progressWidth = (int) (boxWidth * (1.0f - notification.getProgress()));
            context.fill(x, y + boxHeight - 2, x + progressWidth, y + boxHeight, 0xFF55FF55);

            currentY -= (boxHeight + 5);
        }
    }

    @Override
    public void tick() {
        NotificationManager.tick();
    }

    @Override
    public boolean hasBackgroundSetting() { return false; }
    @Override
    public boolean hasShowBackgroundSetting() { return false; }
    @Override
    public boolean hasBorderSetting() { return false; }
    @Override
    public boolean hasColorSetting() { return false; }
    @Override
    public boolean hasTextShadowSetting() { return false; }
    @Override
    public boolean hasScaleSetting() { return false; }
}
