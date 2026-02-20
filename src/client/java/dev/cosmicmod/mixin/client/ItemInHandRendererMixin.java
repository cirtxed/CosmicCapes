package dev.cosmicmod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.cosmicmod.client.CosmicConfig;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {


    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    private void onRenderItemEntry(net.minecraft.client.player.AbstractClientPlayer abstractClientPlayer, float f, float g, net.minecraft.world.InteractionHand interactionHand, float h, net.minecraft.world.item.ItemStack itemStack, float i, PoseStack poseStack, MultiBufferSource multiBufferSource, int j, CallbackInfo ci) {
        if (dev.cosmicmod.client.CosmicmodClient.getHandRescalerHud() == null || !dev.cosmicmod.client.CosmicmodClient.getHandRescalerHud().isEnabled()) return;

        CosmicConfig config = CosmicConfig.getInstance();
        float scale = interactionHand == net.minecraft.world.InteractionHand.MAIN_HAND ? config.mainHandScale : config.offHandScale;
        if (scale != 1.0f) {
            poseStack.scale(scale, scale, scale);
        }
    }

    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderPlayerArm(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IFFLnet/minecraft/world/entity/HumanoidArm;)V"))
    private void onRenderPlayerArmEntry(net.minecraft.client.player.AbstractClientPlayer abstractClientPlayer, float f, float g, net.minecraft.world.InteractionHand interactionHand, float h, net.minecraft.world.item.ItemStack itemStack, float i, PoseStack poseStack, MultiBufferSource multiBufferSource, int j, CallbackInfo ci) {
        if (dev.cosmicmod.client.CosmicmodClient.getHandRescalerHud() == null || !dev.cosmicmod.client.CosmicmodClient.getHandRescalerHud().isEnabled()) return;

        CosmicConfig config = CosmicConfig.getInstance();
        float scale = interactionHand == net.minecraft.world.InteractionHand.MAIN_HAND ? config.mainHandScale : config.offHandScale;
        if (scale != 1.0f) {
            poseStack.scale(scale, scale, scale);
        }
    }
}
