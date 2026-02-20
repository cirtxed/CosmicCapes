package dev.cosmicmod.mixin.client;

import dev.cosmicmod.client.CosmicConfig;
import dev.cosmicmod.client.CosmicmodClient;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class ItemMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void onUse(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (level.isClientSide) {
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.isEmpty() && stack.getItem() instanceof PickaxeItem) {
                dev.cosmicmod.client.ChatStateManager.setLastRightClickTime(System.currentTimeMillis());
                // Only start cooldown if NOT already on cooldown to prevent resetting it on hold
                if (!CosmicmodClient.getCooldownHud().isOnCooldown("Powerball")) {
                    // Cooldown is now started via Sound detection in ClientPlayNetworkHandlerMixin
                }
            }
        }
    }
}
