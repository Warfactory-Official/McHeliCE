package com.norwood.mcheli.compat.hbm;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;


public final class HBMUtil {

    private HBMUtil() {}

    private static final String C_NUKE_MK5 = "com.hbm.entity.logic.EntityNukeExplosionMK5";
    private static final String C_NUKE_TOREX = "com.hbm.entity.effect.EntityNukeTorex";
    private static final String C_EXPLOSION_CHAOS = "com.hbm.explosion.ExplosionChaos";

    public static void nukeMK5(World world, int radius, double x, double y, double z, boolean effectOnly) {
        if (!HBMReflect.available()) return;
        if (!effectOnly) {
            Object mk5 = HBMReflect.callStatic(C_NUKE_MK5, "statFac", world, radius, x, y, z);
            if (mk5 instanceof Entity e) {
                world.spawnEntity(e);
            }
        }
        HBMReflect.callStatic(C_NUKE_TOREX, "statFac", world, x, y, z, (float) radius);
    }

    public static void spawnChlorine(World world, double x, double y, double z, ChemicalContainer container) {
        if (container == null || !HBMReflect.available()) return;
        HBMReflect.callStatic(C_EXPLOSION_CHAOS, "spawnChlorine",
                world, x, y, z, container.count, container.speed, container.type.ordinal());
    }
}
