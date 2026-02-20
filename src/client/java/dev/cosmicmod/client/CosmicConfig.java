package dev.cosmicmod.client;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import dev.cosmicmod.client.waypoint.Waypoint;

public class CosmicConfig {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Color.class, new ColorAdapter())
            .registerTypeAdapter(Vec3.class, new Vec3Adapter())
            .registerTypeAdapter(Component.class, new ComponentAdapter())
            .create();

    private static class ColorAdapter implements JsonSerializer<Color>, JsonDeserializer<Color> {
        @Override
        public JsonElement serialize(Color src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.getRGB());
        }

        @Override
        public Color deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return new Color(json.getAsInt(), true);
        }
    }

    private static class Vec3Adapter implements JsonSerializer<Vec3>, JsonDeserializer<Vec3> {
        @Override
        public JsonElement serialize(Vec3 src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("x", src.x);
            obj.addProperty("y", src.y);
            obj.addProperty("z", src.z);
            return obj;
        }

        @Override
        public Vec3 deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            return new Vec3(obj.get("x").getAsDouble(), obj.get("y").getAsDouble(), obj.get("z").getAsDouble());
        }
    }

    private static class ComponentAdapter implements JsonSerializer<Component>, JsonDeserializer<Component> {
        @Override
        public JsonElement serialize(Component src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.getString());
        }

        @Override
        public Component deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return Component.literal(json.getAsString());
        }
    }

    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("cosmicmod.json").toFile();

    public boolean showOrbs = true;
    public boolean showDiamondChain = true;
    public boolean showAuras = true;
    public int pingDuration = 30;

    public float mainHandScale = 1.0f;
    public float offHandScale = 1.0f;

    public List<Waypoint> waypoints = new ArrayList<>();
    public List<String> friends = new ArrayList<>();
    public Map<String, HudSettings> hudSettings = new HashMap<>();
    public Map<String, Map<String, CooldownData>> serverCooldowns = new HashMap<>();

    public boolean playerListIgnoreFriends = false;
    public boolean playerListHighlightFriends = true;
    public boolean playerListIgnoreSelf = false;
    public boolean playerListHighlightSelf = true;
    public boolean chatFilterEnabled = true;
    public boolean allowMiningWhileRightClicking = false;
    public Map<String, String> itemTextureOverrides = new HashMap<>();
    public String capeGithubUrl = "https://raw.githubusercontent.com/cirtxed/CosmicCapes/main/github_capes/capes.txt";
    public long casinoCoins = 1000;
    public Map<String, Integer> itemTintOverrides = new HashMap<>();
    public Map<String, Integer> stackTintOverrides = new HashMap<>();

    public static class CooldownData {
        public long endTime;
        public long startTime;

        public CooldownData(long startTime, long endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    public static class HudSettings {
        public boolean enabled;
        public float x;
        public float y;
        public float scale = 1.0f;
        public boolean textShadow = true;
        public boolean showBackground = true;
        public int backgroundColor = 0x6f000000;
        public int headerColor = 0x50000000;
        public int borderColor = 0x9f000000;
        public int formattingColor = 0xFFFFFFFF;
        public boolean showBorder = false;
        public float borderThickness = 0.5f;
        public boolean reverseOrder = false;
        public boolean showMeteorWaypoints = true;
        public int meteorDuration = 60;
        public boolean showLabel = true;
        public String label = "";
        
        // Satchel Settings
        public boolean hideSatchelNames = false;
        public int satchelDisplayMode = 0; // 0: x/capacity, 1: x%, 2: x% (x/capacity)
        public boolean dynamicCapacityColor = false;

        public HudSettings(boolean enabled, float x, float y) {
            this.enabled = enabled;
            this.x = x;
            this.y = y;
        }
    }

    private static CosmicConfig instance;

    public static CosmicConfig getInstance() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                instance = GSON.fromJson(reader, CosmicConfig.class);
            } catch (IOException e) {
                instance = new CosmicConfig();
            }
        } else {
            instance = new CosmicConfig();
            save();
        }
    }

    public static void save() {
        if (instance == null) return;
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(instance, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
