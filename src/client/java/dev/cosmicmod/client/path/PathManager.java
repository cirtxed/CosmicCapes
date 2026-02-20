package dev.cosmicmod.client.path;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;

import java.util.*;

public class PathManager {
    private static List<BlockPos> currentPath = null;
    private static BlockPos targetPos = null;

    public static void findPath(BlockPos target) {
        targetPos = target;
        calculatePath();
    }

    public static void clearPath() {
        currentPath = null;
        targetPos = null;
    }

    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        if (targetPos != null && client.player != null) {
            if (client.player.tickCount % 20 == 0) {
                calculatePath();
            }

            if (currentPath != null && !currentPath.isEmpty() && client.level != null) {
                if (client.player.tickCount % 5 == 0) {
                    for (int i = 0; i < currentPath.size(); i++) {
                        BlockPos p = currentPath.get(i);
                        // Only spawn particles near the player to save performance
                        if (p.closerThan(client.player.blockPosition(), 64)) {
                            double x = p.getX() + 0.5;
                            double y = p.getY() + 0.5;
                            double z = p.getZ() + 0.5;
                            client.level.addParticle(net.minecraft.core.particles.ParticleTypes.CRIT, x, y, z, 0, 0, 0);

                            // Draw particles closer together by interpolating to the next point
                            if (i < currentPath.size() - 1) {
                                BlockPos next = currentPath.get(i + 1);
                                double dx = (next.getX() - p.getX()) * 0.5;
                                double dy = (next.getY() - p.getY()) * 0.5;
                                double dz = (next.getZ() - p.getZ()) * 0.5;
                                client.level.addParticle(net.minecraft.core.particles.ParticleTypes.CRIT, x + dx, y + dy, z + dz, 0, 0, 0);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void calculatePath() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || targetPos == null || client.level == null) return;

        WalkNodeEvaluator evaluator = new WalkNodeEvaluator();
        evaluator.setCanPassDoors(true);
        evaluator.setCanOpenDoors(true);
        evaluator.setCanFloat(true);

        PathFinder pathFinder = new PathFinder(evaluator, 2000);
        
        BlockPos start = client.player.blockPosition();
        
        int range = 128;
        PathNavigationRegion region = new PathNavigationRegion(client.level, start.offset(-range, -range, -range), start.offset(range, range, range));

        // Use a dummy zombie to satisfy the PathFinder's Mob requirement.
        // Even though we're on the client, we can create a dummy Mob for the evaluator's use.
        // We use a Zombie as a generic walking mob proxy.
        net.minecraft.world.entity.monster.Zombie dummyMob = new net.minecraft.world.entity.monster.Zombie(net.minecraft.world.entity.EntityType.ZOMBIE, client.level);
        dummyMob.setPos(client.player.getX(), client.player.getY(), client.player.getZ());
        
        // We must also set the bounding box correctly, as the evaluator uses it.
        dummyMob.setBoundingBox(client.player.getBoundingBox());

        Path path = pathFinder.findPath(region, dummyMob, Set.of(targetPos), (float)range, 1, 1.0F);
        
        if (path != null) {
            List<BlockPos> points = new ArrayList<>();
            for (int i = 0; i < path.getNodeCount(); i++) {
                points.add(path.getNodePos(i));
            }
            currentPath = points;
        } else {
            currentPath = null;
        }
    }

    public static void render(PoseStack poseStack, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, Camera camera) {
        // Line rendering removed as per user request to use particles instead.
    }
}
