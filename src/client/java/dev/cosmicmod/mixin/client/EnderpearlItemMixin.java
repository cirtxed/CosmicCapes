package dev.cosmicmod.mixin.client;

import dev.cosmicmod.client.CosmicmodClient;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderpearlItem.class)
public class EnderpearlItemMixin {
    @Inject(method = "use", at = @At("HEAD"))
    private void onPearlUse(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (level.isClientSide) {
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.isEmpty() && stack.getItem() instanceof EnderpearlItem) {
                dev.cosmicmod.client.ChatStateManager.setLastPearlThrowTime(System.currentTimeMillis());
                CosmicmodClient.getCooldownHud().startCooldown("Pearl", 60000); // Pearl base cooldown 60s
            }
        }
    }
}
