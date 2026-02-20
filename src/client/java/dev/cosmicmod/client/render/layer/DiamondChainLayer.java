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

public class DiamondChainLayer extends RenderLayer<PlayerRenderState, PlayerModel> {
    private static final ResourceLocation DIAMOND_BLOCK = ResourceLocation.withDefaultNamespace("textures/block/diamond_block.png");
    private static final java.util.List<UUID> TARGET_UUIDS = java.util.List.of(
            UUID.fromString("dce48f53-8c32-469f-b7fb-dc11e612d0f7"),
            UUID.fromString("09a8c489-1fb9-4617-a2bf-16adad508254")
    );

    public DiamondChainLayer(RenderLayerParent<PlayerRenderState, PlayerModel> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight, PlayerRenderState state, float limbSwing, float limbSwingAmount) {
        if (!CosmicConfig.getInstance().showDiamondChain) {
            return;
        }
        UUID uuid = ((PlayerUUIDHolder) state).cosmicmod$getUUID();
        if (uuid == null || !TARGET_UUIDS.contains(uuid)) {
            return;
        }

        poseStack.pushPose();
        
        // Follow the body (not the head)
        this.getParentModel().body.translateAndRotate(poseStack);
        
        // Center of the neck area
        poseStack.translate(0.0f, -0.02f, 0.0f); 
        
        float ageInTicks = state.ageInTicks;
        int linkCount = 32;
        float radiusX = 0.28f;
        float radiusZ = 0.22f;
        
        // Render diamond "links" all the way around the neck and down to the chest
        for (int i = 0; i < linkCount; i++) {
            poseStack.pushPose();
            
            float angle = (i * ((float)Math.PI * 2 / linkCount));
            float x = Mth.sin(angle) * radiusX;
            float z = Mth.cos(angle) * radiusZ;
            
            // Positioning logic to dip down onto the chest in the front
            float y = 0;
            // The front is at angle PI (Mth.sin(PI)=0, Mth.cos(PI)=-1)
            // We want it to dip lower as it gets closer to the front (angle = PI)
            // Flip the logic: Mth.cos(angle) is -1 at PI (front) and 1 at 0 (back)
            // So (1.0f - Mth.cos(angle)) / 2.0f is 1 at front and 0 at back
            float frontFactor = (1.0f - Mth.cos(angle)) / 2.0f; // 0 at back (angle 0), 1 at front (angle PI)
            y = frontFactor * 0.18f; // Dips 0.18 blocks down on the chest (increased from 0.1)
            
            poseStack.translate(x, y, z);
            
            // Front-most diamond is larger
            float diff = Math.abs(angle - (float)Math.PI);
            boolean isFrontCenter = diff < 0.2f;
            float scale = isFrontCenter ? 0.08f : 0.04f;
            poseStack.scale(scale, scale, scale);
            
            // Swing slightly
            poseStack.mulPose(new Quaternionf().fromAxisAngleDeg(1, 0, 0, Mth.sin(ageInTicks * 0.1f) * 5.0f));

            VertexConsumer vertexConsumer = multiBufferSource.getBuffer(RenderType.entityCutoutNoCull(DIAMOND_BLOCK));
            renderLink(poseStack, vertexConsumer, packedLight);
            
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private void renderLink(PoseStack poseStack, VertexConsumer consumer, int light) {
        PoseStack.Pose entry = poseStack.last();
        float s = 0.5f;
        // Simple cube for each link
        renderFace(entry, consumer, light, -s, -s, s, s, -s, s, s, s, s, -s, s, s, 0, 0, 1);
        renderFace(entry, consumer, light, s, -s, -s, -s, -s, -s, -s, s, -s, s, s, -s, 0, 0, -1);
        renderFace(entry, consumer, light, -s, -s, -s, -s, -s, s, -s, s, s, -s, s, -s, -1, 0, 0);
        renderFace(entry, consumer, light, s, -s, s, s, -s, -s, s, s, -s, s, s, s, 1, 0, 0);
        renderFace(entry, consumer, light, -s, s, s, s, s, s, s, s, -s, -s, s, -s, 0, 1, 0);
        renderFace(entry, consumer, light, -s, -s, -s, s, -s, -s, s, -s, s, -s, -s, s, 0, -1, 0);
    }

    private void renderFace(PoseStack.Pose entry, VertexConsumer consumer, int light, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float nx, float ny, float nz) {
        consumer.addVertex(entry, x1, y1, z1).setColor(1f, 1f, 1f, 1f).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(entry, nx, ny, nz);
        consumer.addVertex(entry, x2, y2, z2).setColor(1f, 1f, 1f, 1f).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(entry, nx, ny, nz);
        consumer.addVertex(entry, x3, y3, z3).setColor(1f, 1f, 1f, 1f).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(entry, nx, ny, nz);
        consumer.addVertex(entry, x4, y4, z4).setColor(1f, 1f, 1f, 1f).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(entry, nx, ny, nz);
    }
}
