package com.norwood.mcheli.compat.oneprobe;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import mcjty.theoneprobe.TheOneProbe;
import mcjty.theoneprobe.api.IEntityDisplayOverride;
import mcjty.theoneprobe.api.IProbeHitEntityData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.ProbeMode;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;

@Optional.Interface(iface = "mcjty.theoneprobe.api.IProbeInfoProvider", modid = "theoneprobe")
public class AircraftInfoProvider implements IEntityDisplayOverride {

    @Optional.Method(modid = "theoneprobe")
    public static void register() {
        TheOneProbe.theOneProbeImp.registerEntityDisplayOverride(new AircraftInfoProvider());
    }


    @Override
    @Optional.Method(modid = "theoneprobe")
    public boolean overrideStandardInfo(ProbeMode probeMode, IProbeInfo iProbeInfo, EntityPlayer entityPlayer, World world, Entity entity, IProbeHitEntityData iProbeHitEntityData) {
        if (entity instanceof MCH_EntityAircraft aircraft) {
            var info = aircraft.getAcInfo();
            if(info == null) return false;
            iProbeInfo.entity(aircraft, iProbeInfo.defaultEntityStyle().scale(info.oneProbeScale));
            return true;
        }
        return false;

    }
}
