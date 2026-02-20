package dev.cosmicmod.mixin.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends net.minecraft.world.entity.LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {

    @Shadow protected abstract ResourceLocation getTextureLocation(S state);
}
