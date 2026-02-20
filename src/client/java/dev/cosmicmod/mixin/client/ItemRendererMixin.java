package dev.cosmicmod.mixin.client;

import dev.cosmicmod.client.CosmicConfig;
import dev.cosmicmod.mixin.client.access.ItemStackRenderStateAccessor;
import dev.cosmicmod.mixin.client.access.LayerRenderStateAccessor;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemModelResolver.class)
public abstract class ItemRendererMixin {

    @Inject(method = "updateForTopItem", at = @At("TAIL"))
    private void onUpdateForTopItem(ItemStackRenderState state, ItemStack stack, ItemDisplayContext context, boolean leftHand, net.minecraft.world.level.Level level, net.minecraft.world.entity.LivingEntity entity, int seed, CallbackInfo ci) {
        applyCosmicTint(state, stack);
    }

    @Inject(method = "updateForLiving", at = @At("TAIL"))
    private void onUpdateForLiving(ItemStackRenderState state, ItemStack stack, ItemDisplayContext context, boolean leftHand, net.minecraft.world.entity.LivingEntity entity, CallbackInfo ci) {
        applyCosmicTint(state, stack);
    }

    @Inject(method = "updateForNonLiving", at = @At("TAIL"))
    private void onUpdateForNonLiving(ItemStackRenderState state, ItemStack stack, ItemDisplayContext context, net.minecraft.world.entity.Entity entity, CallbackInfo ci) {
        applyCosmicTint(state, stack);
    }

    @Inject(method = "appendItemLayers", at = @At("TAIL"))
    private void onAppendItemLayers(ItemStackRenderState state, ItemStack stack, ItemDisplayContext context, net.minecraft.world.level.Level level, net.minecraft.world.entity.LivingEntity entity, int seed, CallbackInfo ci) {
        applyCosmicTint(state, stack);
    }

    private void applyCosmicTint(ItemStackRenderState state, ItemStack stack) {
        if (stack.isEmpty()) return;

        CosmicConfig config = CosmicConfig.getInstance();
        String itemKey = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        Integer tint = config.itemTintOverrides.get(itemKey);

        // Check for Cosmic items
        net.minecraft.world.item.component.CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null) {
            String dataString = customData.copyTag().toString().toUpperCase();
            
            // Priority to specific tiers if multiple exist? 
            // The request says "if a item has ... anywhere in the meta data then it should have a tint"
            // We'll check from highest to lowest tier or just first match. 
            // Usually, these items only have one tier.
            
            if (dataString.contains("GODLY")) {
                tint = 0xFFFF5555;
            } else if (dataString.contains("LEGENDARY")) {
                tint = 0xFFFFAA00;
            } else if (dataString.contains("ULTIMATE")) {
                tint = 0xFFFFFF55;
            } else if (dataString.contains("ELITE")) {
                tint = 0xFF55FFFF;
            } else if (dataString.contains("UNCOMMON")) {
                tint = 0xFF55FF55;
            } else if (dataString.contains("SIMPLE")) {
                tint = 0xFFAAAAAA;
            }
            
            // Special case for money notes which might not have a tier string but we want green
            if (tint == null) {
                net.minecraft.nbt.CompoundTag tag = customData.copyTag();
                if (tag.contains("PublicBukkitValues")) {
                    net.minecraft.nbt.CompoundTag bukkit = tag.getCompound("PublicBukkitValues");
                    if ("money_note".equals(bukkit.getString("cosmicprisons:custom_item_id"))) {
                        tint = 0xFF55FF55;
                    }
                }
            }
        }

        if (tint != null) {
            // Ensure alpha is set to avoid invisibility
            int finalTint = tint | 0xFF000000;
            applyTintToAllLayers((ItemStackRenderStateAccessor) (Object) state, finalTint);
            applyGlintToAllLayers((ItemStackRenderStateAccessor) (Object) state);
        }
    }

    private void applyTintToAllLayers(ItemStackRenderStateAccessor state, int tint) {
        int count = state.getActiveLayerCount();
        ItemStackRenderState.LayerRenderState[] layers = state.getLayers();
        for (int i = 0; i < count; i++) {
            LayerRenderStateAccessor layer = (LayerRenderStateAccessor) (Object) layers[i];
            int[] tintLayers = layer.getTintLayers();
            
            // If the layer already has tint layers, override them all
            if (tintLayers != null && tintLayers.length > 0) {
                for (int j = 0; j < tintLayers.length; j++) {
                    tintLayers[j] = tint;
                }
            } else {
                // Otherwise, force at least one tint layer with our color
                tintLayers = layer.callPrepareTintLayers(1);
                tintLayers[0] = tint;
                layer.setTintLayers(tintLayers);
            }
        }
    }

    private void applyGlintToAllLayers(ItemStackRenderStateAccessor state) {
        int count = state.getActiveLayerCount();
        ItemStackRenderState.LayerRenderState[] layers = state.getLayers();
        for (int i = 0; i < count; i++) {
            LayerRenderStateAccessor layer = (LayerRenderStateAccessor) (Object) layers[i];
            layer.setFoilType(ItemStackRenderState.FoilType.STANDARD);
        }
    }
}
