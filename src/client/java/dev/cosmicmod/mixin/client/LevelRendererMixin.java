package dev.cosmicmod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.cosmicmod.client.ChatStateManager;
import dev.cosmicmod.client.path.PathManager;
import dev.cosmicmod.client.render.DamageIndicatorManager;
import dev.cosmicmod.client.waypoint.WaypointManager;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void onRenderLevel(GraphicsResourceAllocator graphicsResourceAllocator, DeltaTracker deltaTracker, boolean bl, Camera camera, GameRenderer gameRenderer, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        String server = ChatStateManager.getCurrentServer();
        // System.out.println("[DEBUG_LOG] onRenderLevel server='" + server + "' isSky=" + ChatStateManager.isCosmicSky() + " isPrisons=" + ChatStateManager.isCosmicPrisons());
        if (!ChatStateManager.isCosmicSky() && !ChatStateManager.isCosmicPrisons() && !server.equals("localhost") && !server.isEmpty() && !server.equals("play.cosmicsky.com") && !server.equals("play.cosmicprisons.com")) {
            // dev.cosmicmod.client.CosmicmodClient.LOGGER.info("[DEBUG] Not rendering waypoints: server={}, isSky={}, isPrisons={}", server, ChatStateManager.isCosmicSky(), ChatStateManager.isCosmicPrisons());
            return;
        }
        WaypointManager.renderBeams(deltaTracker, modelViewMatrix, projectionMatrix, camera);
        WaypointManager.renderTexts(deltaTracker, modelViewMatrix, projectionMatrix, camera);

        PoseStack poseStack = new PoseStack();

        // Render damage indicators
        DamageIndicatorManager.render(poseStack, modelViewMatrix, projectionMatrix, camera);

        // Render path
        PathManager.render(poseStack, modelViewMatrix, projectionMatrix, camera);
    }
}
