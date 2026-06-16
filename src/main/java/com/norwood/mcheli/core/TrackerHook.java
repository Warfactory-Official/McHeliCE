package com.norwood.mcheli.core;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.uav.IUavStation;
import com.norwood.mcheli.wingman.handler.UavChunkStreamer;
import com.norwood.mcheli.wrapper.W_Entity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;

@SuppressWarnings("unused")
public class TrackerHook {

    /**
     * Effectively-unlimited entity tracking range (blocks) applied to a UAV while it is being
     * controlled or previewed, so the operating/viewing client receives (and can render) the UAV
     * entity far beyond the normal registered tracking range (1200). Called from
     * {@link com.norwood.mcheli.mixin.MixinEntityTrackerEntry}, which redirects the
     * {@code Math.min} in {@code EntityTrackerEntry#isVisibleTo}.
     */
    private static final int UAV_FORCE_TRACK_RANGE = 1_000_000;

    public static int getRenderDistance(Entity entity, int range, int maxRange) {
        if (entity instanceof W_Entity) {
            if (entity instanceof MCH_EntityAircraft uav && uav.isUAV() && isUavForceTracked(uav)) {
                return UAV_FORCE_TRACK_RANGE;
            }
            return range;
        }
        return Math.min(range, maxRange);
    }

    public static boolean shouldForceWatch(Entity entity) {
        return entity instanceof W_Entity;
    }

    /**
     * Camera-aware visibility (the player's view is detached from their body). An entity is also
     * visible to a player if it sits in a chunk currently being streamed to that player around their
     * detached view origin — i.e. the UAV they are controlling or previewing. This is what lets the
     * operator of a far UAV see the mobs / players / other entities around it, not just the UAV
     * itself; the body-anchored vanilla range check never reaches that far. OR-ed into the result of
     * {@code EntityTrackerEntry#isVisibleTo} by {@link com.norwood.mcheli.mixin.MixinEntityTrackerEntry}.
     */
    public static boolean isVisibleFromViewOrigin(Entity entity, EntityPlayerMP player) {
        if (entity == null || player == null) {
            return false;
        }
        return UavChunkStreamer.isChunkStreamedTo(player.getUniqueID(), entity.chunkCoordX, entity.chunkCoordZ);
    }

    /** A UAV is force-tracked while it is actively controlled, or previewed in a station screen. */
    private static boolean isUavForceTracked(MCH_EntityAircraft uav) {
        IUavStation station = uav.getUavStation();
        if (station != null && station.getOperator() != null) {
            return true;
        }
        return UavChunkStreamer.isPreviewed(uav.getUniqueID());
    }
}
