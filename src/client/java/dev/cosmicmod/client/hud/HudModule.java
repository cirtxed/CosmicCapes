package dev.cosmicmod.client.hud;

import dev.cosmicmod.client.CosmicConfig;
import dev.cosmicmod.client.feature.FeatureMenu;
import dev.cosmicmod.client.feature.ServerLock;
import net.minecraft.client.gui.GuiGraphics;

public abstract class HudModule {
    private final String name;
    private final String description;
    private final FeatureMenu menu;
    private final ServerLock serverLock;
    private final boolean moveable;
    private boolean enabled;
    private float x;
    private float y;
    protected float width;
    protected float height;

    public HudModule(String name, boolean defaultEnabled, float defaultX, float defaultY) {
        this(name, "", defaultEnabled, defaultX, defaultY, FeatureMenu.MODS, ServerLock.NONE, true);
    }

    public HudModule(String name, String description, boolean defaultEnabled, float defaultX, float defaultY) {
        this(name, description, defaultEnabled, defaultX, defaultY, FeatureMenu.MODS, ServerLock.NONE, true);
    }

    public HudModule(String name, String description, boolean defaultEnabled, float defaultX, float defaultY, FeatureMenu menu, ServerLock serverLock) {
        this(name, description, defaultEnabled, defaultX, defaultY, menu, serverLock, true);
    }

    public HudModule(String name, String description, boolean defaultEnabled, float defaultX, float defaultY, FeatureMenu menu, ServerLock serverLock, boolean moveable) {
        this.name = name;
        this.description = description;
        this.menu = menu;
        this.serverLock = serverLock;
        this.moveable = moveable;
        this.loadSettings(defaultEnabled, defaultX, defaultY);
        this.width = 50;
        this.height = 10;
    }

    private void loadSettings(boolean defaultEnabled, float defaultX, float defaultY) {
        CosmicConfig config = CosmicConfig.getInstance();
        CosmicConfig.HudSettings settings = config.hudSettings.get(this.name);
        if (settings != null) {
            this.enabled = settings.enabled;
            this.x = settings.x;
            this.y = settings.y;
        } else {
            this.enabled = defaultEnabled;
            this.x = defaultX;
            this.y = defaultY;
            saveSettings();
        }
    }

    public void saveSettings() {
        CosmicConfig config = CosmicConfig.getInstance();
        CosmicConfig.HudSettings settings = config.hudSettings.get(this.name);
        if (settings == null) {
            settings = new CosmicConfig.HudSettings(this.enabled, this.x, this.y);
            config.hudSettings.put(this.name, settings);
        } else {
            settings.enabled = this.enabled;
            settings.x = this.x;
            settings.y = this.y;
        }
        CosmicConfig.save();
    }

    public CosmicConfig.HudSettings getSettings() {
        return CosmicConfig.getInstance().hudSettings.get(this.name);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public FeatureMenu getMenu() {
        return menu;
    }

    public ServerLock getServerLock() {
        return serverLock;
    }

    public boolean isMoveable() {
        return moveable;
    }

    public boolean isEnabled() {
        return enabled && !serverLock.isLocked();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.saveSettings();
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
        this.saveSettings();
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
        this.saveSettings();
    }

    public boolean isRightAligned() {
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        return getX() + getWidth() / 2.0f > client.getWindow().getGuiScaledWidth() / 2.0f;
    }

    public boolean isBottomAligned() {
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        return getY() + getHeight() / 2.0f > client.getWindow().getGuiScaledHeight() / 2.0f;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public abstract void render(GuiGraphics context, float partialTicks);

    protected void renderBackground(GuiGraphics context, float x, float y, float width, float height) {
        CosmicConfig.HudSettings settings = getSettings();
        if (settings == null) return;

        if (hasShowBackgroundSetting() && settings.showBackground) {
            int color = settings.backgroundColor;
            
            context.fill((int) x, (int) y, (int) (x + width), (int) (y + height), color);

            if (hasBorderSetting() && settings.showBorder) {
                int borderColor = settings.borderColor;
                if ((borderColor >> 24) == 0) borderColor |= 0xFF000000;
                // Draw 4 lines for border
                context.fill((int) x, (int) y, (int) (x + width), (int) (y + 1), borderColor); // Top
                context.fill((int) x, (int) (y + height - 1), (int) (x + width), (int) (y + height), borderColor); // Bottom
                context.fill((int) x, (int) y, (int) (x + 1), (int) (y + height), borderColor); // Left
                context.fill((int) (x + width - 1), (int) y, (int) (x + width), (int) (y + height), borderColor); // Right
            }
        }
    }
    
    public void tick() {}

    public boolean hasBackgroundSetting() {
        return true;
    }

    public boolean hasShowBackgroundSetting() {
        return true;
    }

    public boolean hasTextShadowSetting() {
        return true;
    }


    public boolean hasReverseOrderSetting() {
        return false;
    }

    public boolean hasBorderSetting() {
        return true;
    }

    public boolean hasColorSetting() {
        return false;
    }

    public boolean hasFormattingColorSetting() {
        return false;
    }

    public boolean hasTintSetting() {
        return false;
    }

    public boolean hasScaleSetting() {
        return true;
    }

    public boolean hasKeybindSetting() {
        return false;
    }

    public boolean hasPingDurationSetting() {
        return false;
    }

    public boolean hasPlayerListSettings() {
        return false;
    }

    public boolean hasHandScalerSettings() {
        return false;
    }

    public boolean hasSatchelSettings() {
        return false;
    }
}
