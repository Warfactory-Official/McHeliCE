package com.norwood.mcheli.compat.hbm;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;

public class HBMUtil {







    @Optional.Method(modid = "hbm")
    public static Entity EntityNukeExplosionMK5_statFac(World world, int r, double posX, double posY, double posZ) {
        return com.hbm.entity.logic.EntityNukeExplosionMK5.statFac(world, r, posX, posY, posZ);
    }

    @Optional.Method(modid = "hbm")
    public static void EntityNukeTorex_statFac(World world, double posX, double posY, double posZ, float nukeYield) {
        com.hbm.entity.effect.EntityNukeTorex.statFac(world, posX, posY, posZ, nukeYield);
    }

    @Optional.Method(modid = "hbm")
    public static void ExplosionChaos_spawnChlorine(World world, double posX, double posY, double posZ, float chemYield, double chemSpeed, int chemType) {
        com.hbm.explosion.ExplosionChaos.spawnChlorine(world, posX, posY, posZ, (int) chemYield, chemSpeed, chemType);
    }

    @Optional.Method(modid = "hbm")
    public static void ExplosionCreator_composeEffectStandard(World world, double posX, double posY, double posZ, int explosionBlockSize) {
        if (explosionBlockSize < 50) {
            com.hbm.particle.helper.ExplosionCreator.composeEffectSmall(world, posX, posY, posZ);
        } else if (explosionBlockSize < 100) {
            com.hbm.particle.helper.ExplosionCreator.composeEffectStandard(world, posX, posY, posZ);
        } else {
            com.hbm.particle.helper.ExplosionCreator.composeEffectLarge(world, posX, posY, posZ);
        }
    }


    @Optional.Interface(iface = "com.hbm.explosion.ExplosionNT", modid = "hbm")
    public static class ExplosionNTWrapper {
        private final com.hbm.explosion.ExplosionNT instance;

        public ExplosionNTWrapper(World world, Entity entity, double posX, double posY, double posZ, float explosionPower) {
            this.instance = new com.hbm.explosion.ExplosionNT(world, entity, posX, posY, posZ, explosionPower);
        }

        @Optional.Method(modid = "hbm")
        public void overrideResolutionAndExplode(int resolution) {
            instance.overrideResolution(resolution);
            instance.explode();
        }

        @Optional.Method(modid = "hbm")
        public void addAttrib(String attrib) {
            com.hbm.explosion.ExplosionNT.ExAttrib enumAttrib =
                    com.hbm.explosion.ExplosionNT.ExAttrib.valueOf(attrib);
            instance.addAttrib(enumAttrib);
        }
    }
}


