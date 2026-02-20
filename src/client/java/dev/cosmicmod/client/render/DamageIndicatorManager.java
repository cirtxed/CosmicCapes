package dev.cosmicmod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DamageIndicatorManager {
    private static final List<DamageEntry> indicators = new ArrayList<>();
    private static final java.util.Random RANDOM = new java.util.Random();

    public static void addIndicator(net.minecraft.world.entity.LivingEntity entity, double amount, boolean isCritical, boolean isPlayer) {
        indicators.add(new DamageEntry(entity, amount, isCritical, isPlayer));
    }

    public static boolean isEnabled() {
        dev.cosmicmod.client.hud.HudModule module = dev.cosmicmod.client.hud.HudManager.getModules().stream()
                .filter(m -> m.getName().equals("Damage Indicator"))
                .findFirst().orElse(null);
        return module == null || module.isEnabled();
    }

    public static void render(PoseStack poseStack, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, Camera camera) {
        if (indicators.isEmpty()) return;
        
        // Check if enabled
        if (!isEnabled()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        Font font = client.font;
        Vec3 cameraPos = camera.getPosition();
        long now = System.currentTimeMillis();

        MultiBufferSource.BufferSource bufferSource = client.renderBuffers().bufferSource();

        indicators.removeIf(DamageEntry::isExpired);

        for (DamageEntry indicator : indicators) {
            long elapsed = now - indicator.startTime;
            float elapsedSeconds = elapsed / 1000.0f;

            float verticalMovement = calculateVerticalMovement(elapsedSeconds);
            
            Vec3 entityPos = indicator.getCurrentEntityPos();
            double distanceToEntity = entityPos.distanceTo(cameraPos);

            // Calculate position with offsets similar to example
            float horizontalOffset = indicator.horizontalOffset * 0.6f;
            float verticalOffset = indicator.verticalOffset * 0.6f;
            float depthOffset = indicator.depthOffset * 0.6f;

            // Offset the text to be in front of the entity from the camera's perspective
            Vec3 cameraForward = Vec3.directionFromRotation(camera.getXRot(), camera.getYRot());
            float dynamicFrontOffset = 1.2F * (float) Math.max(1.0, distanceToEntity * 0.3);

            Vec3 pos = entityPos.add(
                    cameraForward.x * dynamicFrontOffset + horizontalOffset,
                    (indicator.entityHeight * 0.5f) + verticalOffset + verticalMovement,
                    cameraForward.z * dynamicFrontOffset + depthOffset
            );

            poseStack.pushPose();
            poseStack.setIdentity();
            poseStack.mulPose(modelViewMatrix);
            poseStack.translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z);

            poseStack.mulPose(new Quaternionf().fromAxisAngleDeg(0.0f, 1.0f, 0.0f, -camera.getYRot()));
            poseStack.mulPose(new Quaternionf().fromAxisAngleDeg(1.0f, 0.0f, 0.0f, camera.getXRot()));

            float scale = 0.035F * (float) (1.0 + 1.0 / (distanceToEntity + 1.0));
            scale = Math.min(scale, 0.065F);

            // Stacking scale
            List<DamageEntry> sameEntityEntries = indicators.stream()
                    .filter(e -> e.entityUUID.equals(indicator.entityUUID))
                    .sorted((e1, e2) -> Long.compare(e2.startTime, e1.startTime))
                    .toList();
            int entryIndex = sameEntityEntries.indexOf(indicator);
            float orderScale = (float) Math.pow(0.5, entryIndex);
            scale *= orderScale;

            // Animation scaling
            float animationProgress = elapsedSeconds / 1.5f;
            if (animationProgress < 0.2f) {
                scale *= 0.8f + animationProgress;
            } else if (animationProgress > 0.7f) {
                scale *= 1.0f - (animationProgress - 0.7f) * 0.7f;
            }

            poseStack.scale(-scale, -scale, scale);

            String text;
            if (indicator.amount == 0) {
                text = "Hit!";
            } else {
                text = String.format("-%d", Math.round(indicator.amount));
            }
            
            if (indicator.isCritical) {
                text += "!";
            }
            int color = getTextColor(indicator, animationProgress);

            float width = font.width(text);
            font.drawInBatch(text, -width / 2f, -font.lineHeight / 2f, color, true, poseStack.last().pose(), bufferSource, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);

            poseStack.popPose();
        }
        bufferSource.endBatch();
    }

    private static float calculateVerticalMovement(float elapsedTime) {
        if (elapsedTime < 0.2f)
            return elapsedTime * 2.5f;
        return 0.5f - (elapsedTime - 0.2f) * 0.4f;
    }

    private static int getTextColor(DamageEntry entry, float progress) {
        int color = entry.isPlayer ? 0xFF0000 : 0xFFFFFF; // Red for players, White for others
        int alpha = (int) (255.0f * (1.0f - Math.max(0.0f, (progress - 0.7f) / 0.3f)));
        return (alpha << 24) | color;
    }

    private static class DamageEntry {
        final java.util.UUID entityUUID;
        Vec3 entityPos;
        final float entityHeight;
        final long startTime;
        final double amount;
        final boolean isCritical;
        final boolean isPlayer;
        final float horizontalOffset;
        final float verticalOffset;
        final float depthOffset;

        DamageEntry(net.minecraft.world.entity.LivingEntity entity, double amount, boolean isCritical, boolean isPlayer) {
            this.entityUUID = entity.getUUID();
            this.entityPos = entity.position();
            this.entityHeight = entity.getBbHeight();
            this.amount = amount;
            this.isCritical = isCritical;
            this.isPlayer = isPlayer;
            this.startTime = System.currentTimeMillis();
            this.horizontalOffset = RANDOM.nextFloat() * 2.0f - 1.0f;
            this.verticalOffset = RANDOM.nextFloat() * 2.0f - 1.0f;
            this.depthOffset = RANDOM.nextFloat() * 2.0f - 1.0f;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - startTime > 1500;
        }

        Vec3 getCurrentEntityPos() {
            Minecraft client = Minecraft.getInstance();
            if (client.level != null) {
                for (net.minecraft.world.entity.Entity entity : client.level.entitiesForRendering()) {
                    if (entity.getUUID().equals(entityUUID)) {
                        this.entityPos = entity.position();
                        break;
                    }
                }
            }
            return entityPos;
        }
    }
}
