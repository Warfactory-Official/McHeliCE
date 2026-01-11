package com.norwood.mcheli.factories;

import com.cleanroommc.modularui.factory.EntityGuiData;
import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public class AircraftGuiData extends EntityGuiData {
    final MCH_AircraftInfo info;

    public AircraftGuiData(EntityPlayer player, Entity guiHolder, MCH_AircraftInfo info) {
        super(player, guiHolder);
        this.info = info;
    }
}
