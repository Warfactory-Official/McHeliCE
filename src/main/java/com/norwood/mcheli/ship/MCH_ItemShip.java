package com.norwood.mcheli.ship;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.aircraft.MCH_ItemAircraft;

public class MCH_ItemShip extends MCH_ItemAircraft {

    public MCH_ItemShip(int par1) {
        super(par1);
        this.maxStackSize = 1;
    }

    @Override
    public MCH_AircraftInfo getAircraftInfo() {
        return MCH_ShipInfoManager.getFromItem(this);
    }

    @Nullable
    public MCH_EntityShip createAircraft(World world, double x, double y, double z, ItemStack itemStack) {
        MCH_ShipInfo info = MCH_ShipInfoManager.getFromItem(this);
        if (info == null) {
            MCH_Lib.Log(world, "##### MCH_EntityShip Plane info null %s", this.getTranslationKey());
            return null;
        } else {
            MCH_EntityShip plane = new MCH_EntityShip(world);
            plane.setPosition(x, y, z);
            plane.prevPosX = x;
            plane.prevPosY = y;
            plane.prevPosZ = z;
            plane.camera.setPosition(x, y, z);
            plane.setTypeName(info.name);
            if (!world.isRemote) {
                plane.setTextureName(info.getTextureName());
            }

            return plane;
        }
    }
}
