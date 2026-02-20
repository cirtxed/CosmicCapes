package dev.cosmicmod.client.gui;

import dev.cosmicmod.client.waypoint.Waypoint;
import dev.cosmicmod.client.waypoint.WaypointManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import java.util.Collection;

import net.minecraft.client.gui.components.EditBox;
import java.util.stream.Collectors;

public class WaypointsScreen extends AbstractCosmicScreen {
    private WaypointList list;
    private EditBox searchBox;

    public WaypointsScreen(Screen parent) {
        super(Component.literal("Waypoints"), parent);
        this.activeTab = 1;
    }

    @Override
    protected void init() {
        super.init();
        
        int listWidth = guiWidth - 40;
        int listHeight = guiHeight - 110;
        int listX = left + 20;
        int listY = top + 75;

        this.searchBox = new EditBox(this.font, listX, top + 55, listWidth - 30, 15, Component.literal("Search..."));
        this.searchBox.setResponder(s -> this.list.setFilter(s));
        this.addRenderableWidget(this.searchBox);

        this.list = new WaypointList(this.minecraft, listWidth, listHeight, listY, 25);
        this.list.setX(listX);
        this.addWidget(this.list);

        this.addRenderableWidget(Button.builder(Component.literal("+"), (button) -> {
            this.minecraft.setScreen(new CreateWaypointScreen(this));
        }).bounds(left + guiWidth - 45, top + 52, 20, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        guiGraphics.drawString(this.font, "WAYPOINT LIST", left + 25, top + 42, 0x777777, false);

        this.list.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    class WaypointList extends ObjectSelectionList<WaypointList.Entry> {
        private String filter = "";

        public WaypointList(net.minecraft.client.Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
            this.refresh();
        }

        public void setFilter(String filter) {
            this.filter = filter.toLowerCase();
            this.refresh();
        }

        public void refresh() {
            this.clearEntries();
            
            WaypointManager.getWaypoints().values().stream()
                .filter(w -> w.label().getString().toLowerCase().contains(filter))
                .forEach(waypoint -> this.addEntry(new Entry(waypoint, false)));
            
            WaypointManager.getPings().values().stream()
                .filter(w -> w.label().getString().toLowerCase().contains(filter))
                .forEach(ping -> this.addEntry(new Entry(ping, true)));
        }

        class Entry extends ObjectSelectionList.Entry<Entry> {
            private final Waypoint waypoint;
            private final Button toggleButton;
            private final Button shareButton;
            private final Button deleteButton;

            public Entry(Waypoint waypoint, boolean isPing) {
                this.waypoint = waypoint;
                
                Component eyeIcon = (waypoint.visible() || isPing) ? Component.literal("👁") : Component.literal("👁").withStyle(net.minecraft.ChatFormatting.RED, net.minecraft.ChatFormatting.STRIKETHROUGH);
                this.toggleButton = Button.builder(eyeIcon, (button) -> {
                    if (!isPing) {
                        WaypointManager.toggleWaypoint(waypoint.id());
                        WaypointList.this.refresh();
                    }
                }).bounds(0, 0, 20, 20).build();
                
                if (isPing) {
                    this.toggleButton.active = false;
                }

                this.shareButton = Button.builder(Component.literal("Share"), (button) -> {
                    WaypointManager.shareWaypoint(waypoint);
                }).bounds(0, 0, 40, 20).build();

                this.deleteButton = Button.builder(Component.literal("Delete"), (button) -> {
                    WaypointManager.removeWaypoint(waypoint.id());
                    WaypointList.this.refresh();
                }).bounds(0, 0, 45, 20).build();
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTick) {
                guiGraphics.drawString(WaypointsScreen.this.font, waypoint.label(), left + 5, top + 5, 0xFFFFFF);
                
                int right = left + width - 5;
                
                deleteButton.setX(right - 45);
                deleteButton.setY(top);
                deleteButton.render(guiGraphics, mouseX, mouseY, partialTick);
                
                shareButton.setX(right - 45 - 45);
                shareButton.setY(top);
                shareButton.render(guiGraphics, mouseX, mouseY, partialTick);

                toggleButton.setX(right - 45 - 45 - 25);
                toggleButton.setY(top);
                toggleButton.render(guiGraphics, mouseX, mouseY, partialTick);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (toggleButton.mouseClicked(mouseX, mouseY, button)) return true;
                if (shareButton.mouseClicked(mouseX, mouseY, button)) return true;
                if (deleteButton.mouseClicked(mouseX, mouseY, button)) return true;
                return super.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public Component getNarration() {
                return Component.literal(waypoint.label().getString());
            }
        }
    }
}
