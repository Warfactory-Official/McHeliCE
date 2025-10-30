package com.norwood.mcheli.compat.hbm;

import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.network.NetworkRegistry;


public class HBMUtil {


    @Optional.Method(modid = "hbm")
    public static void explodeMuke(World world, double posX, double posY, double posZ, String mukeType, boolean effectOnly) {
        if (effectOnly) {
            NBTTagCompound data = new NBTTagCompound();
            data.setString("type", "muke");
            com.hbm.handler.threading.PacketThreading.createAllAroundThreadedPacket(
                    new com.hbm.packet.toclient.AuxParticlePacketNT(data, posX, posY + 0.5, posZ),
                    new NetworkRegistry.TargetPoint(world.provider.getDimension(), posX, posY, posZ, 250)
            );
            return;
        }
        //TODO:Custom muke handling
        var type = switch (mukeType) {
            case "TOTS" -> com.hbm.explosion.ExplosionNukeSmall.PARAMS_TOTS;
            case "LOW" -> com.hbm.explosion.ExplosionNukeSmall.PARAMS_LOW;
            case "MEDIUM" -> com.hbm.explosion.ExplosionNukeSmall.PARAMS_MEDIUM;
            case "HIGH" -> com.hbm.explosion.ExplosionNukeSmall.PARAMS_HIGH;
            default -> com.hbm.explosion.ExplosionNukeSmall.PARAMS_SAFE;
        };
        com.hbm.explosion.ExplosionNukeSmall.explode(world, posX, posY, posZ, type);
    }


    @Optional.Method(modid = "hbm")
    public static void EntityNukeExplosionMK5(World world, int radius, double x, double y, double z, boolean effectOnly) {
        if (!effectOnly) {
            var mk5 = EntityNukeExplosionMK5.statFac(world, radius, x, y, z);
            world.spawnEntity(mk5);
        }
        EntityNukeTorex.statFac(world, x, y, z, radius);
    }


    @Optional.Method(modid = "hbm")
    public static void ExplosionChaos_spawnChlorine(World world, double posX, double posY, double posZ, ChemicalContainer container) {
        com.hbm.explosion.ExplosionChaos.spawnChlorine(world, posX, posY, posZ, container.count, container.speed, container.type.ordinal());
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



}


