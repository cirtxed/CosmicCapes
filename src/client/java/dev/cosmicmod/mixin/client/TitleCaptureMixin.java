package dev.cosmicmod.mixin.client;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class TitleCaptureMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("cosmicmod");

    @Inject(method = "setTitleText", at = @At("HEAD"))
    private void onSetTitle(ClientboundSetTitleTextPacket packet, CallbackInfo ci) {
        if (packet.text() != null) {
            String plainText = packet.text().getString();
            LOGGER.info("[Captured Title] {}", plainText);
        }
    }

    @Inject(method = "setSubtitleText", at = @At("HEAD"))
    private void onSetSubtitle(ClientboundSetSubtitleTextPacket packet, CallbackInfo ci) {
        if (packet.text() != null) {
            String plainText = packet.text().getString();
            LOGGER.info("[Captured Subtitle] {}", plainText);
        }
    }

    @Inject(method = "setActionBarText", at = @At("HEAD"))
    private void onSetActionBar(ClientboundSetActionBarTextPacket packet, CallbackInfo ci) {
        if (packet.text() != null) {
            String plainText = packet.text().getString();
            LOGGER.info("[Captured Action Bar] {}", plainText);
        }
    }
}
