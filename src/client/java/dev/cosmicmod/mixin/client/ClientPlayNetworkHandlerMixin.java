package dev.cosmicmod.mixin.client;

import dev.cosmicmod.client.ChatStateManager;
import dev.cosmicmod.client.CosmicmodClient;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Shadow public abstract ServerData getServerData();

    @Inject(method = "handleLogin", at = @At("RETURN"))
    private void onLoginTail(ClientboundLoginPacket packet, CallbackInfo ci) {
        ServerData info = this.getServerData();
        if (info != null) {
            ChatStateManager.setCurrentServer(info.ip);
        } else {
            ChatStateManager.setCurrentServer("localhost");
        }
    }

    @Inject(method = "handleSoundEvent", at = @At("HEAD"))
    private void onSoundEvent(ClientboundSoundPacket packet, CallbackInfo ci) {
        if (packet.getSound().value() == SoundEvents.WITHER_SHOOT) {
            // Powerball shot detected via sound
            // Only start if the player was holding right-click recently
            if (System.currentTimeMillis() - ChatStateManager.getLastRightClickTime() < 100) {
                CosmicmodClient.getCooldownHud().startCooldown("Powerball", 40000);
            }
        }
    }
}
