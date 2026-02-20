package dev.cosmicmod.mixin.client;

import dev.cosmicmod.client.render.layer.DiamondChainLayer;
import dev.cosmicmod.client.render.layer.OrbitalOrbsLayer;
import dev.cosmicmod.client.util.PlayerUUIDHolder;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(targets = "net.minecraft.client.renderer.entity.player.PlayerRenderer")
public abstract class PlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerRenderState, PlayerModel> {

    public PlayerRendererMixin(EntityRendererProvider.Context context, PlayerModel model, float shadowRadius) {
        super(context, model, shadowRadius);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(EntityRendererProvider.Context context, boolean useSlimModel, CallbackInfo ci) {
        this.addLayer(new OrbitalOrbsLayer(this));
        this.addLayer(new DiamondChainLayer(this));
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;F)V", at = @At("TAIL"))
    private void onExtractRenderState(AbstractClientPlayer player, PlayerRenderState state, float partialTick, CallbackInfo ci) {
        UUID uuid = player.getUUID();
        PlayerUUIDHolder holder = (PlayerUUIDHolder) state;
        holder.cosmicmod$setUUID(uuid);
        holder.cosmicmod$setLocalPlayer(player == net.minecraft.client.Minecraft.getInstance().player);
    }
}
