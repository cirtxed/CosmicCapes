package dev.cosmicmod.client;

public class ChatStateManager {
    private static String currentServer = "";
    private static String activeChannel = "None";
    private static boolean switchingChannelForPing = false;
    private static String pendingPingMessage = null;
    private static String lastSentFixCommand = null;
    private static String lastAttemptedPing = null;
    private static long lastPearlThrowTime = 0;
    private static long lastRightClickTime = 0;

    public static void setLastRightClickTime(long time) {
        lastRightClickTime = time;
    }

    public static long getLastRightClickTime() {
        return lastRightClickTime;
    }

    public static void setLastPearlThrowTime(long time) {
        lastPearlThrowTime = time;
    }

    public static long getLastPearlThrowTime() {
        return lastPearlThrowTime;
    }

    public static void setLastAttemptedPing(String message) {
        lastAttemptedPing = message;
    }

    public static String getLastAttemptedPing() {
        return lastAttemptedPing;
    }

    public static void setLastSentFixCommand(String command) {
        lastSentFixCommand = command;
    }

    public static String getLastSentFixCommand() {
        return lastSentFixCommand;
    }

    public static void setSwitchingChannelForPing(boolean switching, String message) {
        switchingChannelForPing = switching;
        pendingPingMessage = message;
    }

    public static boolean isSwitchingChannelForPing() {
        return switchingChannelForPing;
    }

    public static String getPendingPingMessage() {
        return pendingPingMessage;
    }

    public static void setCurrentServer(String server) {
        currentServer = server != null ? server.toLowerCase() : "";
        activeChannel = "None";
    }

    public static String getCurrentServer() {
        return currentServer;
    }

    public static void setActiveChannel(String channel) {
        activeChannel = channel;
        CosmicmodClient.LOGGER.info("Active channel updated to: " + channel);
    }

    public static String getActiveChannel() {
        return activeChannel;
    }

    public static boolean isCosmicSky() {
        return currentServer.contains("cosmicsky");
    }

    public static boolean isCosmicPrisons() {
        return currentServer.contains("cosmicprisons");
    }
}
