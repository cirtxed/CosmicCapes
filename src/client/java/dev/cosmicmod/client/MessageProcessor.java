package dev.cosmicmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

import net.minecraft.world.phys.Vec3;
import dev.cosmicmod.client.waypoint.Waypoint;
import dev.cosmicmod.client.waypoint.WaypointManager;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageProcessor {
    private static final Pattern PING_PATTERN = Pattern.compile("\\[!\\] (?:Alliance )?(\\w+) has pinged at (-?\\d+)x (-?\\d+)y (-?\\d+)z(?: ([\\w-]+))? \\| HP: (\\d+)/(\\d+) \\| Facing: (\\w+)");
    private static final Pattern SHARE_PATTERN = Pattern.compile("\\[!\\] Waypoint: (.+) at (-?\\d+)x (-?\\d+)y (-?\\d+)z in ([\\w-]+)");
    private static final Pattern METEOR_PATTERN = Pattern.compile("(?is)\\(!\\) A meteor (?:summoned by .+ )?is falling from the sky at:\\s*(-?\\d+)x,?\\s+(-?\\d+)y,?\\s+(-?\\d+)z");
    private static final Pattern ANNOUNCEMENT_PATTERN = Pattern.compile("(?i)\\(!\\) A meteor (?:summoned by .+ )?is falling from the sky at:");
    private static final Pattern COORDINATES_PATTERN = Pattern.compile("(?i)^\\s*(-?\\d+)x,?\\s+(-?\\d+)y,?\\s+(-?\\d+)z");
    
    private static long lastMeteorAnnouncementTime = 0;
    private static final long METEOR_ANNOUNCEMENT_TIMEOUT = 5000;

    private static boolean recordingLootbox = false;
    private static boolean isInternalMessage = false;
    private static String openingPlayer = "";
    private static String lootboxName = "";
    private static final List<Component> collectedRewards = new ArrayList<>();

    public static boolean processChatMessage(Component message) {
        if (isInternalMessage) {
            return true;
        }

        String text = message.getString();
        String cleanText = text.replaceAll("§[0-9a-fk-or]", "");

        if (cleanText.contains("(!) You have been healed!")) {
             CosmicmodClient.getCooldownHud().startCooldown("Heal", 10 * 60 * 1000); // Default to longest if not from command
        }

        if (cleanText.contains("(!) Near")) {
            CosmicmodClient.getCooldownHud().startCooldown("Near", 30000);
        }

        if (cleanText.contains("(!) You must wait") && cleanText.contains("before using Powerball again!")) {
            try {
                String secondsStr = cleanText.split("wait ")[1].split("s ")[0];
                float seconds = Float.parseFloat(secondsStr);
                CosmicmodClient.getCooldownHud().startCooldown("Powerball", (long)(seconds * 1000));
            } catch (Exception ignored) {}
        }

        if (cleanText.contains("(!) Please wait") && cleanText.contains("before throwing another Ender Pearl!")) {
            long lastThrow = ChatStateManager.getLastPearlThrowTime();
            if (lastThrow > 0) {
                // Restore the cooldown based on the time remaining before this failed throw attempt
                // Or just don't reset it if we didn't start a new one, but our mixin starts it on HEAD.
                // So we should revert to the state before lastPearlThrowTime.
                // Actually, the easiest is to just subtract the duration we just added if we can,
                // but CooldownHud doesn't keep history.
                // However, we can use the message to set the correct remaining time.
                // "(!) Please wait 12.4s before throwing another Ender Pearl!"
                try {
                    String secondsStr = cleanText.split("wait ")[1].split("s ")[0];
                    float seconds = Float.parseFloat(secondsStr);
                    CosmicmodClient.getCooldownHud().startCooldown("Pearl", (long)(seconds * 1000));
                } catch (Exception e) {
                    // Fallback: just remove it if we can't parse, or leave it. 
                    // Removal might be better than a fresh 15s.
                }
            }
        }

        if (cleanText.contains("(!) You must be holding an item to repair it.") || cleanText.contains("(!) You must hold a valid damaged item you wish to repair in your hand!")) {
            CosmicmodClient.getCooldownHud().removeCooldown("Fix");
        } else if (cleanText.contains("(!) Nothing found to repair!")) {
            String lastFix = ChatStateManager.getLastSentFixCommand();
            if ("fix all".equals(lastFix)) {
                CosmicmodClient.getCooldownHud().removeCooldown("Fix All");
            } else {
                CosmicmodClient.getCooldownHud().removeCooldown("Fix");
            }
        }

        // Meteor detection
        if (CosmicmodClient.getWaypointHud().isEnabled() && CosmicmodClient.getWaypointHud().getSettings().showMeteorWaypoints) {
            Matcher fullMatcher = METEOR_PATTERN.matcher(cleanText);
            if (fullMatcher.find()) {
                processMeteorMatch(fullMatcher);
                lastMeteorAnnouncementTime = 0;
            } else if (ANNOUNCEMENT_PATTERN.matcher(cleanText).find()) {
                lastMeteorAnnouncementTime = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - lastMeteorAnnouncementTime < METEOR_ANNOUNCEMENT_TIMEOUT) {
                Matcher coordMatcher = COORDINATES_PATTERN.matcher(cleanText);
                if (coordMatcher.find()) {
                    processMeteorMatch(coordMatcher);
                    lastMeteorAnnouncementTime = 0;
                }
            }
        }

        Matcher pingMatcher = PING_PATTERN.matcher(cleanText);
        if (pingMatcher.find()) {
            String playerName = pingMatcher.group(1);
            if (ChatStateManager.isCosmicPrisons() && playerName.contains("ip")) {
                playerName = playerName.replace("ip", "");
            }
            
            double x = Double.parseDouble(pingMatcher.group(2));
            double y = Double.parseDouble(pingMatcher.group(3));
            double z = Double.parseDouble(pingMatcher.group(4));
            String worldName = pingMatcher.group(5);
            float hp = Float.parseFloat(pingMatcher.group(6));
            float maxHp = Float.parseFloat(pingMatcher.group(7));
            String direction = pingMatcher.group(8);

            // Log for debugging
            CosmicmodClient.LOGGER.info("[DEBUG] Parsed ping from player: '{}', world: '{}', pos: {} {} {}", playerName, worldName, x, y, z);
            
            // Check if world is unknown or mismatched
            Minecraft client = Minecraft.getInstance();
            if (client.level != null) {
                String currentWorld = CosmicmodClient.getWorldDisplayName(client.level.dimension().location().getPath());
                if (worldName != null && !worldName.isEmpty() && currentWorld != null && !currentWorld.equalsIgnoreCase(worldName)) {
                    CosmicmodClient.LOGGER.warn("[DEBUG] Ping world mismatch! Current: '{}', Ping: '{}'", currentWorld, worldName);
                }
            }

            String label = playerName + " (" + direction + ")";
            if (worldName != null && !worldName.isEmpty()) {
                label += " [" + worldName + "]";
            }

            Waypoint waypoint = new Waypoint(
                    "ping_" + playerName,
                    new Vec3(x, y, z),
                    Component.literal(label),
                    Color.RED,
                    System.currentTimeMillis() + (CosmicConfig.getInstance().pingDuration * 1000L),
                    playerName,
                    hp,
                    false,
                    worldName,
                    true,
                    ChatStateManager.getCurrentServer()
            );
            WaypointManager.addWaypoint(waypoint);
            
            if (client.level != null && client.player != null) {
                client.level.playSound(client.player, client.player.getX(), client.player.getY(), client.player.getZ(), 
                    SoundEvents.NOTE_BLOCK_BELL, SoundSource.PLAYERS, 1.0f, 1.0f);
            }

            return true;
        }

        Matcher shareMatcher = SHARE_PATTERN.matcher(cleanText);
        if (shareMatcher.find()) {
            String name = shareMatcher.group(1);
            double x = Double.parseDouble(shareMatcher.group(2));
            double y = Double.parseDouble(shareMatcher.group(3));
            double z = Double.parseDouble(shareMatcher.group(4));
            String worldName = shareMatcher.group(5);

            Waypoint waypoint = new Waypoint(
                    "shared_" + name + "_" + System.currentTimeMillis(),
                    new Vec3(x, y, z),
                    Component.literal(name),
                    Color.CYAN,
                    0,
                    null,
                    0,
                    false,
                    worldName,
                    true,
                    ChatStateManager.getCurrentServer()
            );
            WaypointManager.addWaypoint(waypoint);
            return true;
        }

        if (ChatStateManager.isSwitchingChannelForPing()) {
            CosmicmodClient.LOGGER.info("[DEBUG] Message while switching: {}", text);
            if (cleanText.contains("(!) Your gang does not have any active truces")) {
                CosmicmodClient.LOGGER.info("[DEBUG] No truces, switching to gang chat");

                new Thread(() -> {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Minecraft client = Minecraft.getInstance();
                    if (client.player != null && client.player.connection != null) {
                        client.player.connection.sendCommand("gang chat gang");
                    }
                }).start();
                return false;
            } else if (cleanText.contains("(!) You must be in a gang to use gang chat")) {
                CosmicmodClient.LOGGER.info("[DEBUG] Not in a gang, stopping switch");
                ChatStateManager.setSwitchingChannelForPing(false, null);
                return false;
            } else if (cleanText.contains("(!) You are now speaking in truce chat.") || cleanText.contains("(!) You are now speaking in gang chat.") || cleanText.contains("New Active Chat Channel: Alliance") || cleanText.contains("(!) You are already in that chat channel.")) {
                CosmicmodClient.LOGGER.info("[DEBUG] Switch confirmation received");
                if (ChatStateManager.isCosmicPrisons()) {
                    if (cleanText.contains("(!) You are now speaking in truce chat.")) {
                        ChatStateManager.setActiveChannel("Truce");
                    } else if (cleanText.contains("(!) You are now speaking in gang chat.") || cleanText.contains("(!) You are already in that chat channel.")) {
                        ChatStateManager.setActiveChannel("Gang");
                    }
                } else if (ChatStateManager.isCosmicSky()) {
                    if (cleanText.contains("New Active Chat Channel: Alliance")) {
                        ChatStateManager.setActiveChannel("Alliance");
                    }
                }

                String pendingMsg = ChatStateManager.getPendingPingMessage();
                if (pendingMsg != null) {
                    CosmicmodClient.LOGGER.info("[DEBUG] Sending pending ping: {}", pendingMsg);
                    Minecraft.getInstance().player.connection.sendChat(pendingMsg);
                    Minecraft client = Minecraft.getInstance();
                    if (client.level != null && client.player != null) {
                        client.level.playSound(client.player, client.player.getX(), client.player.getY(), client.player.getZ(), 
                            SoundEvents.NOTE_BLOCK_PLING, SoundSource.PLAYERS, 1.0f, 1.0f);
                    }
                } else {
                    CosmicmodClient.LOGGER.info("[DEBUG] No pending ping message found!");
                }
                ChatStateManager.setSwitchingChannelForPing(false, null);
                return false;
            }
        } else if (ChatStateManager.isCosmicPrisons() && cleanText.contains("(!) Your gang does not have any active truces")) {
            CosmicmodClient.LOGGER.info("[DEBUG] Unexpected 'no truces' error while NOT in switching state");
            // Error occurred but we weren't in switching state - probably state mismatch
            String lastPing = ChatStateManager.getLastAttemptedPing();
            if (lastPing != null) {
                CosmicmodClient.LOGGER.info("[DEBUG] Retrying ping via gang chat switch. Last ping: {}", lastPing);
                ChatStateManager.setSwitchingChannelForPing(true, lastPing);
                new Thread(() -> {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Minecraft client = Minecraft.getInstance();
                    if (client.player != null && client.player.connection != null) {
                        client.player.connection.sendCommand("gang chat gang");
                    }
                }).start();
                return false;
            } else {
                CosmicmodClient.LOGGER.info("[DEBUG] No last attempted ping to retry");
            }
        }

        if (ChatStateManager.isCosmicSky()) {
            if (cleanText.startsWith("New Active Chat Channel: ")) {
                String channel = cleanText.substring("New Active Chat Channel: ".length()).trim();
                ChatStateManager.setActiveChannel(channel);
            }
        } else if (ChatStateManager.isCosmicPrisons()) {
            if (cleanText.contains("(!) You are now speaking in truce chat.")) {
                ChatStateManager.setActiveChannel("Truce");
            } else if (cleanText.contains("(!) You are now speaking in gang chat.")) {
                ChatStateManager.setActiveChannel("Gang");
            } else if (cleanText.contains("(!) You are now speaking in global chat.")) {
                ChatStateManager.setActiveChannel("Global");
            }
        }

        // Lootbox processing
        // Example: §6§l(!) §6Woofless opened a §f§lLootbox: §c§lUnification§6
        if (CosmicConfig.getInstance().chatFilterEnabled && text.contains("§6§l(!) §6") && text.contains(" opened a ")) {
            recordingLootbox = true;
            collectedRewards.clear();

            int nameStart = text.indexOf("§6", text.indexOf("(!) ")) + 6; // Skip (!) §6
            int nameEnd = text.indexOf(" opened a");
            if (nameStart < nameEnd && nameStart > 0) {
                openingPlayer = text.substring(nameStart, nameEnd);
            } else {
                openingPlayer = "Someone";
            }

            int itemStart = text.indexOf(" opened a ") + 10;
            lootboxName = text.substring(itemStart).trim();

            CosmicmodClient.LOGGER.info("[CHAT] {}", text);
            return false; // Cancel original message
        }

        if (recordingLootbox) {
            String rawText = text.replaceAll("§[0-9a-fk-or]", "").trim();
            if (rawText.isEmpty()) {
                // End of lootbox message reached
                renderCondensedLootbox();
                recordingLootbox = false;
                CosmicmodClient.LOGGER.info("[CHAT] {}", text);
                return false; // Cancel the blank line too
            } else {
                // Buffer the reward line
                collectedRewards.add(message);
                CosmicmodClient.LOGGER.info("[CHAT] {}", text);
                return false; // Cancel original reward message
            }
        }

        return true;
    }

    private static void renderCondensedLootbox() {
        if (openingPlayer.isEmpty()) return;

        // Construct the hover text from collected components
        MutableComponent hoverText = Component.empty();
        for (int i = 0; i < collectedRewards.size(); i++) {
            hoverText.append(collectedRewards.get(i));
            if (i < collectedRewards.size() - 1) {
                hoverText.append(Component.literal("\n"));
            }
        }

        // Create the final message: §6[NAME has opened a: item name]
        MutableComponent finalMessage = Component.literal("§6[" + openingPlayer + " has opened a: ")
                .append(Component.literal(lootboxName))
                .append(Component.literal("§6]"))
                .withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText)));

        // Send to client chat
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            isInternalMessage = true;
            try {
                client.player.displayClientMessage(finalMessage, false);
            } finally {
                isInternalMessage = false;
            }
            CosmicmodClient.LOGGER.info("[CHAT] {}", finalMessage.getString());
        }
    }

    private static void processMeteorMatch(Matcher matcher) {
        try {
            double x = Double.parseDouble(matcher.group(1));
            double y = Double.parseDouble(matcher.group(2));
            double z = Double.parseDouble(matcher.group(3));

            int duration = CosmicmodClient.getWaypointHud().getSettings().meteorDuration;

            Waypoint waypoint = new Waypoint(
                    "meteor_" + System.currentTimeMillis(),
                    new Vec3(x, y, z),
                    Component.literal("§6§lMETEOR"),
                    new Color(255, 170, 0),
                    System.currentTimeMillis() + (duration * 1000L),
                    null,
                    0,
                    false,
                    null, // World unknown from message
                    true,
                    ChatStateManager.getCurrentServer()
            );
            WaypointManager.addWaypoint(waypoint);
        } catch (Exception e) {
            CosmicmodClient.LOGGER.error("Failed to parse meteor coordinates", e);
        }
    }
}
