package dev.cosmicmod.mixin.client;

import dev.cosmicmod.client.CosmicmodClient;
import dev.cosmicmod.client.ChatStateManager;
import dev.cosmicmod.client.SearchManager;
import dev.cosmicmod.client.util.ItemOverlayUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class InventoryMixin extends net.minecraft.client.gui.screens.Screen {
    protected InventoryMixin(Component title) { super(title); }

    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow @Final protected net.minecraft.world.inventory.AbstractContainerMenu menu;
    @Shadow protected int imageWidth;
    @Shadow protected int imageHeight;

    @Unique
    private EditBox searchBox;

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        if (!SearchManager.isOnCosmic() && !ChatStateManager.getCurrentServer().equals("localhost")) return;
        if (!CosmicmodClient.getSearchHud().isEnabled()) return;
        
        Minecraft client = Minecraft.getInstance();
        
        int boxWidth = 80;
        int boxHeight = 12;
        // Position it to the right of the inventory
        this.searchBox = new EditBox(client.font, this.leftPos + this.imageWidth - boxWidth, this.topPos - boxHeight - 2, boxWidth, boxHeight, Component.literal("Search..."));
        this.searchBox.setValue(SearchManager.getSearchQuery());
        this.searchBox.setResponder(SearchManager::setSearchQuery);
    }

    @Shadow protected Slot hoveredSlot;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderLabels(Lnet/minecraft/client/gui/GuiGraphics;II)V", shift = At.Shift.BEFORE))
    private void onRenderBeforeLabels(GuiGraphics context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (!SearchManager.isOnCosmic() && !ChatStateManager.getCurrentServer().equals("localhost")) return;
        boolean searchEnabled = CosmicmodClient.getSearchHud().isEnabled();

        for (Slot slot : this.menu.slots) {
            if (!slot.hasItem()) continue;

            ItemStack inSlot = slot.getItem();
            
            
            // Highlight if matches search
            // Use 0, 0 because the pose is already translated to leftPos, topPos
            if (searchEnabled && SearchManager.matches(inSlot)) {
                ItemOverlayUtil.drawHighlight(context, slot, 0, 0, 0x80FFFF00); // Yellow highlight
            }

            // Draw Item Overlays if enabled
            if (CosmicmodClient.getItemOverlayHud().isEnabled()) {
                float scale = CosmicmodClient.getItemOverlayHud().getSettings().scale;
                int color = CosmicmodClient.getItemOverlayHud().getSettings().formattingColor;

                // Draw Cosmic Energy amount
                String energy = SearchManager.getCosmicEnergyAmount(inSlot);
                if (energy != null) {
                    ItemOverlayUtil.drawText(context, this.font, energy, slot, 0, 0, color, scale);
                }

                // Draw Trinket charges
                String charges = SearchManager.getTrinketCharges(inSlot);
                if (charges != null) {
                    ItemOverlayUtil.drawText(context, this.font, charges, slot, 0, 0, color, scale);
                }

                // Draw Money Note value
                String noteValue = SearchManager.getMoneyNoteValue(inSlot);
                if (noteValue != null) {
                    ItemOverlayUtil.drawText(context, this.font, noteValue, slot, 0, 0, color, scale);
                }

                // Draw EXP Bottle value
                String expValue = SearchManager.getExpBottleValue(inSlot);
                if (expValue != null) {
                    ItemOverlayUtil.drawText(context, this.font, expValue, slot, 0, 0, color, scale);
                }
            }

            // Draw Pearl cooldown
            if (inSlot.getItem() instanceof EnderpearlItem) {
                float progress = CosmicmodClient.getCooldownHud().getCooldownProgress("Pearl");
                if (progress > 0) {
                    ItemOverlayUtil.drawCooldown(context, slot.x, slot.y, progress, 200.0f);
                }
            }

            // Draw Trinket cooldown from potion effects
            if (SearchManager.isTrinket(inSlot)) {
                String effectId = SearchManager.getTrinketEffectId(inSlot);
                if (effectId != null) {
                    Minecraft client = Minecraft.getInstance();
                    if (client.player != null) {
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
                                    int maxDurationMillis = SearchManager.getTrinketMaxDurationMillis(inSlot);
                                    float progress = (float) (duration * 50L) / maxDurationMillis;
                                    ItemOverlayUtil.drawCooldown(context, slot.x, slot.y, Math.min(1.0f, progress), 200.0f);
                                }
                            });
                    }
                }
            }


            // Draw Pet cooldown
            if (SearchManager.isPet(inSlot)) {
                long lastUsed = SearchManager.getPetLastUsed(inSlot);
                if (lastUsed > 0) {
                    long now = System.currentTimeMillis();
                    int cooldownMillis = SearchManager.getPetCooldownMillis(inSlot);
                    if (cooldownMillis > 0) {
                        long elapsed = now - lastUsed;
                        if (elapsed < cooldownMillis) {
                            float progress = 1.0f - ((float) elapsed / cooldownMillis);
                            ItemOverlayUtil.drawCooldown(context, slot.x, slot.y, progress, 200.0f);
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphics context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (!SearchManager.isOnCosmic() && !ChatStateManager.getCurrentServer().equals("localhost")) return;
        if (!CosmicmodClient.getSearchHud().isEnabled()) return;
        
        if (this.searchBox != null) {
            this.searchBox.render(context, mouseX, mouseY, deltaTicks);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (!CosmicmodClient.getSearchHud().isEnabled()) return;
        if (this.searchBox != null && this.searchBox.isFocused()) {
            if (this.searchBox.keyPressed(keyCode, scanCode, modifiers)) {
                cir.setReturnValue(true);
            } else if (keyCode == 256) { // ESC
                this.searchBox.setFocused(false);
                cir.setReturnValue(true);
            } else if (Minecraft.getInstance().options.keyInventory.matches(keyCode, scanCode)) {
                // Prevent inventory from closing when inventory key is pressed while typing
                cir.setReturnValue(true);
            }
        }
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (CosmicmodClient.getSearchHud().isEnabled() && this.searchBox != null && this.searchBox.isFocused()) {
            return this.searchBox.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!CosmicmodClient.getSearchHud().isEnabled()) return;
        if (this.searchBox != null) {
            if (this.searchBox.mouseClicked(mouseX, mouseY, button)) {
                this.searchBox.setFocused(true);
                cir.setReturnValue(true);
            } else {
                this.searchBox.setFocused(false);
            }
        }
    }
}
