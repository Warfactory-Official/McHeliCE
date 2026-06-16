package com.norwood.mcheli.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.norwood.mcheli.core.TrackerHook;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Long-distance entity-tracking patch (port of {@code EntityTrackerEntryTransformer}); gated by the
 * {@code mixins.mcheli.longdistance.json} config.
 *
 * <ul>
 *   <li>{@code isVisibleTo}: replaces the vanilla {@code Math.min(range, maxRange)} radius clamp with
 *       {@link TrackerHook#getRenderDistance(Entity, int, int, EntityPlayerMP)} — the watching player
 *       is captured from {@code isVisibleTo}'s argument so the extended UAV range is granted only to
 *       that UAV's own operator/previewer, never to bystanders.</li>
 *   <li>{@code isPlayerWatchingThisChunk}: returns true early for force-watched entities
 *       ({@link TrackerHook#shouldForceWatch(Entity)}), bypassing the chunk-loaded precondition.</li>
 * </ul>
 */
@Mixin(EntityTrackerEntry.class)
public class MixinEntityTrackerEntry {

    @Shadow @Final private Entity trackedEntity;

    @Redirect(
            method = "isVisibleTo(Lnet/minecraft/entity/player/EntityPlayerMP;)Z",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I"))
    private int mcheli$forceTrackRange(int range, int maxRange, EntityPlayerMP playerMP) {
        return TrackerHook.getRenderDistance(this.trackedEntity, range, maxRange, playerMP);
    }

    /**
     * Camera-aware visibility: after the vanilla body-anchored check, also treat the entity as
     * visible if it sits in a chunk being streamed to the player around their detached view origin
     * (the UAV they control/preview). Makes entities around a far UAV visible to its operator. The
     * deterministic re-evaluation that fires this as the UAV moves is driven by
     * {@code UavChunkStreamer} via {@code EntityTracker.updateVisibility}.
     */
    @ModifyReturnValue(method = "isVisibleTo(Lnet/minecraft/entity/player/EntityPlayerMP;)Z", at = @At("RETURN"))
    private boolean mcheli$orVisibleFromViewOrigin(boolean original, EntityPlayerMP playerMP) {
        return original || TrackerHook.isVisibleFromViewOrigin(this.trackedEntity, playerMP);
    }

    @Inject(
            method = "isPlayerWatchingThisChunk(Lnet/minecraft/entity/player/EntityPlayerMP;)Z",
            at = @At("HEAD"),
            cancellable = true)
    private void mcheli$forceWatch(EntityPlayerMP playerMP, CallbackInfoReturnable<Boolean> cir) {
        if (TrackerHook.shouldForceWatch(this.trackedEntity)) {
            cir.setReturnValue(true);
        }
    }
}
