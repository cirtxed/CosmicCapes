package dev.cosmicmod.client.gui;

import dev.cosmicmod.client.ChatStateManager;
import dev.cosmicmod.client.CosmicmodClient;
import dev.cosmicmod.client.gui.components.ColorSlider;
import dev.cosmicmod.client.waypoint.Waypoint;
import dev.cosmicmod.client.waypoint.WaypointManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;
import java.util.UUID;

public class CreateWaypointScreen extends AbstractCosmicScreen {
    private EditBox nameEdit;
    private EditBox xEdit;
    private EditBox yEdit;
    private EditBox zEdit;
    private ColorSlider colorSlider;

    public CreateWaypointScreen(Screen parent) {
        super(Component.literal("CREATE WAYPOINT"), parent);
        this.activeTab = -1;
    }

    @Override
    protected void init() {
        super.init();
        int startX = left + 25;
        int startY = top + 65; 

        Vec3 playerPos = this.minecraft.player != null ? this.minecraft.player.position() : Vec3.ZERO;

        this.nameEdit = new EditBox(this.font, startX, startY, 150, 20, Component.literal("Name"));
        this.nameEdit.setValue("New Waypoint");
        this.addRenderableWidget(this.nameEdit);

        int coordsY = startY + 50; 
        this.xEdit = new EditBox(this.font, startX, coordsY + 12, 45, 20, Component.literal("X"));
        this.xEdit.setValue(String.valueOf((int) playerPos.x));
        this.addRenderableWidget(this.xEdit);

        this.yEdit = new EditBox(this.font, startX + 52, coordsY + 12, 45, 20, Component.literal("Y"));
        this.yEdit.setValue(String.valueOf((int) playerPos.y));
        this.addRenderableWidget(this.yEdit);

        this.zEdit = new EditBox(this.font, startX + 104, coordsY + 12, 45, 20, Component.literal("Z"));
        this.zEdit.setValue(String.valueOf((int) playerPos.z));
        this.addRenderableWidget(this.zEdit);

        this.colorSlider = new ColorSlider(left + 205, startY, 175, 66, 0xFF0000, null);
        this.addRenderableWidget(this.colorSlider);

        this.addRenderableWidget(Button.builder(Component.literal("Create"), (button) -> {
            try {
                String name = this.nameEdit.getValue();
                double x = Double.parseDouble(this.xEdit.getValue());
                double y = Double.parseDouble(this.yEdit.getValue());
                double z = Double.parseDouble(this.zEdit.getValue());
                Color color = new Color(0xFF000000 | this.colorSlider.getSelectedColor());
                
                String worldKey = this.minecraft.level.dimension().location().getPath();
                String worldName = dev.cosmicmod.client.CosmicmodClient.getWorldDisplayName(worldKey);
                if (worldName == null) worldName = "Overworld"; 

                Waypoint waypoint = new Waypoint(
                        UUID.randomUUID().toString(),
                        new Vec3(x, y, z),
                        Component.literal(name),
                        color,
                        0,
                        null,
                        0,
                        false,
                        worldName,
                        true,
                        dev.cosmicmod.client.ChatStateManager.getCurrentServer()
                );
                WaypointManager.addWaypoint(waypoint);
                this.minecraft.setScreen(this.parent);
            } catch (Exception e) {
            }
        }).bounds(left + 25, top + guiHeight - 35, 80, 20).build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, (button) -> {
            this.minecraft.setScreen(this.parent);
        }).bounds(left + 115, top + guiHeight - 35, 80, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        int startY = top + 65;
        int coordsY = startY + 50;

        guiGraphics.drawString(this.font, "NAME", left + 25, startY - 10, 0x777777);
        guiGraphics.drawString(this.font, "COORDINATES", left + 25, coordsY, 0x777777);
        guiGraphics.drawString(this.font, "COLOR", left + 205, startY - 10, 0x777777);
    }
}
