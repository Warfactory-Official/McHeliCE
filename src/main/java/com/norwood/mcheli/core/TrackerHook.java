package com.norwood.mcheli.core;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.wrapper.W_Entity;
import net.minecraft.entity.Entity;

@SuppressWarnings("unused")
public class TrackerHook {

    public static int getRenderDistance(Entity entity, int range, int maxRange) {
        if (entity instanceof W_Entity) return range;
        return Math.min(range, maxRange);
    }

    public static boolean shouldForceWatch(Entity entity) {
        return entity instanceof W_Entity;
    }
}
