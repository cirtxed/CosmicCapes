package dev.cosmicmod.mixin.client;

import dev.cosmicmod.client.CosmicConfig;
import dev.cosmicmod.client.gui.CosmicConfigScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.screens.options.SkinOptionsScreen")
public abstract class SkinOptionsScreenMixin extends OptionsSubScreen {
    public SkinOptionsScreenMixin(net.minecraft.client.gui.screens.Screen parent, net.minecraft.client.Options options, Component title) {
        super(parent, options, title);
    }

    @Inject(method = "addOptions", at = @At("TAIL"))
    private void onAddOptions(CallbackInfo ci) {
        CosmicConfig config = CosmicConfig.getInstance();

        this.list.addSmall(
            CycleButton.onOffBuilder(config.showOrbs)
                .create(Component.literal("Orbital Orbs"), (button, value) -> {
                    config.showOrbs = value;
                    CosmicConfig.save();
                }),
            CycleButton.onOffBuilder(config.showDiamondChain)
                .create(Component.literal("Diamond Chain"), (button, value) -> {
                    config.showDiamondChain = value;
                    CosmicConfig.save();
                })
        );

        this.list.addSmall(
            Button.builder(Component.literal("More CosmicMod Settings..."), (button) -> {
                this.minecraft.setScreen(new CosmicConfigScreen(this));
            }).build(),
            null // Empty slot
        );
    }
}
