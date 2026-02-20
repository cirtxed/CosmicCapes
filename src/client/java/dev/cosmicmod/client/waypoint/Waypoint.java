package dev.cosmicmod.client.waypoint;

import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import java.awt.Color;

public record Waypoint(
    String id,
    Vec3 pos,
    Component label,
    Color color,
    long expirationTime, // System.currentTimeMillis(), 0 for permanent
    String playerName,
    float health,
    boolean isFocus,
    String world,
    boolean visible,
    String server
) {
    public boolean isExpired() {
        return expirationTime > 0 && System.currentTimeMillis() > expirationTime;
    }

    public Waypoint withVisible(boolean visible) {
        return new Waypoint(id, pos, label, color, expirationTime, playerName, health, isFocus, world, visible, server);
    }
}
