package dev.cosmicmod.client.hud;

import net.minecraft.client.gui.GuiGraphics;
import java.util.ArrayList;
import java.util.List;

public class HudManager {
    private static final List<HudModule> modules = new ArrayList<>();

    public static void register(HudModule module) {
        modules.add(module);
    }

    public static List<HudModule> getModules() {
        return modules;
    }

    public static void render(GuiGraphics context, float partialTicks) {
        for (HudModule module : modules) {
            if (module.isEnabled()) {
                module.render(context, partialTicks);
            }
        }
    }

    public static void tick() {
        for (HudModule module : modules) {
            if (module.isEnabled()) {
                module.tick();
            }
        }
    }
}
