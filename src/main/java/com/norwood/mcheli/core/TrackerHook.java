package com.norwood.mcheli.core;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import net.minecraft.entity.Entity;

@SuppressWarnings("unused")
public class TrackerHook {

    public static int getRenderDistance(Entity entity, int range, int maxRange) {
        if (entity instanceof MCH_EntityAircraft) return range;
        return Math.min(range, maxRange);
    }
}
