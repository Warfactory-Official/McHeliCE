package com.norwood.mcheli.aircraft.components;

import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import net.minecraft.nbt.NBTTagCompound;

public interface IAircraftComponent {
    public MCH_EntityAircraft getParent();
    void init();
    public default MCH_AircraftInfo getAcInfo(){return getParent().getAcInfo();}
    void onUpdate();
    void readFromNBT(NBTTagCompound compound);
    void writeToNBT(NBTTagCompound compound);
}
