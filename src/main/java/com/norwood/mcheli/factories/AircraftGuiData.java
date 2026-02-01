package com.norwood.mcheli.factories;

import com.cleanroommc.modularui.factory.EntityGuiData;
import com.cleanroommc.modularui.factory.GuiData;
import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public class AircraftGuiData extends GuiData {
    @Getter
    final MCH_AircraftInfo info;
    @Getter
    final MCH_EntityAircraft guiHolder;
    public AircraftGuiData(EntityPlayer player, MCH_EntityAircraft guiHolder, MCH_AircraftInfo info) {
        super(player);
        this.info = info;
        this.guiHolder = guiHolder;
    }
}
