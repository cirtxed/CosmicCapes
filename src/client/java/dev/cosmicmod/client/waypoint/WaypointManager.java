package dev.cosmicmod.client.waypoint;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.cosmicmod.client.ChatStateManager;
import dev.cosmicmod.client.CosmicConfig;
import dev.cosmicmod.client.CosmicmodClient;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.cosmicmod.client.render.GuardBeaconRenderType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WaypointManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Cosmicmod-Waypoint");
    private static final Map<String, Waypoint> WAYPOINTS = new ConcurrentHashMap<>();
    private static final Map<String, Waypoint> PINGS = new ConcurrentHashMap<>();
    private static Waypoint previewWaypoint = null;
    
    private static final Map<String, String> LABEL_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> LOCATION_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Player> PLAYER_REF_CACHE = new ConcurrentHashMap<>();

    private static final StringBuilder TEXT_BUILDER = new StringBuilder();

    static {
        loadWaypoints();
    }

    public static void loadWaypoints() {
        WAYPOINTS.clear();
        CosmicConfig config = CosmicConfig.getInstance();
        if (config.waypoints != null) {
            String currentServer = ChatStateManager.getCurrentServer();
            for (Waypoint w : config.waypoints) {
                if (w.server() == null || w.server().isEmpty() || w.server().equals(currentServer)) {
                    WAYPOINTS.put(w.id(), w);
                    updateCaches(w);
                }
            }
        }
    }

    public static void saveWaypoints() {
        CosmicConfig config = CosmicConfig.getInstance();
        List<Waypoint> otherServerWaypoints = config.waypoints.stream()
                .filter(w -> w.server() != null && !w.server().isEmpty() && !w.server().equals(ChatStateManager.getCurrentServer()))
                .toList();
        
        List<Waypoint> currentServerWaypoints = WAYPOINTS.values().stream()
                .filter(w -> w.expirationTime() == 0)
                .toList();

        List<Waypoint> allToSave = new ArrayList<>(otherServerWaypoints);
        allToSave.addAll(currentServerWaypoints);
        
        config.waypoints = allToSave;
        CosmicConfig.save();
    }

    public static void addWaypoint(Waypoint waypoint) {
        if (waypoint.id().startsWith("ping_")) {
            PINGS.put(waypoint.id(), waypoint);
        } else {
            
            // Prevent adding duplicate waypoints by name
            for (Waypoint existing : WAYPOINTS.values()) {
                if (existing.label().getString().equalsIgnoreCase(waypoint.label().getString())) {
                    return;
                }
            }
            WAYPOINTS.put(waypoint.id(), waypoint);
        }
        updateCaches(waypoint);
        if (waypoint.isFocus() && waypoint.playerName() != null) {
            updatePlayerCache(waypoint.playerName());
        }
        if (waypoint.expirationTime() == 0) {
            saveWaypoints();
        }
    }

    public static void removeWaypoint(String id) {
        Waypoint waypoint = WAYPOINTS.remove(id);
        if (waypoint == null) {
            waypoint = PINGS.remove(id);
        }
        LABEL_CACHE.remove(id);
        LOCATION_CACHE.remove(id);
        if (waypoint != null && waypoint.playerName() != null) {
            PLAYER_REF_CACHE.remove(waypoint.playerName());
        }
        if (waypoint != null && waypoint.expirationTime() == 0) {
            saveWaypoints();
        }
    }

    public static void toggleWaypoint(String id) {
        Waypoint w = WAYPOINTS.get(id);
        if (w != null) {
            Waypoint updated = w.withVisible(!w.visible());
            WAYPOINTS.put(id, updated);
            if (updated.expirationTime() == 0) {
                saveWaypoints();
            }
        }
    }

    public static void toggleAllWaypoints() {
        boolean newState = !CosmicmodClient.getWaypointHud().isEnabled();
        CosmicmodClient.getWaypointHud().setEnabled(newState);

        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.displayClientMessage(Component.literal("§b[CosmicMod] §fWaypoints are now " + (newState ? "§aVISIBLE" : "§cHIDDEN")), true);
        }
    }

    public static void shareWaypoint(Waypoint waypoint) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        String msg = String.format("[!] Waypoint: %s at %dx %dy %dz in %s",
                waypoint.label().getString(),
                (int) waypoint.pos().x, (int) waypoint.pos().y, (int) waypoint.pos().z,
                waypoint.world() != null ? waypoint.world() : "Unknown");

        if (ChatStateManager.isCosmicSky()) {
            if (!ChatStateManager.getActiveChannel().equalsIgnoreCase("Alliance")) {
                ChatStateManager.setSwitchingChannelForPing(true, msg);
                client.player.connection.sendCommand("chat a");
            } else {
                client.player.connection.sendChat(msg);
                client.level.playSound(client.player, client.player.getX(), client.player.getY(), client.player.getZ(), 
                    SoundEvents.NOTE_BLOCK_PLING, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
        } else if (ChatStateManager.isCosmicPrisons()) {
            if (!ChatStateManager.getActiveChannel().equalsIgnoreCase("Gang") && !ChatStateManager.getActiveChannel().equalsIgnoreCase("Truce")) {
                ChatStateManager.setLastAttemptedPing(msg);
                ChatStateManager.setSwitchingChannelForPing(true, msg);
                client.player.connection.sendCommand("gang chat truce");
            } else {
                ChatStateManager.setLastAttemptedPing(msg);
                client.player.connection.sendChat(msg);
                client.level.playSound(client.player, client.player.getX(), client.player.getY(), client.player.getZ(), 
                    SoundEvents.NOTE_BLOCK_PLING, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
        } else {
            client.player.connection.sendChat(msg);
        }
    }

    public static void clearWaypoints() {
        WAYPOINTS.clear();
        LABEL_CACHE.clear();
        LOCATION_CACHE.clear();
        PLAYER_REF_CACHE.clear();
        LOGGER.info("Cleared all waypoints");
    }

    private static void updatePlayerCache(String playerName) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        for (Player p : client.level.players()) {
            if (p.getName().getString().equals(playerName)) {
                PLAYER_REF_CACHE.put(playerName, p);
                return;
            }
        }
    }

    private static void updateCaches(Waypoint waypoint) {
        StringBuilder labelBuilder = new StringBuilder();
        if (waypoint.playerName() != null && !waypoint.playerName().isEmpty()) {
            labelBuilder.append("§b").append(waypoint.playerName());
        } else if (waypoint.label() != null) {
            labelBuilder.append("§f").append(waypoint.label().getString());
        }
        LABEL_CACHE.put(waypoint.id(), labelBuilder.toString());
        
        LOCATION_CACHE.put(waypoint.id(), String.format("\n§7%dx %dy %dz", (int)waypoint.pos().x, (int)waypoint.pos().y, (int)waypoint.pos().z));
    }

    public static Map<String, Waypoint> getWaypoints() {
        return WAYPOINTS;
    }

    public static Map<String, Waypoint> getPings() {
        return PINGS;
    }

    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;

        WAYPOINTS.entrySet().removeIf(entry -> tickWaypoint(entry.getKey(), entry.getValue(), WAYPOINTS));
        PINGS.entrySet().removeIf(entry -> tickWaypoint(entry.getKey(), entry.getValue(), PINGS));
    }

    private static boolean tickWaypoint(String key, Waypoint waypoint, Map<String, Waypoint> map) {
        if (waypoint.isExpired()) return true;

        if (waypoint.isFocus()) {
            Player p = PLAYER_REF_CACHE.get(waypoint.playerName());
            if (p == null || p.isRemoved()) {
                updatePlayerCache(waypoint.playerName());
                p = PLAYER_REF_CACHE.get(waypoint.playerName());
            }

            if (p != null) {
                Waypoint updated = new Waypoint(
                        waypoint.id(),
                        p.position(),
                        waypoint.label(),
                        waypoint.color(),
                        waypoint.expirationTime(),
                        waypoint.playerName(),
                        p.getHealth(),
                        true,
                        waypoint.world(),
                        waypoint.visible(),
                        waypoint.server()
                );
                map.put(key, updated);
                updateCaches(updated);
            }
        }
        return false;
    }

    public static void renderBeams(net.minecraft.client.DeltaTracker deltaTracker, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, Camera camera) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        String currentWorld = CosmicmodClient.getWorldDisplayName(client.level.dimension().location().getPath());

        MultiBufferSource.BufferSource bufferSource = client.renderBuffers().bufferSource();
        long gameTime = client.level.getGameTime();
        float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(true);
        PoseStack poseStack = new PoseStack();

        if (CosmicmodClient.getWaypointHud().isEnabled()) {
            for (Waypoint waypoint : WAYPOINTS.values()) {
                if (waypoint.visible() && !waypoint.isFocus()) {
                    if (waypoint.world() == null || waypoint.world().isEmpty() || waypoint.world().equals(currentWorld)) {
                        renderWaypointBeam(waypoint, poseStack, modelViewMatrix, bufferSource, gameTime, partialTicks, camera);
                    }
                }
            }
        } else {
        }

        for (Waypoint ping : PINGS.values()) {
            if (!ping.isFocus()) {
                if (ping.world() == null || ping.world().isEmpty() || ping.world().equals(currentWorld)) {
                    renderWaypointBeam(ping, poseStack, modelViewMatrix, bufferSource, gameTime, partialTicks, camera);
                }
            }
        }

        if (previewWaypoint != null) {
            renderWaypointBeam(previewWaypoint, poseStack, modelViewMatrix, bufferSource, gameTime, partialTicks, camera);
        }
        bufferSource.endBatch();
    }

    public static void renderTexts(net.minecraft.client.DeltaTracker deltaTracker, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, Camera camera) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        String currentWorld = CosmicmodClient.getWorldDisplayName(client.level.dimension().location().getPath());

        MultiBufferSource.BufferSource bufferSource = client.renderBuffers().bufferSource();
        float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(true);
        PoseStack poseStack = new PoseStack();

        if (CosmicmodClient.getWaypointHud().isEnabled()) {
            for (Waypoint waypoint : WAYPOINTS.values()) {
                if (waypoint.visible()) {
                    if (waypoint.isFocus()) {
                        renderFocusArrow(waypoint, poseStack, modelViewMatrix, bufferSource, camera, partialTicks);
                    } else {
                        if (waypoint.world() == null || waypoint.world().isEmpty() || waypoint.world().equals(currentWorld)) {
                            renderWaypointText(waypoint, poseStack, modelViewMatrix, bufferSource, camera, partialTicks);
                        }
                    }
                }
            }
        }

        for (Waypoint ping : PINGS.values()) {
            if (ping.isFocus()) {
                renderFocusArrow(ping, poseStack, modelViewMatrix, bufferSource, camera, partialTicks);
            } else {
                if (ping.world() == null || ping.world().isEmpty() || ping.world().equals(currentWorld)) {
                    renderWaypointText(ping, poseStack, modelViewMatrix, bufferSource, camera, partialTicks);
                }
            }
        }

        if (previewWaypoint != null) {
            renderWaypointText(previewWaypoint, poseStack, modelViewMatrix, bufferSource, camera, partialTicks);
        }
        bufferSource.endBatch();
    }

    private static void renderFocusArrow(Waypoint waypoint, PoseStack poseStack, Matrix4f modelViewMatrix, MultiBufferSource bufferSource, Camera camera, float partialTicks) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        
        Player target = PLAYER_REF_CACHE.get(waypoint.playerName());
        if (target == null || target.isRemoved()) {
            updatePlayerCache(waypoint.playerName());
            target = PLAYER_REF_CACHE.get(waypoint.playerName());
        }
        
        if (target == null) return;
        
        Vec3 cameraPos = camera.getPosition();
        double tx = Mth.lerp(partialTicks, target.xo, target.getX());
        double ty = Mth.lerp(partialTicks, target.yo, target.getY());
        double tz = Mth.lerp(partialTicks, target.zo, target.getZ());

        double dx = tx - cameraPos.x;
        double dy = ty - cameraPos.y;
        double dz = tz - cameraPos.z;
        double distSq = dx * dx + dy * dy + dz * dz;

        double renderX = tx;
        double renderY = ty;
        double renderZ = tz;

        double maxRenderDistSq = 150.0 * 150.0;
        if (distSq > maxRenderDistSq) {
            double dist = Math.sqrt(distSq);
            double ratio = 150.0 / dist;
            renderX = cameraPos.x + dx * ratio;
            renderY = cameraPos.y + dy * ratio;
            renderZ = cameraPos.z + dz * ratio;
        }
        
        poseStack.pushPose();
        poseStack.setIdentity();
        poseStack.mulPose(modelViewMatrix);
        poseStack.translate(renderX - cameraPos.x, renderY - cameraPos.y + target.getBbHeight() + 2.0, renderZ - cameraPos.z);
        
        float yawRad = (float) Math.toRadians(-camera.getYRot());
        Quaternionf yawQuat = new Quaternionf().fromAxisAngleRad(0.0f, 1.0f, 0.0f, yawRad);
        poseStack.mulPose(yawQuat);

        float pitchRad = (float) Math.toRadians(camera.getXRot());
        Quaternionf pitchQuat = new Quaternionf().fromAxisAngleRad(1.0f, 0.0f, 0.0f, pitchRad);
        poseStack.mulPose(pitchQuat);

        float bob = Mth.sin((client.level.getGameTime() + partialTicks) * 0.2f) * 0.2f;
        poseStack.translate(0, bob, 0);

        float scale = 0.1f;
        poseStack.scale(-scale, -scale, scale);

        String arrow = "▼";
        Font font = client.font;
        float textWidth = (float)(-font.width(arrow) / 2);
        Matrix4f matrix = poseStack.last().pose();
        
        font.drawInBatch(arrow, textWidth, 0, 0xFFFF0000, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, 15728880);
        
        poseStack.popPose();
    }

    public static void setPreviewWaypoint(Waypoint waypoint) {
        previewWaypoint = waypoint;
        if (waypoint != null) {
            updateCaches(waypoint);
        }
    }

    public static Waypoint getPreviewWaypoint() {
        return previewWaypoint;
    }

    private static void renderWaypointText(Waypoint waypoint, PoseStack poseStack, Matrix4f modelViewMatrix, MultiBufferSource bufferSource, Camera camera, float partialTicks) {
        Minecraft client = Minecraft.getInstance();
        Vec3 cameraPos = camera.getPosition();
        Font font = client.font;
        
        double wx = waypoint.pos().x;
        double wy = waypoint.pos().y + 1.5;
        double wz = waypoint.pos().z;

        double dx = wx - cameraPos.x;
        double dy = wy - cameraPos.y;
        double dz = wz - cameraPos.z;
        double distSq = dx * dx + dy * dy + dz * dz;
        double dist = Math.sqrt(distSq);
        
        float alpha = 0.95f;
        int alphaByte = (int)(alpha * 255.0f);
        int baseColor = (alphaByte << 24) | 0x00FFFFFF;

        TEXT_BUILDER.setLength(0);
        
        String cachedLabel = LABEL_CACHE.getOrDefault(waypoint.id(), "");
        TEXT_BUILDER.append(cachedLabel);
        if (waypoint.playerName() != null && !waypoint.playerName().isEmpty() && waypoint.health() >= 0) {
            String healthColor = waypoint.health() > 10 ? "§a" : (waypoint.health() > 5 ? "§e" : "§c");
            TEXT_BUILDER.append(" ").append(healthColor).append((int)waypoint.health()).append(" HP");
        }

        boolean isMeteor = waypoint.id().startsWith("meteor_");
        boolean isPing = waypoint.id().startsWith("ping_");
        boolean isShared = waypoint.id().startsWith("shared_");
        
        if (isMeteor || isPing || isShared || waypoint == previewWaypoint) {
            TEXT_BUILDER.append(LOCATION_CACHE.getOrDefault(waypoint.id(), ""));
        }
        
        TEXT_BUILDER.append("\n§f").append((int)dist).append("m Away");

        if (waypoint.expirationTime() > 0) {
            long remainingMs = waypoint.expirationTime() - System.currentTimeMillis();
            if (remainingMs > 0) {
                long seconds = (remainingMs / 1000) % 60;
                long minutes = (remainingMs / (1000 * 60));
                TEXT_BUILDER.append("\n§e");
                if (isMeteor) {
                    TEXT_BUILDER.append("Duration: ");
                }
                if (minutes < 10) TEXT_BUILDER.append('0');
                TEXT_BUILDER.append(minutes).append(':');
                if (seconds < 10) TEXT_BUILDER.append('0');
                TEXT_BUILDER.append(seconds);
            }
        }

        double renderX = wx;
        double renderY = wy + 1.0;
        double renderZ = wz;

        double maxRenderDist = 150.0; 
        if (dist > maxRenderDist) {
            double ratio = maxRenderDist / dist;
            renderX = cameraPos.x + (wx - cameraPos.x) * ratio;
            renderY = cameraPos.y + (wy - cameraPos.y) * ratio;
            renderZ = cameraPos.z + (wz - cameraPos.z) * ratio;
        }

        renderBillboardText(poseStack, modelViewMatrix, bufferSource, camera, font, TEXT_BUILDER.toString(), renderX, renderY, renderZ, baseColor, scaleForDistance(dist));
    }

    private static float scaleForDistance(double dist) {
        float base = 0.025f;
        double factor = Math.min(30.0, 1.0 + dist / 10.0);
        return (float)(base * factor);
    }

    private static void renderBillboardText(PoseStack poseStack, Matrix4f modelViewMatrix, MultiBufferSource bufferSource, Camera camera, Font font, String text, double worldX, double worldY, double worldZ, int argb, float scale) {
        Vec3 cameraPos = camera.getPosition();
        double relX = worldX - cameraPos.x;
        double relY = worldY - cameraPos.y;
        double relZ = worldZ - cameraPos.z;

        poseStack.pushPose();
        poseStack.setIdentity();
        poseStack.mulPose(modelViewMatrix);
        poseStack.translate(relX, relY, relZ);

        float yawRad = (float) Math.toRadians(-camera.getYRot());
        Quaternionf yawQuat = new Quaternionf().fromAxisAngleRad(0.0f, 1.0f, 0.0f, yawRad);
        poseStack.mulPose(yawQuat);

        poseStack.scale(-scale, -scale, scale);

        Matrix4f matrix = poseStack.last().pose();
        
        String[] lines = text.split("\n");
        float yOffset = -(lines.length * font.lineHeight) / 2.0f;
        
        for (String line : lines) {
            float textWidth = (float)(-font.width(line) / 2);
            font.drawInBatch(line, textWidth, yOffset, argb, true, matrix, bufferSource, Font.DisplayMode.SEE_THROUGH, 0, 15728880);
            yOffset += font.lineHeight;
        }

        poseStack.popPose();
    }

    private static void renderWaypointBeam(Waypoint waypoint, PoseStack poseStack, Matrix4f modelViewMatrix, MultiBufferSource bufferSource, long gameTime, float partialTicks, Camera camera) {
        Vec3 pos = waypoint.pos();
        Vec3 cameraPos = camera.getPosition();

        double wx = pos.x;
        double wy = pos.y;
        double wz = pos.z;
        
        double dx = wx - cameraPos.x;
        double dy = wy - cameraPos.y;
        double dz = wz - cameraPos.z;
        double distSq = dx * dx + dy * dy + dz * dz;

        double renderX = wx;
        double renderY = wy;
        double renderZ = wz;

        double maxRenderDistSq = 150.0 * 150.0;
        if (distSq > maxRenderDistSq) {
            double dist = Math.sqrt(distSq);
            double ratio = 150.0 / dist;
            renderX = cameraPos.x + dx * ratio;
            renderY = cameraPos.y + dy * ratio;
            renderZ = cameraPos.z + dz * ratio;
        }

        poseStack.pushPose();
        poseStack.setIdentity();
        poseStack.mulPose(modelViewMatrix);
        poseStack.translate(renderX - cameraPos.x, renderY - cameraPos.y, renderZ - cameraPos.z);
        
        int colorInt = waypoint.color().getRGB();
        int semiTransparentColor = (colorInt & 0x00FFFFFF) | 0x80000000;

        VertexConsumer beaconConsumer = bufferSource.getBuffer(GuardBeaconRenderType.INSTANCE.getBEACON());

        BeaconRenderer.renderBeaconBeam(
            poseStack, 
            (rt) -> beaconConsumer, 
            BeaconRenderer.BEAM_LOCATION, 
            partialTicks, 
            1.0f, 
            gameTime, 
            0, 
            256, 
            semiTransparentColor,
            0.3f, 
            0.35f 
        );

        BeaconRenderer.renderBeaconBeam(
            poseStack,
            (rt) -> beaconConsumer,
            BeaconRenderer.BEAM_LOCATION,
            partialTicks,
            1.0f,
            gameTime,
            -256, 
            256, 
            semiTransparentColor,
            0.25f, 
            0.2f  
        );
        
        poseStack.popPose();
    }
}
