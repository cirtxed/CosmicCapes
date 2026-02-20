package dev.cosmicmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SkinTextureDownloader;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CosmeticManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("cosmicmod-cosmetics");
    private static final Map<UUID, ResourceLocation> CAPE_MAP = new HashMap<>();
    private static final Map<UUID, ParticleOptions> AURA_MAP = new HashMap<>();
    private static final Map<UUID, Map<String, ResourceLocation>> ITEM_OVERRIDE_MAP = new HashMap<>();
    private static final Path CAPE_CACHE_DIR = Minecraft.getInstance().gameDirectory.toPath().resolve("capes");

    public static void init() {
        File dir = CAPE_CACHE_DIR.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        refreshCosmetics();
    }

    public static void refreshCosmetics() {
        String url = CosmicConfig.getInstance().capeGithubUrl;
        if (url == null || url.isEmpty()) {
            LOGGER.info("Cosmetic GitHub URL is not set.");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("GET");
                
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    Map<UUID, String> newCapes = new HashMap<>();
                    Map<UUID, ParticleOptions> newAuras = new HashMap<>();
                    Map<UUID, Map<String, ResourceLocation>> newItemOverrides = new HashMap<>();

                    while ((line = reader.readLine()) != null) {
                        if (line.trim().isEmpty() || line.startsWith("#")) continue;
                        
                        String[] parts = line.split(":", 3);
                        if (parts.length >= 2) {
                            String type = parts[0].trim().toLowerCase();
                            String playerIdentifier = parts[1].trim();
                            String value = parts.length > 2 ? parts[2].trim() : "";
                            
                            UUID uuid = parseUUID(playerIdentifier);
                            if (uuid == null) {
                                LOGGER.warn("Invalid UUID format: {}", playerIdentifier);
                                continue;
                            }

                            switch (type) {
                                case "cape":
                                    newCapes.put(uuid, value);
                                    break;
                                case "aura":
                                    ParticleOptions aura = parseParticle(value);
                                    if (aura != null) {
                                        newAuras.put(uuid, aura);
                                    }
                                    break;
                                case "block":
                                    // format: block:uuid:itemName=textureUrl
                                    String[] blockParts = value.split("=", 2);
                                    if (blockParts.length == 2) {
                                        String itemName = blockParts[0].trim();
                                        String texture = blockParts[1].trim();
                                        try {
                                            ResourceLocation rl = ResourceLocation.parse(texture);
                                            newItemOverrides.computeIfAbsent(uuid, k -> new HashMap<>()).put(itemName, rl);
                                        } catch (Exception e) {
                                            LOGGER.warn("Invalid texture ResourceLocation: {}", texture);
                                        }
                                    }
                                    break;
                                default:
                                    // Backward compatibility: uuid:cape_url
                                    if (type.length() >= 32 || type.contains("-")) {
                                        UUID legacyUuid = parseUUID(type);
                                        if (legacyUuid != null) {
                                            newCapes.put(legacyUuid, playerIdentifier);
                                        }
                                    }
                                    break;
                            }
                        }
                    }
                    
                    updateCosmetics(newCapes, newAuras, newItemOverrides);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to fetch cosmetic configuration from GitHub", e);
            }
        });
    }

    private static ParticleOptions parseParticle(String name) {
        try {
            ResourceLocation rl = ResourceLocation.parse(name);
            var holder = BuiltInRegistries.PARTICLE_TYPE.get(rl);
            if (holder != null && holder.isPresent()) {
                ParticleType<?> type = holder.get().value();
                if (type instanceof ParticleOptions options) {
                    return options;
                }
            }
        } catch (Exception ignored) {}
        return ParticleTypes.END_ROD;
    }

    private static UUID parseUUID(String s) {
        try {
            if (s.length() == 32) {
                return UUID.fromString(s.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})", "$1-$2-$3-$4-$5"));
            }
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void updateCosmetics(Map<UUID, String> newCapes, Map<UUID, ParticleOptions> newAuras, Map<UUID, Map<String, ResourceLocation>> newItemOverrides) {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            CAPE_MAP.clear();
            newCapes.forEach((uuid, url) -> {
                ResourceLocation rl = ResourceLocation.fromNamespaceAndPath("cosmicmod", "capes/" + uuid.toString());
                registerCape(rl, url);
                CAPE_MAP.put(uuid, rl);
            });

            AURA_MAP.clear();
            AURA_MAP.putAll(newAuras);

            ITEM_OVERRIDE_MAP.clear();
            ITEM_OVERRIDE_MAP.putAll(newItemOverrides);

            LOGGER.info("Updated cosmetics: {} capes, {} auras, {} item override sets.", CAPE_MAP.size(), AURA_MAP.size(), ITEM_OVERRIDE_MAP.size());
        });
    }

    private static void registerCape(ResourceLocation rl, String url) {
        Path cachePath = CAPE_CACHE_DIR.resolve(rl.getPath().replace("/", "_") + ".png");
        SkinTextureDownloader.downloadAndRegisterSkin(rl, cachePath, url, false);
    }

    public static ResourceLocation getCape(UUID uuid) {
        return CAPE_MAP.get(uuid);
    }

    public static ParticleOptions getAura(UUID uuid) {
        return AURA_MAP.get(uuid);
    }

    public static ResourceLocation getItemOverride(UUID uuid, String itemName) {
        Map<String, ResourceLocation> overrides = ITEM_OVERRIDE_MAP.get(uuid);
        if (overrides == null) return null;
        
        for (Map.Entry<String, ResourceLocation> entry : overrides.entrySet()) {
            if (itemName.equals(entry.getKey()) || itemName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
