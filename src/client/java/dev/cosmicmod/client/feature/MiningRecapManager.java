package dev.cosmicmod.client.feature;

import dev.cosmicmod.client.notification.Notification;
import dev.cosmicmod.client.notification.NotificationManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;

public class MiningRecapManager {
    private static final long RECAP_INTERVAL = 60000; // 1 minute
    private static long lastRecapTime = 0;
    private static double energyAtStartOfMinute = -1;
    private static double totalEnergyGainedThisMinute = 0;
    private static double lastSeenEnergy = -1;
    private static String lastPickaxeId = null;
    
    private static final List<Double> hourlyHistory = new ArrayList<>();

    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        ItemStack heldItem = client.player.getMainHandItem();
        if (heldItem.isEmpty() || !isPickaxe(heldItem)) {
            // Reset if not holding a pickaxe
            lastSeenEnergy = -1;
            energyAtStartOfMinute = -1;
            lastPickaxeId = null;
            return;
        }

        double currentEnergy = getCosmicEnergy(heldItem);
        String currentPickaxeId = getPickaxeUniqueId(heldItem);

        // If switched pickaxe, reset tracking
        if (lastPickaxeId != null && !lastPickaxeId.equals(currentPickaxeId)) {
            lastSeenEnergy = -1;
            energyAtStartOfMinute = -1;
        }
        lastPickaxeId = currentPickaxeId;

        if (lastSeenEnergy == -1) {
            lastSeenEnergy = currentEnergy;
            energyAtStartOfMinute = currentEnergy;
            lastRecapTime = System.currentTimeMillis();
            return;
        }

        // If energy changed, the player is mining
        if (currentEnergy != lastSeenEnergy) {
            if (currentEnergy > lastSeenEnergy) {
                totalEnergyGainedThisMinute += (currentEnergy - lastSeenEnergy);
            }
            lastSeenEnergy = currentEnergy;
        }

        long now = System.currentTimeMillis();
        if (now - lastRecapTime >= RECAP_INTERVAL) {
            if (totalEnergyGainedThisMinute > 0) {
                sendRecapNotification(heldItem, totalEnergyGainedThisMinute);
                
                // Track for hourly rate
                hourlyHistory.add(totalEnergyGainedThisMinute);
                if (hourlyHistory.size() > 60) {
                    hourlyHistory.remove(0);
                }
            }
            
            // Reset for next minute
            totalEnergyGainedThisMinute = 0;
            energyAtStartOfMinute = currentEnergy;
            lastRecapTime = now;
        }
    }

    private static boolean isPickaxe(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("PublicBukkitValues")) {
                CompoundTag bukkitValues = tag.getCompound("PublicBukkitValues");
                if (bukkitValues.contains("cosmicprisons:custom_item_id")) {
                    return bukkitValues.getString("cosmicprisons:custom_item_id").contains("pickaxe");
                }
            }
        }
        return stack.getItem() == Items.DIAMOND_PICKAXE || stack.getItem() == Items.GOLDEN_PICKAXE || 
               stack.getItem() == Items.IRON_PICKAXE || stack.getItem() == Items.STONE_PICKAXE || 
               stack.getItem() == Items.NETHERITE_PICKAXE;
    }

    private static double getCosmicEnergy(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("PublicBukkitValues")) {
                CompoundTag bukkitValues = tag.getCompound("PublicBukkitValues");
                if (bukkitValues.contains("cosmicprisons:cosmic_energy")) {
                    return bukkitValues.getDouble("cosmicprisons:cosmic_energy");
                }
            }
        }
        return 0;
    }

    private static String getPickaxeUniqueId(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("PublicBukkitValues")) {
                CompoundTag bukkitValues = tag.getCompound("PublicBukkitValues");
                if (bukkitValues.contains("cosmicprisons:custom_item_uuid")) {
                    return bukkitValues.getString("cosmicprisons:custom_item_uuid");
                }
            }
        }
        return null;
    }

    private static void sendRecapNotification(ItemStack icon, double gained) {
        double avgMinute = hourlyHistory.stream().mapToDouble(Double::doubleValue).average().orElse(gained);
        double hourlyRate = avgMinute * 60;
        
        String text = String.format("§b§lMINING RECAP\n§fLast Minute: §a+%,.1f NRG\n§fEst. Hourly: §e%,.0f NRG/hr", gained, hourlyRate);
        
        NotificationManager.addNotification(new Notification(text, icon.copy(), 8000));
    }
}
