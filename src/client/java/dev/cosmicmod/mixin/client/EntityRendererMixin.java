package dev.cosmicmod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.cosmicmod.client.render.DamageIndicatorManager;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.properties.numeric.Damage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends net.minecraft.world.entity.Entity, S extends EntityRenderState> {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(S state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {

        if (!DamageIndicatorManager.isEnabled()) return;

        net.minecraft.network.chat.Component nameComponent = ((EntityRenderState) state).nameTag;
        if (nameComponent == null) return;
        String name = nameComponent.getString();
        // Pattern matches §c- followed by a number, e.g., §c-0.60, §c-5.5, §c-10
        if (name.startsWith("§c-") && name.substring(3).matches("[0-9.]+")) {
            ci.cancel();
        }

    }
}
