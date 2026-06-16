package com.norwood.mcheli.mixin;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Keeps MCHeli aircraft out of {@code RenderGlobal}'s normal entity pass; they are drawn separately
 * by {@link com.norwood.mcheli.core.VehicleRenderHook} (and the far pass when enabled). Faithful
 * port of the old {@code RenderGlobalTransformer}, which forced the per-entity {@code flag} to false
 * for aircraft.
 *
 */
@Mixin(RenderGlobal.class)
public class MixinRenderGlobal {

    @Redirect(
            method = "renderEntities(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/renderer/culling/ICamera;F)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/RenderManager;shouldRender(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/renderer/culling/ICamera;DDD)Z"))
    private boolean mcheli$aircraftNeverShouldRender(RenderManager renderManager, Entity entity2, ICamera camera,
                                                     double camX, double camY, double camZ) {
        if (entity2 instanceof MCH_EntityAircraft) {
            return false;
        }
        return renderManager.shouldRender(entity2, camera, camX, camY, camZ);
    }

    @Redirect(
            method = "renderEntities(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/renderer/culling/ICamera;F)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;isRidingOrBeingRiddenBy(Lnet/minecraft/entity/Entity;)Z"))
    private boolean mcheli$aircraftNeverRidingFlag(Entity entity2, Entity other) {
        if (entity2 instanceof MCH_EntityAircraft) {
            return false;
        }
        return entity2.isRidingOrBeingRiddenBy(other);
    }
}
