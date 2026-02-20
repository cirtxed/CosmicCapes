package dev.cosmicmod.mixin.client;

import dev.cosmicmod.client.util.PlayerUUIDHolder;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

@Mixin(targets = "net.minecraft.client.renderer.entity.state.PlayerRenderState")
public class PlayerRenderStateMixin implements PlayerUUIDHolder {
    @Unique
    private UUID cosmicmod$uuid;
    @Unique
    private boolean cosmicmod$local;

    @Override
    public void cosmicmod$setUUID(UUID uuid) {
        this.cosmicmod$uuid = uuid;
    }

    @Override
    public UUID cosmicmod$getUUID() {
        return this.cosmicmod$uuid;
    }

    @Override
    public void cosmicmod$setLocalPlayer(boolean local) {
        this.cosmicmod$local = local;
    }

    @Override
    public boolean cosmicmod$isLocalPlayer() {
        return this.cosmicmod$local;
    }
}
