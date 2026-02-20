package dev.cosmicmod.client.gui;

import dev.cosmicmod.client.CosmicConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FriendsScreen extends AbstractCosmicScreen {
    private FriendList list;
    private EditBox addFriendBox;

    public FriendsScreen(Screen parent) {
        super(Component.literal("Friends"), parent);
        this.activeTab = 2;
    }

    @Override
    protected void init() {
        super.init();

        int listWidth = guiWidth - 40;
        int listHeight = guiHeight - 110;
        int listX = left + 20;
        int listY = top + 75;

        this.addFriendBox = new EditBox(this.font, listX, top + 55, listWidth - 30, 15, Component.literal("Enter IGN..."));
        this.addFriendBox.setMaxLength(16);
        this.addRenderableWidget(this.addFriendBox);

        this.list = new FriendList(this.minecraft, listWidth, listHeight, listY, 25);
        this.list.setX(listX);
        this.addWidget(this.list);

        this.addRenderableWidget(Button.builder(Component.literal("+"), (button) -> {
            String name = addFriendBox.getValue().trim();
            if (!name.isEmpty()) {
                String url = "https://namemc.com/profile/" + name;
                if (!CosmicConfig.getInstance().friends.contains(url)) {
                    CosmicConfig.getInstance().friends.add(url);
                    CosmicConfig.save();
                    addFriendBox.setValue("");
                    this.list.refresh();
                }
            }
        }).bounds(left + guiWidth - 45, top + 52, 20, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawString(this.font, "FRIEND LIST", left + 25, top + 42, 0x777777, false);
        this.list.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    class FriendList extends ObjectSelectionList<FriendList.Entry> {
        public FriendList(net.minecraft.client.Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
            this.refresh();
        }

        public void refresh() {
            this.clearEntries();
            List<String> friends = new ArrayList<>(CosmicConfig.getInstance().friends);
            friends.sort(String.CASE_INSENSITIVE_ORDER);
            for (String friend : friends) {
                this.addEntry(new Entry(friend));
            }
        }

        class Entry extends ObjectSelectionList.Entry<Entry> {
            private final String name;
            private final String url;
            private final Button deleteButton;

            public Entry(String url) {
                this.url = url;
                String[] parts = url.split("/");
                this.name = parts[parts.length - 1];
                this.deleteButton = Button.builder(Component.literal("Delete"), (button) -> {
                    CosmicConfig.getInstance().friends.remove(url);
                    CosmicConfig.save();
                    FriendList.this.refresh();
                }).bounds(0, 0, 45, 20).build();
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTick) {
                guiGraphics.drawString(FriendsScreen.this.font, name, left + 5, top + 5, 0x55FF55);
                
                int right = left + width - 5;
                deleteButton.setX(right - 45);
                deleteButton.setY(top);
                deleteButton.render(guiGraphics, mouseX, mouseY, partialTick);
                
                if (isHovered) {
                    guiGraphics.renderTooltip(font, Component.literal(url), mouseX, mouseY);
                }
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (deleteButton.mouseClicked(mouseX, mouseY, button)) return true;
                return super.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public Component getNarration() {
                return Component.literal(name);
            }
        }
    }
}
