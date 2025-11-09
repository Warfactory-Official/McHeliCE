package com.norwood.mcheli.helper;

import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.registry.EntityRegistry;

public class MCH_Entities {

    public static <T extends Entity> void register(
                                                   Class<T> entityClass, String entityName, int id, Object mod,
                                                   int trackingRange, int updateFrequency,
                                                   boolean sendsVelocityUpdates) {
        EntityRegistry.registerModEntity(MCH_Utils.suffix(entityName), entityClass, entityName, id, mod, trackingRange,
                updateFrequency, sendsVelocityUpdates);
    }
}
