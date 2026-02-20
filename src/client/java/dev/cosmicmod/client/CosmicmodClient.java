package dev.cosmicmod.client;

import dev.cosmicmod.client.hud.CooldownHud;
import dev.cosmicmod.client.hud.PlayerListHud;
import dev.cosmicmod.client.hud.HudManager;
import dev.cosmicmod.client.particle.MoneyBagParticle;
import dev.cosmicmod.client.waypoint.WaypointManager;
import dev.cosmicmod.client.path.PathManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.lwjgl.glfw.GLFW;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID;

public class CosmicmodClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("cosmicmod");
    private static CooldownHud cooldownHud;
    private static PlayerListHud playerListHud;
    private static dev.cosmicmod.client.hud.ChatFilterHud chatFilterHud;
    private static dev.cosmicmod.client.hud.WaypointHud waypointHud;
    private static dev.cosmicmod.client.hud.NotificationHud notificationHud;
    private static dev.cosmicmod.client.hud.ItemOverlayHud itemOverlayHud;
    private static dev.cosmicmod.client.hud.SearchHud searchHud;
    public static KeyMapping pingKey;
    public static KeyMapping configKey;
    public static KeyMapping waypointKey;
    public static KeyMapping toggleWaypointsKey;
    public static KeyMapping hudEditorKey;
    private static boolean wasPingPressed = false;

    public static final SimpleParticleType MONEY_BAG_PARTICLE = FabricParticleTypes.simple();
    private static final java.util.List<UUID> TARGET_UUIDS = java.util.List.of(
            UUID.fromString("dce48f53-8c32-469f-b7fb-dc11e612d0f7"),
            UUID.fromString("09a8c489-1fb9-4617-a2bf-16adad508254")
    );

    private static dev.cosmicmod.client.hud.HandRescalerHud handRescalerHud;

    public static dev.cosmicmod.client.hud.HandRescalerHud getHandRescalerHud() {
        return handRescalerHud;
    }

    public static CooldownHud getCooldownHud() {
        return cooldownHud;
    }

    public static dev.cosmicmod.client.hud.ItemOverlayHud getItemOverlayHud() {
        return itemOverlayHud;
    }

    public static dev.cosmicmod.client.hud.SearchHud getSearchHud() {
        return searchHud;
    }

    public static dev.cosmicmod.client.hud.ChatFilterHud getChatFilterHud() {
        return chatFilterHud;
    }

    public static dev.cosmicmod.client.hud.WaypointHud getWaypointHud() {
        return waypointHud;
    }

    public static dev.cosmicmod.client.hud.NotificationHud getNotificationHud() {
        return notificationHud;
    }

    public static String getWorldDisplayName(String worldKey) {
        if (worldKey == null) return null;
        String lowerKey = worldKey.toLowerCase();
        if (lowerKey.contains("skyblock_world")) {
            return "Skyblock-Island";
        }
        return switch (lowerKey) {
            case "overworld" -> "Spawn";
            case "realm_lake" -> "Lake-Realm";
            case "adventure_ruins-0" -> "Abandoned-Ruins";
            case "adventure_wasteland" -> "Lost-Wasteland";
            case "adventure_demonic_realm" -> "Demonic-Realm";
            case "koth_world" -> "Koth";
            case "adventure_abyss" -> "Abyss";
            case "outpost_stone" -> "Stone-Outpost";
            case "outpost_iron" -> "Iron-Outpost";
            case "outpost_diamond" -> "Diamond-Outpost";
            case "adventure_ruins_facility" -> "Chain-Facility";
            case "adventure_wasteland_facility" -> "Iron-Facility";
            case "adventure_demonic_realm_facility" -> "Diamond-Facility";
            case "world_lms" -> "LMS";
            case "world" -> "Overworld";
            case "cell_plot_citizen" -> "Citizen-Plots";
            case "cell_plot_merchant" -> "Merchant-Plots";
            case "cell_plot_king" -> "King-Plots";
            default -> null;
        };
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing CosmicMod...");
        System.out.println("[CosmicMod] Mod initialized successfully!");
        CosmeticManager.init();
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, ResourceLocation.fromNamespaceAndPath("cosmicmod", "money_bag"), MONEY_BAG_PARTICLE);
        ParticleFactoryRegistry.getInstance().register(MONEY_BAG_PARTICLE, MoneyBagParticle.Provider::new);

        CosmicmodClientCommands.register();
        cooldownHud = new CooldownHud();
        HudManager.register(cooldownHud);
        playerListHud = new PlayerListHud();
        HudManager.register(playerListHud);
        chatFilterHud = new dev.cosmicmod.client.hud.ChatFilterHud();
        HudManager.register(chatFilterHud);
        HudManager.register(new dev.cosmicmod.client.hud.DamageIndicatorHud());
        HudManager.register(new dev.cosmicmod.client.hud.PingHud());
        waypointHud = new dev.cosmicmod.client.hud.WaypointHud();
        HudManager.register(waypointHud);

        notificationHud = new dev.cosmicmod.client.hud.NotificationHud();
        HudManager.register(notificationHud);
        itemOverlayHud = new dev.cosmicmod.client.hud.ItemOverlayHud();
        HudManager.register(itemOverlayHud);
        searchHud = new dev.cosmicmod.client.hud.SearchHud();
        HudManager.register(searchHud);
        HudManager.register(new dev.cosmicmod.client.hud.ClueScrollHud());
        HudManager.register(new dev.cosmicmod.client.hud.SatchelHud());
        handRescalerHud = new dev.cosmicmod.client.hud.HandRescalerHud();
        HudManager.register(handRescalerHud);


        pingKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.cosmicmod.ping",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "category.cosmicmod"
        ));

        configKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.cosmicmod.config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                "category.cosmicmod"
        ));

        waypointKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.cosmicmod.waypoint",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                "category.cosmicmod"
        ));

        toggleWaypointsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.cosmicmod.toggle_waypoints",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.cosmicmod"
        ));

        hudEditorKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.cosmicmod.hud_editor",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.cosmicmod"
        ));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // Server address will be set via Mixin or by checking handler
            LOGGER.info("Joined server, checking address...");
            WaypointManager.loadWaypoints();
            
            // Show welcome message
            dev.cosmicmod.client.notification.NotificationManager.addNotification(
                new dev.cosmicmod.client.notification.Notification(
                    "Welcome to CosmicMod!",
                    new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHER_STAR),
                    5000
                )
            );
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ChatStateManager.setCurrentServer("");
            WaypointManager.loadWaypoints();
        });

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            return MessageProcessor.processChatMessage(message);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            HudManager.tick();
            WaypointManager.tick();
            PathManager.tick();
            dev.cosmicmod.client.feature.MiningRecapManager.tick();

            if (client.level != null) {
                // Find the player with the target UUID in the current level
                for (AbstractClientPlayer player : client.level.players()) {
                    // Money Bag for TARGET_UUID
                    if (TARGET_UUIDS.contains(player.getUUID())) {
                        // Spawn a particle every few ticks or based on a random chance
                        if (client.level.random.nextFloat() < 0.15f) {
                            double x = player.getX() + (client.level.random.nextDouble() - 0.5) * 1.5;
                            double y = player.getY() + 1.5 + (client.level.random.nextDouble() - 0.5);
                            double z = player.getZ() + (client.level.random.nextDouble() - 0.5) * 1.5;

                            client.level.addParticle(MONEY_BAG_PARTICLE, x, y, z, 0, 0, 0);
                        }
                    }

                    // Player Auras (Particle effect around the player)
                    if (CosmicConfig.getInstance().showAuras) {
                        net.minecraft.core.particles.ParticleOptions aura = CosmeticManager.getAura(player.getUUID());
                        if (aura == null && player == client.player) {
                            aura = net.minecraft.core.particles.ParticleTypes.END_ROD;
                        }

                        if (aura != null && client.level.random.nextFloat() < 0.3f) {
                            double angle = client.level.random.nextDouble() * Math.PI * 2;
                            double radius = 0.6;
                            double x = player.getX() + Math.cos(angle) * radius;
                            double z = player.getZ() + Math.sin(angle) * radius;
                            double y = player.getY() + client.level.random.nextDouble() * 2.0;

                            client.level.addParticle(aura, x, y, z, 0, 0.05, 0);
                        }
                    }
                }
            }

            if (client.player != null) {
                while (waypointKey.consumeClick()) {
                    client.setScreen(new dev.cosmicmod.client.gui.CreateWaypointScreen(null));
                }

                while (toggleWaypointsKey.consumeClick()) {
                    WaypointManager.toggleAllWaypoints();
                }

                while (hudEditorKey.consumeClick()) {
                    client.setScreen(new dev.cosmicmod.client.gui.HudEditorScreen(null));
                }

                while (configKey.consumeClick()) {
                    client.setScreen(new dev.cosmicmod.client.gui.ModsScreen(null));
                }

                boolean isPressed = pingKey.isDown();
                if (isPressed) {
                    // Raycast to find where player is looking
                    Vec3 start = client.player.getEyePosition(1.0f);
                    Vec3 end = start.add(client.player.getViewVector(1.0f).scale(100.0));
                    BlockHitResult hit = client.level.clip(new ClipContext(start, end, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, client.player));

                    if (hit.getType() != HitResult.Type.MISS) {
                        Vec3 pos = hit.getLocation();
                        String worldKey = client.level.dimension().location().getPath();
                        String worldName = getWorldDisplayName(worldKey);
                        
                        dev.cosmicmod.client.waypoint.Waypoint preview = new dev.cosmicmod.client.waypoint.Waypoint(
                                "preview",
                                pos,
                                net.minecraft.network.chat.Component.literal("Ping Preview"),
                                java.awt.Color.YELLOW,
                                0,
                                client.player.getName().getString(),
                                client.player.getHealth(),
                                false,
                                worldName,
                                true,
                                ChatStateManager.getCurrentServer()
                        );
                        WaypointManager.setPreviewWaypoint(preview);
                    } else {
                        WaypointManager.setPreviewWaypoint(null);
                    }
                } else if (wasPingPressed) {
                    // Released
                    dev.cosmicmod.client.waypoint.Waypoint preview = WaypointManager.getPreviewWaypoint();
                    if (preview != null) {
                        Direction dir = client.player.getDirection();
                        
                        String worldKey = client.level.dimension().location().getPath();
                        String worldName = getWorldDisplayName(worldKey);

                        String playerName = client.player.getName().getString();
                        if (ChatStateManager.isCosmicPrisons() && playerName.contains("ip")) {
                            playerName = playerName.replace("ip", "");
                        }

                        String displayName = playerName;
                        if (ChatStateManager.isCosmicSky()) {
                            displayName = "Alliance " + playerName;
                        }

                        String msg;
                        if (worldName != null) {
                            msg = String.format("[!] %s has pinged at %dx %dy %dz %s | HP: %d/%d | Facing: %s",
                                    displayName,
                                    (int) preview.pos().x, (int) preview.pos().y, (int) preview.pos().z,
                                    worldName,
                                    (int) client.player.getHealth(), (int) client.player.getMaxHealth(),
                                    dir.getName().substring(0, 1).toUpperCase() + dir.getName().substring(1).toLowerCase());
                        } else {
                            msg = String.format("[!] %s has pinged at %dx %dy %dz | HP: %d/%d | Facing: %s",
                                    displayName,
                                    (int) preview.pos().x, (int) preview.pos().y, (int) preview.pos().z,
                                    (int) client.player.getHealth(), (int) client.player.getMaxHealth(),
                                    dir.getName().substring(0, 1).toUpperCase() + dir.getName().substring(1).toLowerCase());
                        }

                        if (ChatStateManager.isCosmicSky() && !ChatStateManager.getActiveChannel().equalsIgnoreCase("Alliance")) {
                            LOGGER.info("[DEBUG] CosmicSky: Switching to Alliance channel for ping");
                            ChatStateManager.setSwitchingChannelForPing(true, msg);
                            client.player.connection.sendCommand("chat a");
                        } else if (ChatStateManager.isCosmicPrisons() && !ChatStateManager.getActiveChannel().equalsIgnoreCase("Gang") && !ChatStateManager.getActiveChannel().equalsIgnoreCase("Truce")) {
                            LOGGER.info("[DEBUG] CosmicPrisons: Switching to Truce channel for ping. Current channel: {}", ChatStateManager.getActiveChannel());
                            ChatStateManager.setLastAttemptedPing(msg);
                            ChatStateManager.setSwitchingChannelForPing(true, msg);
                            client.player.connection.sendCommand("gang chat truce");
                        } else {
                            LOGGER.info("[DEBUG] Sending ping directly. Current channel: {}", ChatStateManager.getActiveChannel());
                            ChatStateManager.setLastAttemptedPing(msg);
                            client.player.connection.sendChat(msg);
                            if (client.level != null) {
                                client.level.playSound(client.player, client.player.getX(), client.player.getY(), client.player.getZ(), 
                                    SoundEvents.NOTE_BLOCK_PLING, SoundSource.PLAYERS, 1.0f, 1.0f);
                            }
                        }
                    }
                    WaypointManager.setPreviewWaypoint(null);
                }
                wasPingPressed = isPressed;
            }

            while (pingKey.consumeClick()) {
                // Consume clicks
            }
        });

        ClientSendMessageEvents.COMMAND.register(command -> {
            processCommand(command);
        });

        ClientSendMessageEvents.CHAT.register(message -> {
            if (message.toLowerCase().startsWith("/pearl") || message.toLowerCase().equals("/pearl")) {
                 // Some servers might have a /pearl command, but usually it's just throwing it.
            }
        });
    }

    private void processCommand(String command) {
        String[] parts = command.split(" ");
        String baseCmd = parts[0].toLowerCase();

        if (ChatStateManager.isCosmicPrisons()) {
            switch (baseCmd) {
                case "eat":
                case "feed":
                    if (!cooldownHud.isOnCooldown("Feed")) {
                        cooldownHud.startCooldown("Feed", 3 * 60 * 1000);
                    }
                    break;
                case "fix":
                    if (command.toLowerCase().startsWith("fix all")) {
                        if (!cooldownHud.isOnCooldown("Fix All")) {
                            cooldownHud.startCooldown("Fix All", 3 * 60 * 1000);
                            ChatStateManager.setLastSentFixCommand("fix all");
                        }
                    } else {
                        if (!cooldownHud.isOnCooldown("Fix")) {
                            cooldownHud.startCooldown("Fix", 3 * 60 * 1000);
                            ChatStateManager.setLastSentFixCommand("fix");
                        }
                    }
                    break;
                case "jet":
                    if (!cooldownHud.isOnCooldown("Jet")) {
                        cooldownHud.startCooldown("Jet", 30 * 1000);
                    }
                    break;
                case "near":
                    if (!cooldownHud.isOnCooldown("Near")) {
                        cooldownHud.startCooldown("Near", 30 * 1000);
                    }
                    break;
                case "tpa":
                    if (!cooldownHud.isOnCooldown("TPA")) {
                        cooldownHud.startCooldown("TPA", 3 * 60 * 1000);
                    }
                    break;
                case "tpahere":
                    if (!cooldownHud.isOnCooldown("TPAHere")) {
                        cooldownHud.startCooldown("TPAHere", 4 * 60 * 1000);
                    }
                    break;
            }
        } else if (ChatStateManager.isCosmicSky()) {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null) return;
            
            String displayName = client.player.getDisplayName().getString().toLowerCase();
            
            if (baseCmd.equals("mule")) {
                if (!cooldownHud.isOnCooldown("Mule")) {
                    cooldownHud.startCooldown("Mule", 20 * 60 * 1000);
                }
                return;
            }

            if (baseCmd.equals("fix")) {
                if (command.toLowerCase().startsWith("fix all")) {
                    if (!cooldownHud.isOnCooldown("Fix All")) {
                        if (displayName.contains("celestial")) {
                            cooldownHud.startCooldown("Fix All", 90 * 1000);
                            ChatStateManager.setLastSentFixCommand("fix all");
                        } else if (displayName.contains("galactic")) {
                            cooldownHud.startCooldown("Fix All", 2 * 60 * 1000);
                            ChatStateManager.setLastSentFixCommand("fix all");
                        }
                    }
                } else {
                    if (!cooldownHud.isOnCooldown("Fix")) {
                        if (displayName.contains("titan")) {
                            cooldownHud.startCooldown("Fix", 5 * 60 * 1000);
                            ChatStateManager.setLastSentFixCommand("fix");
                        } else if (displayName.contains("comet")) {
                            cooldownHud.startCooldown("Fix", 10 * 60 * 1000);
                            ChatStateManager.setLastSentFixCommand("fix");
                        }
                    }
                }
            } else if (baseCmd.equals("eat") || baseCmd.equals("feed")) {
                if (!cooldownHud.isOnCooldown("Feed")) {
                    if (displayName.contains("celestial")) {
                        cooldownHud.startCooldown("Feed", 3 * 60 * 1000);
                    } else if (displayName.contains("galactic")) {
                        cooldownHud.startCooldown("Feed", 5 * 60 * 1000);
                    } else if (displayName.contains("titan")) {
                        cooldownHud.startCooldown("Feed", 10 * 60 * 1000);
                    }
                }
            } else if (baseCmd.equals("heal")) {
                if (!cooldownHud.isOnCooldown("Heal")) {
                    if (displayName.contains("celestial")) {
                        cooldownHud.startCooldown("Heal", 3 * 60 * 1000);
                    } else if (displayName.contains("galactic")) {
                        cooldownHud.startCooldown("Heal", 5 * 60 * 1000);
                    } else if (displayName.contains("titan")) {
                        cooldownHud.startCooldown("Heal", 10 * 60 * 1000);
                    }
                }
            } else if (baseCmd.equals("jet")) {
                if (!cooldownHud.isOnCooldown("Jet")) {
                    cooldownHud.startCooldown("Jet", 30 * 1000);
                }
            } else if (baseCmd.equals("near")) {
                if (!cooldownHud.isOnCooldown("Near")) {
                    cooldownHud.startCooldown("Near", 30 * 1000);
                }
            }
        }
    }
}
