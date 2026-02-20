package dev.cosmicmod.client.hud;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.cosmicmod.client.feature.FeatureMenu;
import dev.cosmicmod.client.feature.ServerLock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;

public class ClueScrollHud extends HudModule {
    private static final Gson GSON = new Gson();
    private final List<ClueScrollData> activeClueScrolls = new ArrayList<>();

    public ClueScrollHud() {
        super("Clue Scroll Hud", "Displays active clue scroll quests in your inventory.", true, 5, 100, FeatureMenu.MODS, ServerLock.NONE);
    }

    @Override
    public void tick() {
        activeClueScrolls.clear();
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
                    if (bukkitValues.contains("cosmicprisons:clue_scroll_data")) {
                        String jsonData = bukkitValues.getString("cosmicprisons:clue_scroll_data");
                        try {
                            ClueScrollData data = parseClueScrollData(jsonData);
                            if (data != null && !data.isCompleted()) {
                                activeClueScrolls.add(data);
                            }
                        } catch (Exception e) {
                            // Ignore malformed data
                        }
                    }
                }
            }
        }
    }

    private ClueScrollData parseClueScrollData(String json) {
        JsonObject obj = GSON.fromJson(json, JsonObject.class);
        String tier = obj.has("tier") ? obj.get("tier").getAsString() : "UNKNOWN";
        int currentIndex = obj.has("currentClueIndex") ? obj.get("currentClueIndex").getAsInt() : 0;
        JsonArray cluesArray = obj.getAsJsonArray("clues");
        
        List<Clue> clues = new ArrayList<>();
        if (cluesArray != null) {
            for (JsonElement el : cluesArray) {
                JsonObject clueObj = el.getAsJsonObject();
                clues.add(new Clue(
                    clueObj.get("type").getAsString(),
                    clueObj.get("progress").getAsInt(),
                    clueObj.get("target").getAsInt(),
                    clueObj.get("completed").getAsBoolean()
                ));
            }
        }
        return new ClueScrollData(tier, clues, currentIndex);
    }

    @Override
    public void render(GuiGraphics context, float partialTicks) {
        // Check if there are any active clues to show across all scrolls
        boolean hasAnyActiveClues = false;
        for (ClueScrollData scroll : activeClueScrolls) {
            for (int i = 0; i < scroll.clues.size(); i++) {
                Clue clue = scroll.clues.get(i);
                if (!clue.completed && i >= scroll.currentIndex) {
                    hasAnyActiveClues = true;
                    break;
                }
            }
            if (hasAnyActiveClues) break;
        }

        if (!hasAnyActiveClues) {
            this.width = 0;
            this.height = 0;
            return;
        }

        Minecraft client = Minecraft.getInstance();
        float scale = getSettings().scale;
        int formattingColor = getSettings().formattingColor;
        boolean shadow = getSettings().textShadow;
        boolean rightAligned = isRightAligned();

        context.pose().pushPose();
        context.pose().translate(getX(), getY(), 0);
        context.pose().scale(scale, scale, 1.0f);

        // First pass: calculate maxWidth and yOffset
        int yOffsetCalc = 2;
        int maxWidthCalc = 100;

        String customLabel = getSettings().label;
        String title = (customLabel != null && !customLabel.isEmpty()) ? customLabel.replace("&", "§") : "§6§lClue Scroll";
        
        if (getSettings().showLabel) {
            maxWidthCalc = Math.max(maxWidthCalc, client.font.width(title) + 4);
            yOffsetCalc += 10;
        }

        for (ClueScrollData scroll : activeClueScrolls) {
            for (int i = 0; i < scroll.clues.size(); i++) {
                Clue clue = scroll.clues.get(i);
                if (clue.completed || i < scroll.currentIndex) continue;

                String prefix = (i == scroll.currentIndex ? "§e* " : "§7- ");
                String color = (i == scroll.currentIndex ? "§f" : "§7");
                String clueText = prefix + color + formatClueType(clue.type) + " (" + clue.progress + "/" + clue.target + ")";
                
                maxWidthCalc = Math.max(maxWidthCalc, client.font.width(clueText) + 8);
                yOffsetCalc += 10;
            }
        }

        // Render Background
        renderBackground(context, 0, 0, maxWidthCalc, yOffsetCalc);

        int yOffset = 2;
        if (getSettings().showLabel) {
            int titleWidth = client.font.width(title);
            int titleX = rightAligned ? maxWidthCalc - titleWidth - 2 : 2;
            context.drawString(client.font, title, titleX, yOffset, 0xFFFFFF, shadow);
            yOffset += 10;
        }

        for (ClueScrollData scroll : activeClueScrolls) {
            for (int i = 0; i < scroll.clues.size(); i++) {
                Clue clue = scroll.clues.get(i);
                if (clue.completed || i < scroll.currentIndex) continue;

                String prefix = (i == scroll.currentIndex ? "§e* " : "§7- ");
                String color = (i == scroll.currentIndex ? "§f" : "§7");
                String clueType = formatClueType(clue.type);
                String progress = " (" + clue.progress + "/" + clue.target + ")";
                
                String fullText;
                if (rightAligned) {
                    prefix = (i == scroll.currentIndex ? " *§e" : " -§7");
                    fullText = color + clueType + progress + prefix;
                } else {
                    fullText = prefix + color + clueType + progress;
                }
                
                int textWidth = client.font.width(fullText);
                int textX = rightAligned ? maxWidthCalc - textWidth - 6 : 6;
                context.drawString(client.font, fullText, textX, yOffset, 0xFFFFFF, shadow);
                yOffset += 10;
            }
        }

        this.width = maxWidthCalc * scale;
        this.height = yOffset * scale;

        context.pose().popPose();
    }

    private String formatClueType(String type) {
        String formatted = type.replace("_", " ").toLowerCase();
        return formatted.substring(0, 1).toUpperCase() + formatted.substring(1);
    }

    private static class ClueScrollData {
        final String tier;
        final List<Clue> clues;
        final int currentIndex;

        ClueScrollData(String tier, List<Clue> clues, int currentIndex) {
            this.tier = tier;
            this.clues = clues;
            this.currentIndex = currentIndex;
        }

        boolean isCompleted() {
            return currentIndex >= clues.size() || (currentIndex == clues.size() - 1 && clues.get(currentIndex).completed);
        }
    }

    private static class Clue {
        final String type;
        final int progress;
        final int target;
        final boolean completed;

        Clue(String type, int progress, int target, boolean completed) {
            this.type = type;
            this.progress = progress;
            this.target = target;
            this.completed = completed;
        }
    }
}
