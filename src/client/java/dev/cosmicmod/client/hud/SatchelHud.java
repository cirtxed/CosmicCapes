package dev.cosmicmod.client.hud;

import dev.cosmicmod.client.feature.FeatureMenu;
import dev.cosmicmod.client.feature.ServerLock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SatchelHud extends HudModule {
    private final Map<String, SatchelData> aggregatedSatchels = new HashMap<>();

    public SatchelHud() {
        super("Satchel Hud", "Displays all satchels in your inventory and their total counts.", true, 5, 150, FeatureMenu.MODS, ServerLock.NONE);
    }

    @Override
    public void tick() {
        aggregatedSatchels.clear();
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        for (int i = 0; i < client.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                CompoundTag tag = customData.copyTag();
                if (tag.contains("PublicBukkitValues")) {
                    CompoundTag bukkitValues = tag.getCompound("PublicBukkitValues");
                    if (bukkitValues.contains("cosmicprisons:custom_item_id") && 
                        bukkitValues.getString("cosmicprisons:custom_item_id").endsWith("_satchel")) {
                        
                        String oreType = bukkitValues.contains("cosmicprisons:satchel_ore") ? 
                                         bukkitValues.getString("cosmicprisons:satchel_ore") : "UNKNOWN";
                        boolean isRefined = bukkitValues.contains("cosmicprisons:satchel_refined") && 
                                           bukkitValues.getInt("cosmicprisons:satchel_refined") == 1;
                        
                        String key = oreType + (isRefined ? "_refined" : "");

                        int count = bukkitValues.contains("cosmicprisons:satchel_count") ? 
                                    bukkitValues.getInt("cosmicprisons:satchel_count") : 0;
                        int capacity = 0;
                        
                        // Parse capacity from lore if possible
                        if (stack.has(DataComponents.LORE)) {
                            net.minecraft.world.item.component.ItemLore lore = stack.get(DataComponents.LORE);
                            if (lore != null) {
                                for (net.minecraft.network.chat.Component line : lore.lines()) {
                                    String lineStr = line.getString();
                                    if (lineStr.contains("/") && lineStr.contains(",")) {
                                        // Potential capacity line: "5,510 / 13,824"
                                        String[] parts = lineStr.split("/");
                                        if (parts.length == 2) {
                                            try {
                                                String capPart = parts[1].trim().replace(",", "");
                                                // Handle potential extra text like ")"
                                                capPart = capPart.replaceAll("[^0-9]", "");
                                                capacity = Integer.parseInt(capPart);
                                                break;
                                            } catch (NumberFormatException ignored) {}
                                        }
                                    }
                                }
                            }
                        }
                        
                        final int finalCapacity = capacity;
                        aggregatedSatchels.compute(key, (k, v) -> {
                            if (v == null) {
                                return new SatchelData(oreType, isRefined, count, finalCapacity, stack.copy());
                            } else {
                                v.count += count;
                                v.capacity += finalCapacity;
                                return v;
                            }
                        });
                    }
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics context, float partialTicks) {
        if (aggregatedSatchels.isEmpty()) {
            this.width = 0;
            this.height = 0;
            return;
        }

        Minecraft client = Minecraft.getInstance();
        float scale = getSettings().scale;
        boolean shadow = getSettings().textShadow;
        boolean hideNames = getSettings().hideSatchelNames;
        int displayMode = getSettings().satchelDisplayMode;
        boolean dynamicColor = getSettings().dynamicCapacityColor;
        boolean rightAligned = isRightAligned();

        context.pose().pushPose();
        context.pose().translate(getX(), getY(), 0);
        context.pose().scale(scale, scale, 1.0f);

        int yOffset = 2;
        int maxWidth = 100;

        // First pass: calculate maxWidth and yOffset for background
        int calcY = 14;
        List<SatchelData> sortedSatchels = new ArrayList<>(aggregatedSatchels.values());
        sortedSatchels.sort((a, b) -> b.count - a.count);

        for (SatchelData data : sortedSatchels) {
            String displayText = getDisplayText(data, hideNames, displayMode);
            String fullLine = "      " + displayText;
            maxWidth = Math.max(maxWidth, client.font.width(fullLine) + 10);
            calcY += 16;
        }
        
        if (!getSettings().showLabel) {
            calcY -= 12;
        }

        renderBackground(context, 0, 0, maxWidth, calcY);
        
        if (getSettings().showLabel) {
            String customLabel = getSettings().label;
            String title = (customLabel != null && !customLabel.isEmpty()) ? customLabel.replace("&", "§") : "§c§lSatchels";
            int titleWidth = client.font.width(title);
            int titleX = rightAligned ? maxWidth - titleWidth - 2 : 2;
            context.drawString(client.font, title, titleX, yOffset, 0xFFFFFF, shadow);
            yOffset += 12;
        }

        for (SatchelData data : sortedSatchels) {
            String displayText = getDisplayText(data, hideNames, displayMode);
            int textWidth = client.font.width(displayText);
            
            int itemX = rightAligned ? maxWidth - 18 : 2;
            int textX = rightAligned ? maxWidth - 18 - textWidth - 4 : 2 + 18 + 2;

            // Draw item icon
            context.renderItem(data.iconStack, itemX, yOffset - 3);
            
            int color = 0xFFFFFF;
            if (dynamicColor && data.capacity > 0) {
                float percent = (float) data.count / data.capacity;
                if (percent >= 1.0f) color = 0xFF5555; // Red
                else if (percent >= 0.9f) color = 0xFFAA00; // Gold/Orange
                else if (percent >= 0.75f) color = 0xFFFF55; // Yellow
                else color = 0x55FF55; // Green
            }

            context.drawString(client.font, displayText, textX, yOffset + 2, color, shadow);
            yOffset += 16;
        }

        if (isBottomAligned() && !getSettings().reverseOrder) {
            // Suggesting reverse order might be better if bottom aligned?
            // User didn't ask for it specifically, but it's common.
        }

        this.width = maxWidth * scale;
        this.height = yOffset * scale;

        context.pose().popPose();
    }

    private String getDisplayText(SatchelData data, boolean hideNames, int displayMode) {
        StringBuilder sb = new StringBuilder();
        if (!hideNames) {
            sb.append(formatOreName(data.oreType, data.isRefined));
            sb.append(" §7: §f");
        }

        String countStr = String.format("%,d", data.count);
        String capStr = String.format("%,d", data.capacity);
        float percent = data.capacity > 0 ? ((float) data.count / data.capacity) * 100 : 0;
        String percentStr = String.format("%.1f%%", percent);

        switch (displayMode) {
            case 0: // x/capacity
                sb.append(countStr).append(" / ").append(capStr);
                break;
            case 1: // x%
                sb.append(percentStr);
                break;
            case 2: // x% (x/capacity)
                sb.append(percentStr).append(" (").append(countStr).append("/").append(capStr).append(")");
                break;
            default:
                sb.append(countStr);
                break;
        }
        return sb.toString();
    }

    private String formatOreName(String oreType, boolean isRefined) {
        String lower = oreType.toLowerCase();
        String name = switch (lower) {
            case "coal" -> "§8Coal";
            case "iron" -> "§fIron";
            case "gold" -> "§6Gold";
            case "redstone" -> "§cRedstone";
            case "lapis" -> "§9Lapis";
            case "diamond" -> "§bDiamond";
            case "emerald" -> "§aEmerald";
            default -> {
                String formatted = oreType.replace("_", " ").toLowerCase();
                yield formatted.substring(0, 1).toUpperCase() + formatted.substring(1);
            }
        };
        
        if (isRefined) {
            return name;
        } else {
            return name + " Ore";
        }
    }

    @Override
    public boolean hasSatchelSettings() {
        return true;
    }

    private static class SatchelData {
        String oreType;
        boolean isRefined;
        int count;
        int capacity;
        ItemStack iconStack;

        SatchelData(String oreType, boolean isRefined, int count, int capacity, ItemStack iconStack) {
            this.oreType = oreType;
            this.isRefined = isRefined;
            this.count = count;
            this.capacity = capacity;
            this.iconStack = iconStack;
        }
    }
}
