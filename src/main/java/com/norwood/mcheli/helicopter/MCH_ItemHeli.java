package com.norwood.mcheli.helicopter;

import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.aircraft.MCH_ItemAircraft;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class MCH_ItemHeli extends MCH_ItemAircraft {

    public MCH_ItemHeli(int par1) {
        super(par1);
        this.maxStackSize = 1;
    }

    @Nullable
    @Override
    public MCH_AircraftInfo getAircraftInfo() {
        return MCH_HeliInfoManager.getFromItem(this);
    }

    @Nullable
    public MCH_EntityHeli createAircraft(World world, double x, double y, double z, ItemStack itemStack) {
        MCH_HeliInfo info = MCH_HeliInfoManager.getFromItem(this);
        if (info == null) {
            MCH_Lib.Log(world, "##### MCH_ItemHeli Heli info null %s", this.getTranslationKey());
            return null;
        } else {
            MCH_EntityHeli heli = new MCH_EntityHeli(world);
            heli.setPosition(x, y, z);
            heli.prevPosX = x;
            heli.prevPosY = y;
            heli.prevPosZ = z;
            heli.camera.setPosition(x, y, z);
            heli.setTypeName(info.name);
            if (!world.isRemote) {
                heli.setTextureName(info.getTextureName());
            }

            return heli;
        }
    }
}
