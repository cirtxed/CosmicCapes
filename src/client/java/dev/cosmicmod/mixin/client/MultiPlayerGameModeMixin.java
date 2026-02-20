package dev.cosmicmod.mixin.client;

import dev.cosmicmod.client.CosmicConfig;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Inject(method = "stopDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onStopDestroyBlock(CallbackInfo ci) {
        if (CosmicConfig.getInstance().allowMiningWhileRightClicking) {
            // Check the stack trace or a flag to see if we are in useItem
            // For now, let's try just canceling it if the setting is on.
            // This might have side effects like not stopping mining when we should.
            // But usually, you WANT to stop mining if you let go of the left click.
            // If we are holding both, we don't want it to stop.
            if (net.minecraft.client.Minecraft.getInstance().options.keyAttack.isDown()) {
                ci.cancel();
            }
        }
    }
}
