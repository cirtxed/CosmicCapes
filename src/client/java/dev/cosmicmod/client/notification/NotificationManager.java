package dev.cosmicmod.client.notification;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NotificationManager {
    private static final List<Notification> notifications = new CopyOnWriteArrayList<>();

    public static void addNotification(Notification notification) {
        notifications.add(notification);
    }

    public static List<Notification> getNotifications() {
        return notifications;
    }

    public static void tick() {
        notifications.removeIf(Notification::isExpired);
    }
}
