package dev.cosmicmod.client.hud;

import dev.cosmicmod.client.CosmicConfig;
import dev.cosmicmod.client.feature.FeatureMenu;
import dev.cosmicmod.client.feature.ServerLock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PlayerListHud extends HudModule {

    public PlayerListHud() {
        super("Player List", "Shows a list of nearby real players on the screen.", false, 5, 150, FeatureMenu.MODS, ServerLock.NONE);
    }

    @Override
    public void render(GuiGraphics context, float partialTicks) {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) return;

        Collection<PlayerInfo> players = client.getConnection().getOnlinePlayers();
        if (players.isEmpty()) return;

        CosmicConfig config = CosmicConfig.getInstance();

        List<PlayerInfo> filteredPlayers = players.stream()
                .filter(this::isRealPlayer)
                .filter(info -> {
                    boolean isSelf = client.player != null && info.getProfile().getId().equals(client.player.getUUID());
                    if (isSelf) return !config.playerListIgnoreSelf;
                    
                    String name = info.getProfile().getName();
                    boolean isFriend = config.friends.stream().anyMatch(url -> url.endsWith("/" + name) || url.equals(name));
                    if (isFriend) return !config.playerListIgnoreFriends;
                    
                    return !isSelf; // Original logic excluded self
                })
                .sorted(Comparator.comparing(info -> info.getProfile().getName()))
                .collect(Collectors.toList());

        int yOffset = 0;
        int maxWidth = 0;

        String customLabel = getSettings().label;
        String header;
        if (customLabel != null && !customLabel.isEmpty()) {
            header = customLabel.replace("&", "§");
        } else {
            header = "§bPlayers (" + filteredPlayers.size() + "):";
        }
        int headerWidth = 0;
        if (getSettings().showLabel) {
            headerWidth = client.font.width(header);
        }
        
        for (PlayerInfo info : filteredPlayers) {
            String name = info.getProfile().getName();
            String text = "- " + name;
            maxWidth = Math.max(maxWidth, client.font.width(text));
        }
        maxWidth = Math.max(maxWidth, headerWidth) + 4;
        
        int totalHeight = (filteredPlayers.size() * 10) + 4;
        if (getSettings().showLabel) {
            totalHeight += 12;
        }

        float scale = getSettings().scale;
        boolean rightAligned = isRightAligned();

        context.pose().pushPose();
        context.pose().translate(getX(), getY(), 0);
        context.pose().scale(scale, scale, 1.0f);

        renderBackground(context, 0, 0, maxWidth, totalHeight);
        
        yOffset = 2;
        if (getSettings().showLabel) {
            int headerX = rightAligned ? maxWidth - headerWidth - 2 : 2;
            context.drawString(client.font, header, headerX, 2, 0xFFFFFFFF, getSettings().textShadow);
            yOffset = 14;
        }

        for (PlayerInfo info : filteredPlayers) {
            String name = info.getProfile().getName();
            boolean isSelf = client.player != null && info.getProfile().getId().equals(client.player.getUUID());
            boolean isFriend = config.friends.stream().anyMatch(url -> url.endsWith("/" + name) || url.equals(name));
            
            int color = 0xFFFFFF;
            if (isSelf && config.playerListHighlightSelf) {
                color = 0x55FFFF; // Cyan for self
            } else if (isFriend && config.playerListHighlightFriends) {
                color = 0x55FF55; // Green for friends
            }
            
            String text = name + (rightAligned ? " -" : "");
            if (!rightAligned) text = "- " + name;

            int textWidth = client.font.width(text);
            int textX = rightAligned ? maxWidth - textWidth - 2 : 2;
            context.drawString(client.font, text, textX, yOffset, color, getSettings().textShadow);
            yOffset += 10;
        }

        context.pose().popPose();

        this.width = maxWidth * scale;
        this.height = totalHeight * scale;
    }

    @Override
    public boolean hasFormattingColorSetting() {
        return false;
    }

    @Override
    public boolean hasScaleSetting() {
        return true;
    }

    @Override
    public boolean hasShowBackgroundSetting() {
        return true;
    }

    @Override
    public boolean hasBackgroundSetting() {
        return true;
    }

    @Override
    public boolean hasBorderSetting() {
        return true;
    }

    @Override
    public boolean hasTextShadowSetting() {
        return true;
    }

    @Override
    public boolean hasPlayerListSettings() {
        return true;
    }

    private boolean isRealPlayer(PlayerInfo info) {
        if (info.getProfile() == null) return false;
        
        // Real players on Cosmic/Premium servers have Version 4 UUIDs.
        // Offline mode players have Version 3.
        // NPCs and fake tab entries often use Version 2 or other non-standard ones.
        int version = info.getProfile().getId().version();
        if (version != 4 && version != 3) return false;

        // Fake players often have 0 or -1 ping
        if (info.getLatency() <= 0) return false;

        // Real players have a skin texture property
        if (!info.getProfile().getProperties().containsKey("textures")) return false;

        // Common fake player name patterns
        String name = info.getProfile().getName();
        if (name == null || name.isEmpty()) return false;
        if (name.startsWith("!") || name.startsWith(" ") || name.contains(" ")) return false;

        return true;
    }
}
