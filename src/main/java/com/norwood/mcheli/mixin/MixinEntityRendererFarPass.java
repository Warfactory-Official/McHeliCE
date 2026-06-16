package com.norwood.mcheli.mixin;

import com.norwood.mcheli.core.EntityRendererHook;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Long-distance render hook (port of {@code EntityRendererTransformer}); gated by the
 * {@code mixins.mcheli.longdistance.json} config, which {@code MCHCore} only queues when
 * "remove client tracking restrictions" is enabled. After the first {@code renderEntities} call in
 * {@code renderWorldPass}, run the extended far-plane render pass that draws force-tracked entities
 * beyond the normal view distance.
 */
@Mixin(EntityRenderer.class)
public class MixinEntityRendererFarPass {

    @Inject(
            method = "renderWorldPass(IFJ)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderGlobal;renderEntities(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/renderer/culling/ICamera;F)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER))
    private void mcheli$renderFarPass(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        EntityRendererHook.renderFarPass((EntityRenderer) (Object) this, partialTicks);
    }
}
