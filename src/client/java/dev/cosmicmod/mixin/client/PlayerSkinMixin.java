package dev.cosmicmod.mixin.client;

import dev.cosmicmod.client.CosmeticManager;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public class PlayerSkinMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void onGetSkin(CallbackInfoReturnable<PlayerSkin> cir) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        ResourceLocation customCape = CosmeticManager.getCape(player.getUUID());
        
        if (customCape != null) {
            PlayerSkin original = cir.getReturnValue();
            PlayerSkin customSkin = new PlayerSkin(
                    original.texture(),
                    original.textureUrl(),
                    customCape,
                    original.elytraTexture(),
                    original.model(),
                    original.secure()
            );
            cir.setReturnValue(customSkin);
        }
    }
}
