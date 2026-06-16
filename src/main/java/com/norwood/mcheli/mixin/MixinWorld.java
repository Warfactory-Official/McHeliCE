package com.norwood.mcheli.mixin;

import com.norwood.mcheli.core.WorldUnloadHook;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Collection;

/**
 * Long-distance world patch (port of {@code EntityUnloadTransformer} + {@code WorldUnloadHook}); gated
 * by the {@code mixins.mcheli.longdistance.json} config. Rewrites the collection passed to
 * {@code unloadEntities} so that, on the client, MCHeli entities are not dropped when their chunk
 * unloads. {@link WorldUnloadHook#onEntityUnload(Collection)} is a no-op on the server.
 */
@Mixin(World.class)
public class MixinWorld {

    @ModifyVariable(
            method = "unloadEntities(Ljava/util/Collection;)V",
            at = @At("HEAD"),
            argsOnly = true)
    private Collection<Entity> mcheli$filterClientUnload(Collection<Entity> entities) {
        return WorldUnloadHook.onEntityUnload(entities);
    }
}
