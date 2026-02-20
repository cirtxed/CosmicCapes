package dev.cosmicmod.mixin.client;

import dev.cosmicmod.client.CosmicmodClient;
import dev.cosmicmod.client.ChatStateManager;
import dev.cosmicmod.client.SearchManager;
import dev.cosmicmod.client.hud.HudManager;
import dev.cosmicmod.client.util.ItemOverlayUtil;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!ChatStateManager.isCosmicSky() && !ChatStateManager.isCosmicPrisons() && !ChatStateManager.getCurrentServer().equals("localhost")) return;
        HudManager.render(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false));
        
        renderHotbarOverlays(guiGraphics);
    }

    private void renderHotbarOverlays(GuiGraphics context) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null) return;

        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();
        int hotbarX = width / 2 - 91;
        int hotbarY = height - 22;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            int x = hotbarX + i * 20 + 3;
            int y = hotbarY + 3;

            // Draw Item Overlays if enabled
            if (CosmicmodClient.getItemOverlayHud().isEnabled()) {
                float scale = CosmicmodClient.getItemOverlayHud().getSettings().scale;
                int color = CosmicmodClient.getItemOverlayHud().getSettings().formattingColor;
                
                // Draw Cosmic Energy amount
                String energy = SearchManager.getCosmicEnergyAmount(stack);
                if (energy != null) {
                    ItemOverlayUtil.drawTextAt(context, client.font, energy, x, y, color, 800.0f, scale);
                }

                // Draw Trinket charges
                String charges = SearchManager.getTrinketCharges(stack);
                if (charges != null) {
                    ItemOverlayUtil.drawTextAt(context, client.font, charges, x, y, color, 800.0f, scale);
                }

                // Draw Money Note value
                String noteValue = SearchManager.getMoneyNoteValue(stack);
                if (noteValue != null) {
                    ItemOverlayUtil.drawTextAt(context, client.font, noteValue, x, y, color, 800.0f, scale);
                }

                // Draw EXP Bottle value
                String expValue = SearchManager.getExpBottleValue(stack);
                if (expValue != null) {
                    ItemOverlayUtil.drawTextAt(context, client.font, expValue, x, y, color, 800.0f, scale);
                }
            }

            // Draw Pearl cooldown
            if (stack.getItem() instanceof EnderpearlItem) {
                float progress = CosmicmodClient.getCooldownHud().getCooldownProgress("Pearl");
                if (progress > 0) {
                    ItemOverlayUtil.drawCooldown(context, x, y, progress, 700.0f);
                }
            }

            // Draw Trinket cooldown from potion effects
            if (SearchManager.isTrinket(stack)) {
                String effectId = SearchManager.getTrinketEffectId(stack);
                if (effectId != null) {
                    client.player.getActiveEffects().stream()
                        .filter(instance -> {
                            String registryName = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(instance.getEffect().value()).toString();
                            return registryName.contains(effectId);
                        })
                        .findFirst()
                        .ifPresent(instance -> {
                            // Ignore infinite effects
                            if (instance.getDuration() < 0 || instance.getDuration() > 1000000) return;

                            int duration = instance.getDuration();
                            if (duration > 0) {
                                int maxDurationMillis = SearchManager.getTrinketMaxDurationMillis(stack);
                                float progress = (float) (duration * 50L) / maxDurationMillis;
                                ItemOverlayUtil.drawCooldown(context, x, y, Math.min(1.0f, progress), 700.0f);
                            }
                        });
                }
            }


            // Draw Powerball cooldown on pickaxes
            if (stack.getItem() instanceof net.minecraft.world.item.PickaxeItem) {
                float progress = CosmicmodClient.getCooldownHud().getCooldownProgress("Powerball");
                if (progress > 0) {
                    ItemOverlayUtil.drawCooldown(context, x, y, progress, 700.0f);
                }
            }

            // Draw Pet cooldown
            if (SearchManager.isPet(stack)) {
                long lastUsed = SearchManager.getPetLastUsed(stack);
                if (lastUsed > 0) {
                    long now = System.currentTimeMillis();
                    int cooldownMillis = SearchManager.getPetCooldownMillis(stack);
                    if (cooldownMillis > 0) {
                        long elapsed = now - lastUsed;
                        if (elapsed < cooldownMillis) {
                            float progress = 1.0f - ((float) elapsed / cooldownMillis);
                            ItemOverlayUtil.drawCooldown(context, x, y, progress, 700.0f);
                        }
                    }
                }
            }
        }
    }
}
