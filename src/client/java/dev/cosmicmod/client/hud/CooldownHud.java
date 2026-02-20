package dev.cosmicmod.client.hud;

import dev.cosmicmod.client.ChatStateManager;
import dev.cosmicmod.client.CosmicConfig;
import dev.cosmicmod.client.feature.FeatureMenu;
import dev.cosmicmod.client.feature.ServerLock;
import dev.cosmicmod.client.util.ItemOverlayUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownHud extends HudModule {
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<String, Long> startTimes = new ConcurrentHashMap<>();
    private String lastServer = "";

    public CooldownHud() {
        super("Cooldown Hud", "Displays active cooldowns for pearls, commands, and more.", true, 5, 50, FeatureMenu.MODS, ServerLock.NONE);
    }

    public boolean isOnCooldown(String name) {
        return cooldowns.containsKey(name) && cooldowns.get(name) > System.currentTimeMillis();
    }

    public float getCooldownProgress(String name) {
        if (!cooldowns.containsKey(name)) return 0.0f;
        long end = cooldowns.get(name);
        long start = startTimes.getOrDefault(name, end - 1000);
        long now = System.currentTimeMillis();
        if (now >= end) return 0.0f;
        return (float) (end - now) / (float) (end - start);
    }

    public void removeCooldown(String name) {
        cooldowns.remove(name);
        startTimes.remove(name);
        saveToConfig();
    }

    public void startCooldown(String name, long durationMs) {
        long now = System.currentTimeMillis();
        cooldowns.put(name, now + durationMs);
        startTimes.put(name, now);
        saveToConfig();
    }

    private void saveToConfig() {
        String currentServer = ChatStateManager.getCurrentServer();
        if (currentServer == null || currentServer.isEmpty()) return;

        CosmicConfig config = CosmicConfig.getInstance();
        Map<String, CosmicConfig.CooldownData> dataMap = new HashMap<>();
        
        for (Map.Entry<String, Long> entry : cooldowns.entrySet()) {
            dataMap.put(entry.getKey(), new CosmicConfig.CooldownData(startTimes.getOrDefault(entry.getKey(), 0L), entry.getValue()));
        }

        config.serverCooldowns.put(currentServer, dataMap);
        CosmicConfig.save();
    }

    private void loadFromConfig() {
        String currentServer = ChatStateManager.getCurrentServer();
        if (currentServer == null || currentServer.isEmpty()) return;

        CosmicConfig config = CosmicConfig.getInstance();
        Map<String, CosmicConfig.CooldownData> dataMap = config.serverCooldowns.get(currentServer);
        
        cooldowns.clear();
        startTimes.clear();

        if (dataMap != null) {
            long now = System.currentTimeMillis();
            for (Map.Entry<String, CosmicConfig.CooldownData> entry : dataMap.entrySet()) {
                if (entry.getValue().endTime > now) {
                    cooldowns.put(entry.getKey(), entry.getValue().endTime);
                    startTimes.put(entry.getKey(), entry.getValue().startTime);
                }
            }
        }
        lastServer = currentServer;
    }

    @Override
    public void tick() {
        String currentServer = ChatStateManager.getCurrentServer();
        if (!currentServer.equals(lastServer)) {
            loadFromConfig();
        }

        long now = System.currentTimeMillis();
        boolean changed = cooldowns.entrySet().removeIf(entry -> {
            boolean expired = now > entry.getValue();
            if (expired) {
                startTimes.remove(entry.getKey());
            }
            return expired;
        });

        if (changed) {
            saveToConfig();
        }
    }

    @Override
    public void render(GuiGraphics context, float partialTicks) {
        Minecraft client = Minecraft.getInstance();
        if (cooldowns.isEmpty()) {
            return;
        }

        int xOffset = 0;
        int maxHeight = 0;
        long now = System.currentTimeMillis();
        
        List<Map.Entry<String, Long>> entries = new java.util.ArrayList<>(cooldowns.entrySet());
        
        for (Map.Entry<String, Long> entry : entries) {
            String name = entry.getKey();
            long endTime = entry.getValue();
            long startTime = startTimes.getOrDefault(name, now - 1000);
            long total = endTime - startTime;
            long remaining = endTime - now;
            
            if (remaining <= 0) continue;

            float progress = (float) remaining / (float) total;
            
            int x = (int) getX() + xOffset;
            int y = (int) getY();

            // Render Item Icon (Centered horizontally relative to text)
            // We assume a column width of 40 for centering
            int columnWidth = 40;
            int totalHeight = 40;
            
            ItemStack stack = new ItemStack(getItemForName(name));
            float scale = getSettings().scale;
            int formattingColor = getSettings().formattingColor;
            
            context.pose().pushPose();
            context.pose().translate(x, y, 0);
            context.pose().scale(scale, scale, 1.0f);
            
            renderBackground(context, 0, 0, columnWidth, totalHeight);
            
            // Re-adjust item render pos because we translated
            int itemX = (columnWidth / 2) - 8;
            context.renderItem(stack, itemX, 2);
            
            // Render Circular Cooldown over it
            ItemOverlayUtil.drawCircularCooldown(context, itemX + 8, 10, 8, progress, 0x80FFFFFF, 200);

            // Line 2: The command or message
            int nameWidth = client.font.width(name);
            context.drawString(client.font, name, (columnWidth / 2) - nameWidth / 2, 20, formattingColor, getSettings().textShadow);

            // Line 3: The time
            String timeText = String.format("%.1f", remaining / 1000.0) + "s";
            int timeWidth = client.font.width(timeText);
            context.drawString(client.font, timeText, (columnWidth / 2) - timeWidth / 2, 30, 0xFFFFFF, getSettings().textShadow);

            context.pose().popPose();

            xOffset += (int)((columnWidth + 8) * scale);
            maxHeight = Math.max(maxHeight, (int)(40 * scale));
        }
        this.width = xOffset;
        this.height = maxHeight;
    }

    @Override
    public boolean hasFormattingColorSetting() {
        return true;
    }

    @Override
    public boolean hasScaleSetting() {
        return true;
    }

    @Override
    public boolean hasShowBackgroundSetting() {
        return true;
    }

    @Override
    public boolean hasBackgroundSetting() {
        return true;
    }

    @Override
    public boolean hasBorderSetting() {
        return true;
    }

    @Override
    public boolean hasTextShadowSetting() {
        return true;
    }

    @Override
    public boolean hasReverseOrderSetting() {
        return false;
    }

    private net.minecraft.world.item.Item getItemForName(String name) {
        String lower = name.toLowerCase();
        
        return switch (lower) {
            case String s when s.contains("strength") -> net.minecraft.world.item.Items.BLAZE_POWDER;
            case String s when s.contains("speed") -> net.minecraft.world.item.Items.SUGAR;
            case String s when s.contains("resistance") -> net.minecraft.world.item.Items.IRON_INGOT;
            case String s when s.contains("regeneration") -> net.minecraft.world.item.Items.GHAST_TEAR;
            case String s when s.contains("food") || s.contains("feed") || s.contains("steak") || s.contains("saturation") || s.contains("hunger") -> net.minecraft.world.item.Items.COOKED_BEEF;
            case String s when s.contains("haste") -> net.minecraft.world.item.Items.GOLDEN_PICKAXE;
            case String s when s.contains("fire resistance") || s.contains("fire_resistance") -> net.minecraft.world.item.Items.MAGMA_CREAM;
            case String s when s.contains("night vision") || s.contains("night_vision") -> net.minecraft.world.item.Items.GOLDEN_CARROT;
            case String s when s.contains("jump boost") || s.contains("jump_boost") -> net.minecraft.world.item.Items.RABBIT_FOOT;
            case String s when s.contains("invisibility") -> net.minecraft.world.item.Items.FERMENTED_SPIDER_EYE;
            case String s when s.contains("water breathing") || s.contains("water_breathing") -> net.minecraft.world.item.Items.PUFFERFISH;
            case String s when s.contains("fix all") || s.contains("fixall") -> net.minecraft.world.item.Items.NETHERITE_INGOT;
            case String s when s.contains("fix") -> net.minecraft.world.item.Items.ANVIL;
            case String s when s.contains("heal") -> net.minecraft.world.item.Items.GLISTERING_MELON_SLICE;
            case String s when s.contains("pearl") -> net.minecraft.world.item.Items.ENDER_PEARL;
            case String s when s.contains("jet") -> net.minecraft.world.item.Items.FIREWORK_ROCKET;
            case String s when s.contains("tpa") -> net.minecraft.world.item.Items.BEACON;
            case String s when s.contains("mule") -> net.minecraft.world.item.Items.CHEST;
            case String s when s.contains("combat") -> net.minecraft.world.item.Items.DIAMOND_SWORD;
            case String s when s.contains("near") -> net.minecraft.world.item.Items.COMPASS;
            case String s when s.contains("powerball") -> net.minecraft.world.item.Items.NETHERITE_PICKAXE;
            default -> net.minecraft.world.item.Items.CLOCK;
        };
    }
}
