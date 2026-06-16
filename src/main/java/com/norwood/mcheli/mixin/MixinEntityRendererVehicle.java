package com.norwood.mcheli.mixin;

import com.norwood.mcheli.core.VehicleRenderHook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Always-on render hook (port of {@code EntityRenderHooks}): after {@code RenderGlobal.renderEntities}
 * runs in the pass-0 entity stage of {@code renderWorldPass}, draw the MCHeli aircraft separately via
 * {@link VehicleRenderHook}. {@code VehicleRenderHook#renderVehicles} no-ops for render pass != 0 and
 * ignores the camera argument, so we inject only at the first {@code renderEntities} call and pass the
 * current render-view entity (== the {@code entity} local the call uses).
 */
@Mixin(EntityRenderer.class)
public class MixinEntityRendererVehicle {

    @Inject(
            method = "renderWorldPass(IFJ)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderGlobal;renderEntities(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/renderer/culling/ICamera;F)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER))
    private void mcheli$renderVehicles(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        VehicleRenderHook.INSTANCE.renderVehicles(
                Minecraft.getMinecraft().getRenderViewEntity(), null, partialTicks);
    }
}
