package dev.cosmicmod.mixin.client;

import dev.cosmicmod.client.CosmicmodClient;
import dev.cosmicmod.client.render.DamageIndicatorManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow public abstract float getHealth();
    @Shadow public abstract float getMaxHealth();
    @Shadow public abstract net.minecraft.world.entity.LivingEntity getLastHurtByMob();
    @Shadow public int hurtTime;

    @Unique
    private float cosmicmod$lastHealth = -1;

    @Unique
    private static long cosmicmod$lastHitTime = 0;
    @Unique
    private static java.util.UUID cosmicmod$lastHitEntityId = null;
    @Unique
    private static boolean cosmicmod$wasCriticalHit = false;

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!entity.level().isClientSide) return;

        float currentHealth = this.getHealth();
        if (cosmicmod$lastHealth == -1) {
            cosmicmod$lastHealth = currentHealth;
            return;
        }

        if (currentHealth != cosmicmod$lastHealth) {
            float diff = cosmicmod$lastHealth - currentHealth;
            Player localPlayer = Minecraft.getInstance().player;
            
            if (localPlayer != null && entity != localPlayer) {
                long currentTime = System.currentTimeMillis();
                long timeSinceLastHit = currentTime - cosmicmod$lastHitTime;
                boolean isRecentHit = entity.getUUID().equals(cosmicmod$lastHitEntityId) && (timeSinceLastHit < 1000);
                double distSqr = entity.distanceToSqr(localPlayer);
                boolean isPlayerNearby = distSqr < 49.0;
                boolean isPlayer = entity instanceof Player;

                if (diff > 0) {
                    if ((isRecentHit || isPlayerNearby) && entity != localPlayer) {
                        boolean isCrit = isRecentHit && cosmicmod$wasCriticalHit;
                        DamageIndicatorManager.addIndicator(entity, diff, isCrit, isPlayer);
                    }
                }
            }
            cosmicmod$lastHealth = currentHealth;
        }
    }

    @Inject(method = "handleEntityEvent", at = @At("HEAD"))
    private void onHandleEntityEvent(byte status, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!entity.level().isClientSide) return;

        // Ensure lastHealth is initialized
        if (cosmicmod$lastHealth == -1) {
            cosmicmod$lastHealth = this.getHealth();
        }

        // status 2: damage
        // status 33: damage (player?)
        // status 36: damage (shield?)
        // status 37: damage (shield break?)
        if (status == 2 || status == 33 || status == 36 || status == 37 || status == 44 || status == 60) {
            Player localPlayer = Minecraft.getInstance().player;
            if (localPlayer == null) return;

            float currentHealth = this.getHealth();
            float lastKnownHealth = cosmicmod$lastHealth;
            float diff = lastKnownHealth - currentHealth;

            // If diff is 0, health hasn't updated in the object yet, but we KNOW damage happened because of the status.
            if (diff == 0) {
                 long currentTime = System.currentTimeMillis();
                 if (entity.getUUID().equals(cosmicmod$lastHitEntityId) && (currentTime - cosmicmod$lastHitTime < 1000) && entity != localPlayer) {
                     DamageIndicatorManager.addIndicator(entity, 0, cosmicmod$wasCriticalHit, entity instanceof Player);
                 }
            }
        }
        
        // Combat Cooldown
        if (status == 2 || status == 33 || status == 36 || status == 37 || status == 44 || status == 60) {
            LivingEntity entityObj = (LivingEntity) (Object) this;
            Player localPlayer = Minecraft.getInstance().player;
            if (localPlayer == null) return;

            if (entityObj instanceof Player) {
                net.minecraft.world.entity.LivingEntity attacker = entityObj.getLastHurtByMob();
                if (attacker instanceof Player) {
                    CosmicmodClient.getCooldownHud().startCooldown("Combat", 10000);
                } else if (entityObj == localPlayer) {
                    CosmicmodClient.getCooldownHud().startCooldown("Combat", 10000);
                }
            } else if (entityObj.getLastHurtByMob() == localPlayer) {
                CosmicmodClient.getCooldownHud().startCooldown("Combat", 10000);
            }
        }
    }

    @Inject(method = "setHealth", at = @At("HEAD"))
    private void onSetHealth(float health, CallbackInfo ci) {
        if (cosmicmod$lastHealth == -1) {
            cosmicmod$lastHealth = this.getHealth();
        }
    }

    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;)V", at = @At("HEAD"))
    private void onSwing(net.minecraft.world.InteractionHand hand, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        Minecraft client = Minecraft.getInstance();
        if (entity == client.player) {
            if (client.crosshairPickEntity instanceof LivingEntity target) {
                cosmicmod$lastHitEntityId = target.getUUID();
                cosmicmod$lastHitTime = System.currentTimeMillis();
                cosmicmod$wasCriticalHit = client.player.fallDistance > 0.0F && !client.player.onGround() && !client.player.onClimbable() && !client.player.isInWater() && !client.player.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS) && !client.player.isPassenger();
            }
        }
    }
}
