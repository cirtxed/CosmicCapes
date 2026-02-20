package dev.cosmicmod.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(net.minecraft.client.Minecraft.class)
public abstract class MinecraftClientMixin {
    @Shadow public net.minecraft.client.multiplayer.MultiPlayerGameMode gameMode;

    @Redirect(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;isDestroying()Z"))
    private boolean allowItemUseWhileMining(net.minecraft.client.multiplayer.MultiPlayerGameMode instance) {
        return false;
    }
}
