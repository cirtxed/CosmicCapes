package dev.cosmicmod.client;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.Map;

public class ItemTextureManager {
    
    public static boolean isPossible() {
        return true;
    }
    
    public static ResourceLocation getOverrideTexture(ItemStack stack) {
        if (stack.isEmpty()) return null;
        
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            ResourceLocation playerOverride = CosmeticManager.getItemOverride(client.player.getUUID(), stack.getHoverName().getString());
            if (playerOverride != null) return playerOverride;
        }

        CosmicConfig config = CosmicConfig.getInstance();
        
        if (config.itemTextureOverrides.isEmpty()) return null;
        
        String itemName = stack.getHoverName().getString();
        
        for (Map.Entry<String, String> entry : config.itemTextureOverrides.entrySet()) {
            String match = entry.getKey();
            if (itemName.equals(match) || itemName.contains(match)) {
                try {
                    return ResourceLocation.parse(entry.getValue());
                } catch (Exception e) {
                    // Invalid ResourceLocation
                }
            }
        }
        
        return null;
    }

    public static Object getEquippableVariant(ItemStack stack, DataComponentType<?> type, com.llamalad7.mixinextras.injector.wrapoperation.Operation<?> originalOp) {
        return originalOp.call(stack, type);
    }
}
