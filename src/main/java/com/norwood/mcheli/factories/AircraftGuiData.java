package com.norwood.mcheli.factories;

import com.cleanroommc.modularui.factory.EntityGuiData;
import com.cleanroommc.modularui.factory.GuiData;
import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public class AircraftGuiData extends GuiData {
    final MCH_AircraftInfo info;
    final MCH_EntityAircraft guiHolder;
    final boolean containerOnly;
    public AircraftGuiData(EntityPlayer player, MCH_EntityAircraft guiHolder, MCH_AircraftInfo info, boolean containerOnly) {
        super(player);
        this.info = info;
        this.guiHolder = guiHolder;
        this.containerOnly = containerOnly;
    }

    public MCH_AircraftInfo getInfo() {
        return info;
    }

    public MCH_EntityAircraft getGuiHolder() {
        return guiHolder;
    }

    public boolean isContainerOnly() {
        return containerOnly;
    }
}
