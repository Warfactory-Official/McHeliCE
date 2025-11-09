package com.norwood.mcheli.compat.hbm;

import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;

import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityNukeExplosionMK5;

public class HBMUtil {

    @Optional.Method(modid = "hbm")
    public static void EntityNukeExplosionMK5(World world, int radius, double x, double y, double z,
                                              boolean effectOnly) {
        if (!effectOnly) {
            var mk5 = EntityNukeExplosionMK5.statFac(world, radius, x, y, z);
            world.spawnEntity(mk5);
        }
        EntityNukeTorex.statFac(world, x, y, z, radius);
    }

    @Optional.Method(modid = "hbm")
    public static void ExplosionChaos_spawnChlorine(World world, double posX, double posY, double posZ,
                                                    ChemicalContainer container) {
        com.hbm.explosion.ExplosionChaos.spawnChlorine(world, posX, posY, posZ, container.count, container.speed,
                container.type.ordinal());
    }
}
