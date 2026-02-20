package dev.cosmicmod.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.cosmicmod.client.CosmicConfig;
import dev.cosmicmod.client.util.PlayerUUIDHolder;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

import java.util.UUID;

public class OrbitalOrbsLayer extends RenderLayer<PlayerRenderState, PlayerModel> {
    private static final ResourceLocation ORB_TEXTURE = ResourceLocation.withDefaultNamespace("textures/block/emerald_block.png");
    private static final java.util.List<UUID> TARGET_UUIDS = java.util.List.of(
            UUID.fromString("dce48f53-8c32-469f-b7fb-dc11e612d0f7"),
            UUID.fromString("09a8c489-1fb9-4617-a2bf-16adad508254")
    );

    public OrbitalOrbsLayer(RenderLayerParent<PlayerRenderState, PlayerModel> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight, PlayerRenderState state, float limbSwing, float limbSwingAmount) {
        if (!CosmicConfig.getInstance().showOrbs) {
            return;
        }
        UUID uuid = ((PlayerUUIDHolder) state).cosmicmod$getUUID();
        if (uuid == null || !TARGET_UUIDS.contains(uuid)) {
            return;
        }
        
        float ageInTicks = state.ageInTicks;
        int orbCount = 3;
        float orbitRadius = 1.2f;
        float orbitSpeed = 0.05f;

        for (int i = 0; i < orbCount; i++) {
            poseStack.pushPose();
            
            float angle = (ageInTicks * orbitSpeed) + (i * ((float)Math.PI * 2 / orbCount));
            float x = Mth.cos(angle) * orbitRadius;
            float z = Mth.sin(angle) * orbitRadius;
            float y = Mth.sin(ageInTicks * 0.1f + i) * 0.2f + 1.0f; // Floating up and down

            poseStack.translate(x, y, z);
            
            // Spin the orb
            poseStack.mulPose(new Quaternionf().fromAxisAngleDeg(0, 1, 0, ageInTicks * 2));
            
            float scale = 0.2f;
            poseStack.scale(scale, scale, scale);

            VertexConsumer vertexConsumer = multiBufferSource.getBuffer(RenderType.entityCutoutNoCull(ORB_TEXTURE));
            renderCube(poseStack, vertexConsumer, packedLight);
            
            poseStack.popPose();
        }
    }

    private void renderCube(PoseStack poseStack, VertexConsumer consumer, int light) {
        PoseStack.Pose entry = poseStack.last();
        // Simple 1x1x1 cube (will be scaled by the caller)
        float s = 0.5f;
        // Front
        renderFace(entry, consumer, light, -s, -s, s, s, -s, s, s, s, s, -s, s, s, 0, 0, 1);
        // Back
        renderFace(entry, consumer, light, s, -s, -s, -s, -s, -s, -s, s, -s, s, s, -s, 0, 0, -1);
        // Left
        renderFace(entry, consumer, light, -s, -s, -s, -s, -s, s, -s, s, s, -s, s, -s, -1, 0, 0);
        // Right
        renderFace(entry, consumer, light, s, -s, s, s, -s, -s, s, s, -s, s, s, s, 1, 0, 0);
        // Top
        renderFace(entry, consumer, light, -s, s, s, s, s, s, s, s, -s, -s, s, -s, 0, 1, 0);
        // Bottom
        renderFace(entry, consumer, light, -s, -s, -s, s, -s, -s, s, -s, s, -s, -s, s, 0, -1, 0);
    }

    private void renderFace(PoseStack.Pose entry, VertexConsumer consumer, int light, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float nx, float ny, float nz) {
        consumer.addVertex(entry, x1, y1, z1).setColor(1f, 1f, 1f, 1f).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(entry, nx, ny, nz);
        consumer.addVertex(entry, x2, y2, z2).setColor(1f, 1f, 1f, 1f).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(entry, nx, ny, nz);
        consumer.addVertex(entry, x3, y3, z3).setColor(1f, 1f, 1f, 1f).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(entry, nx, ny, nz);
        consumer.addVertex(entry, x4, y4, z4).setColor(1f, 1f, 1f, 1f).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(entry, nx, ny, nz);
    }
}
