package dev.cosmicmod.mixin.client.access;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public interface LayerRenderStateAccessor {
    @Accessor("tintLayers")
    int[] getTintLayers();

    @Accessor("tintLayers")
    void setTintLayers(int[] tintLayers);

    @Accessor("foilType")
    void setFoilType(ItemStackRenderState.FoilType foilType);

    @Invoker("prepareTintLayers")
    int[] callPrepareTintLayers(int count);
}
