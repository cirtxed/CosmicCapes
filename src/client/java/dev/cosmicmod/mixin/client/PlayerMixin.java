package dev.cosmicmod.mixin.client;

import dev.cosmicmod.client.CosmicmodClient;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.cosmicmod.client.render.DamageIndicatorManager;
import net.minecraft.world.entity.LivingEntity;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "attack", at = @At("HEAD"))
    private void onAttack(Entity target, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (player.level().isClientSide) {
            // Check if this is the local player
            if (player.getUUID().equals(net.minecraft.client.Minecraft.getInstance().player.getUUID())) {
                CosmicmodClient.getCooldownHud().startCooldown("Combat", 10000);
            }
        }
    }
}