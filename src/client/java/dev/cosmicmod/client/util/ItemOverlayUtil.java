package dev.cosmicmod.client.util;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;

public class ItemOverlayUtil {

    /**
     * Draws a semi-transparent highlight over a slot.
     */
    public static void drawHighlight(GuiGraphics context, Slot slot, int xOffset, int yOffset, int color) {
        context.pose().pushPose();
        // Z=250 ensures it's above items but below text (350)
        context.pose().translate(0, 0, 250);
        context.fill(xOffset + slot.x, yOffset + slot.y, xOffset + slot.x + 16, yOffset + slot.y + 16, color);
        context.pose().popPose();
    }

    /**
     * Draws text over a slot with a default Z-index (350).
     */
    public static void drawText(GuiGraphics context, Font font, String text, Slot slot, int xOffset, int yOffset, int color, float customScale) {
        drawTextAt(context, font, text, xOffset + slot.x, yOffset + slot.y, color, 350.0f, customScale);
    }

    /**
     * Draws text at specific coordinates with a default Z-index (350).
     */
    public static void drawTextAt(GuiGraphics context, Font font, String text, int x, int y, int color, float customScale) {
        drawTextAt(context, font, text, x, y, color, 350.0f, customScale);
    }

    /**
     * Draws text at specific coordinates with a specified Z-index.
     */
    public static void drawTextAt(GuiGraphics context, Font font, String text, int x, int y, int color, float z, float customScale) {
        context.pose().pushPose();
        // Translate to a Z level that is on top of items.
        // Hotbar usually needs higher (800), Inventory needs lower (350) to stay behind tooltips.
        context.pose().translate(0, 0, z);
        
        float baseScale = 0.5f * customScale;
        int textWidth = font.width(text);
        float scale = baseScale;
        
        // If text at baseScale is wider than 16 pixels, shrink it to fit
        if (textWidth * baseScale > 16) {
            scale = 16.0f / textWidth;
        }
        
        context.pose().scale(scale, scale, 1.0f);
        
        // At calculated scale, we want it to be roughly in the same relative position (bottom right)
        // x + 16 is the right edge.
        int textX = (int)((x + 16) / scale) - textWidth;
        int textY = (int)((y + 16) / scale) - font.lineHeight;
        
        // Use drawString with shadow to make it more readable
        context.drawString(font, text, textX, textY, color, true);
        context.pose().popPose();
    }

    /**
     * Draws centered text over a slot.
     */
    public static void drawCenteredText(GuiGraphics context, Font font, String text, Slot slot, int xOffset, int yOffset, int color) {
        drawCenteredTextAt(context, font, text, xOffset + slot.x, yOffset + slot.y, color);
    }

    /**
     * Draws centered text at specific coordinates.
     */
    public static void drawCenteredTextAt(GuiGraphics context, Font font, String text, int x, int y, int color) {
        context.pose().pushPose();
        context.pose().translate(0, 0, 210);
        
        float scale = 0.5f;
        context.pose().scale(scale, scale, 1.0f);
        
        int textX = (int)((x + 8) / scale) - (font.width(text) / 2);
        int textY = (int)((y + 8) / scale) - (font.lineHeight / 2);
        
        context.drawString(font, text, textX, textY, color, true);
        context.pose().popPose();
    }

    /**
     * Draws a cooldown overlay similar to Minecraft's native one.
     */
    public static void drawCooldown(GuiGraphics context, int x, int y, float progress, float z) {
        if (progress <= 0.0f || progress > 1.0f) return;

        int height = (int) (progress * 16.0f);
        int top = y + 16 - height;
        
        context.pose().pushPose();
        context.pose().translate(0, 0, z); // Customizable Z-level
        // Use a slightly offset rectangle to match native look if needed, 
        // but here we just use the provided x, y as the top-left of the 16x16 area.
        context.fill(x, top, x + 16, top + height, 0x7FFFFFFF); // Semi-transparent white
        context.pose().popPose();
    }

    /**
     * Draws a circular cooldown progress.
     */
    public static void drawCircularCooldown(GuiGraphics context, float centerX, float centerY, float radius, float progress, int color, float z) {
        if (progress <= 0.0f) return;

        com.mojang.blaze3d.vertex.Tesselator tesselator = com.mojang.blaze3d.vertex.Tesselator.getInstance();
        com.mojang.blaze3d.vertex.BufferBuilder buffer = tesselator.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.TRIANGLE_FAN, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR);

        context.pose().pushPose();
        context.pose().translate(0, 0, z);
        org.joml.Matrix4f matrix = context.pose().last().pose();

        float r = (color >> 16 & 255) / 255.0f;
        float g = (color >> 8 & 255) / 255.0f;
        float b = (color & 255) / 255.0f;
        float a = (color >> 24 & 255) / 255.0f;

        buffer.addVertex(matrix, centerX, centerY, 0).setColor(r, g, b, a);

        int segments = 32;
        float angleLimit = progress * 360.0f;
        for (int i = 0; i <= segments; i++) {
            float angle = (i / (float) segments) * angleLimit - 90.0f;
            float radians = (float) Math.toRadians(angle);
            float vx = centerX + (float) Math.cos(radians) * radius;
            float vy = centerY + (float) Math.sin(radians) * radius;
            buffer.addVertex(matrix, vx, vy, 0).setColor(r, g, b, a);
        }

        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(buffer.buildOrThrow());
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();

        context.pose().popPose();
    }

    public static void drawOutline(GuiGraphics guiGraphics, int x, int y, int w, int h, int color) {
        guiGraphics.fill(x + 1, y, x + w - 1, y + 1, color); // Top
        guiGraphics.fill(x + 1, y + h - 1, x + w - 1, y + h, color); // Bottom
        guiGraphics.fill(x, y + 1, x + 1, y + h - 1, color); // Left
        guiGraphics.fill(x + w - 1, y + 1, x + w, y + h - 1, color); // Right
    }
}
