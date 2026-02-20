package dev.cosmicmod.client.notification;

import net.minecraft.world.item.ItemStack;

public class Notification {
    private final String text;
    private final ItemStack icon;
    private final long duration;
    private final long startTime;

    public Notification(String text, ItemStack icon, long durationMillis) {
        this.text = text;
        this.icon = icon;
        this.duration = durationMillis;
        this.startTime = System.currentTimeMillis();
    }

    public String getText() {
        return text;
    }

    public ItemStack getIcon() {
        return icon;
    }

    public long getDuration() {
        return duration;
    }

    public long getStartTime() {
        return startTime;
    }

    public float getProgress() {
        long elapsed = System.currentTimeMillis() - startTime;
        return Math.min(1.0f, (float) elapsed / duration);
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - startTime > duration;
    }
}
