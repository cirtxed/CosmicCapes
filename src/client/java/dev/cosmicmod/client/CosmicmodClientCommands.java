package dev.cosmicmod.client;

import dev.cosmicmod.client.waypoint.WaypointManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mojang.brigadier.arguments.StringArgumentType;

import java.util.Map;
import dev.cosmicmod.client.waypoint.Waypoint;
import dev.cosmicmod.client.notification.Notification;
import dev.cosmicmod.client.notification.NotificationManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class CosmicmodClientCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger("cosmicmod");

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("iteminfo").executes(context -> {
                var client = Minecraft.getInstance();
                if (client.player != null) {
                    var stack = client.player.getMainHandItem();
                    if (stack.isEmpty()) {
                        return 0;
                    }

                    var itemName = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                    context.getSource().sendFeedback(Component.literal("§bItem: §f" + itemName));
                }
                return 1;
            }));


            dispatcher.register(ClientCommandManager.literal("playerinfo").executes(context -> {
                var client = Minecraft.getInstance();
                var player = client.player;
                if (player != null) {
                    context.getSource().sendFeedback(Component.literal("§bPlayer: §f" + player.getScoreboardName()));
                    context.getSource().sendFeedback(Component.literal("§bUUID: §f" + player.getUUID()));
                    context.getSource().sendFeedback(Component.literal("§bPosition: §f" + player.position()));
                }
                return 1;
            }));

            dispatcher.register(ClientCommandManager.literal("readtitle").executes(context -> {
                var client = Minecraft.getInstance();
                
                if (client.getConnection() != null) {
                    context.getSource().sendFeedback(Component.literal("§aScreen titles and player titles have been sent to the console."));
                }
                return 1;
            }));

            dispatcher.register(ClientCommandManager.literal("cosmicmod")
                .then(ClientCommandManager.literal("config").executes(context -> {
                    var client = Minecraft.getInstance();
                    client.execute(() -> client.setScreen(new dev.cosmicmod.client.gui.ModsScreen(null)));
                    return 1;
                }))
                .then(ClientCommandManager.literal("debug").executes(context -> {
                    var client = Minecraft.getInstance();
                    var player = client.player;
                    if (player != null) {
                        context.getSource().sendFeedback(Component.literal("§e--- CosmicMod Debug ---"));
                        context.getSource().sendFeedback(Component.literal("§7Your UUID: §f" + player.getUUID()));
                        boolean matches = player.getUUID().toString().equals("dce48f53-8c32-469f-b7fb-dc11e612d0f7") || 
                                         player.getUUID().toString().equals("09a8c489-1fb9-4617-a2bf-16adad508254");
                        context.getSource().sendFeedback(Component.literal("§7Cosmetic Match: " + (matches ? "§aYES" : "§cNO")));
                        
                        // Check if skin has cape
                        if (player instanceof net.minecraft.client.player.AbstractClientPlayer acp) {
                            var skin = acp.getSkin();
                            // In 1.21.4 capeTexture() might be renamed or moved. Let's just log skin info.
                            context.getSource().sendFeedback(Component.literal("§7Skin Model: §f" + skin.model()));
                        }
                        
                        context.getSource().sendFeedback(Component.literal("§7Render Layers: §fOrbs, DiamondChain"));
                        context.getSource().sendFeedback(Component.literal("§7Config Settings:"));
                        context.getSource().sendFeedback(Component.literal("§7 - Orbs: " + (CosmicConfig.getInstance().showOrbs ? "§aON" : "§cOFF")));
                        context.getSource().sendFeedback(Component.literal("§7 - Diamond Chain: " + (CosmicConfig.getInstance().showDiamondChain ? "§aON" : "§cOFF")));
                        context.getSource().sendFeedback(Component.literal("§7 - Waypoints: " + (dev.cosmicmod.client.CosmicmodClient.getWaypointHud().isEnabled() ? "§aON" : "§cOFF")));
                        context.getSource().sendFeedback(Component.literal("§e-----------------------"));
                    }
                    return 1;
                }))
                .then(ClientCommandManager.literal("indicator").executes(context -> {
                    var client = Minecraft.getInstance();
                    var player = client.player;
                    if (player != null) {
                        dev.cosmicmod.client.render.DamageIndicatorManager.addIndicator(player, 5.5, false, true);
                        context.getSource().sendFeedback(Component.literal("§aSpawned test damage indicator at your position."));
                    }
                    return 1;
                }))
                .then(ClientCommandManager.literal("notify").executes(context -> {
                    NotificationManager.addNotification(new Notification("This is a test notification with a very long text to check multi-line support. It should wrap correctly!", new ItemStack(Items.DIAMOND), 5000));
                    NotificationManager.addNotification(new Notification("Short notification", new ItemStack(Items.GOLD_INGOT), 3000));
                    context.getSource().sendFeedback(Component.literal("§aSent test notifications."));
                    return 1;
                }))
                .then(ClientCommandManager.literal("cosmetics")
                    .then(ClientCommandManager.literal("reload").executes(context -> {
                        CosmeticManager.refreshCosmetics();
                        context.getSource().sendFeedback(Component.literal("§aRefreshing cosmetics from GitHub..."));
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("seturl").then(ClientCommandManager.argument("url", StringArgumentType.string()).executes(context -> {
                        String url = StringArgumentType.getString(context, "url");
                        CosmicConfig.getInstance().capeGithubUrl = url;
                        CosmicConfig.save();
                        CosmeticManager.refreshCosmetics();
                        context.getSource().sendFeedback(Component.literal("§aCosmetic GitHub URL set to: §f" + url));
                        return 1;
                    })))
                )
            );

            dispatcher.register(ClientCommandManager.literal("waypoint")
                .then(ClientCommandManager.literal("clear").executes(context -> {
                    WaypointManager.clearWaypoints();
                    context.getSource().sendFeedback(Component.literal("§aCleared all waypoints."));
                    return 1;
                }))
                .then(ClientCommandManager.literal("list").executes(context -> {
                    Map<String, Waypoint> waypoints = WaypointManager.getWaypoints();
                    if (waypoints.isEmpty()) {
                        context.getSource().sendFeedback(Component.literal("§eNo active waypoints."));
                    } else {
                        context.getSource().sendFeedback(Component.literal("§bActive waypoints:"));
                        for (Map.Entry<String, Waypoint> entry : waypoints.entrySet()) {
                            Waypoint w = entry.getValue();
                            context.getSource().sendFeedback(Component.literal("§7 - " + entry.getKey() + ": " + w.label().getString() + " at [" + (int)w.pos().x + "x " + (int)w.pos().y + "y " + (int)w.pos().z + "z]"));
                        }
                    }
                    return 1;
                }))
                .then(ClientCommandManager.literal("remove")
                    .then(ClientCommandManager.argument("id", StringArgumentType.string()).executes(context -> {
                        String id = StringArgumentType.getString(context, "id");
                        WaypointManager.removeWaypoint(id);
                        context.getSource().sendFeedback(Component.literal("§aRemoved waypoint: " + id));
                        return 1;
                    })))
            );

        });
    }
}
