package dev.cosmicmod.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.TriState;

public class GuardBeaconRenderType extends RenderType {
    public static final GuardBeaconRenderType INSTANCE = new GuardBeaconRenderType("guard_beacon", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, false, true, () -> {}, () -> {});

    private static final RenderType BEACON = create(
        "guard_beacon",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.QUADS,
        256,
        false,
        true,
        RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_BEACON_BEAM_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png"), TriState.FALSE, false))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setWriteMaskState(COLOR_WRITE)
            .setDepthTestState(LEQUAL_DEPTH_TEST) // Standard depth testing
            .createCompositeState(false)
    );

    public GuardBeaconRenderType(String string, VertexFormat vertexFormat, VertexFormat.Mode mode, int i, boolean bl, boolean bl2, Runnable runnable, Runnable runnable2) {
        super(string, vertexFormat, mode, i, bl, bl2, runnable, runnable2);
    }

    public RenderType getBEACON() {
        return BEACON;
    }
}
