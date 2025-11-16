package com.norwood.mcheli.plane;

import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.aircraft.MCH_ItemAircraft;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class MCP_ItemPlane extends MCH_ItemAircraft {

    public MCP_ItemPlane(int par1) {
        super(par1);
        this.maxStackSize = 1;
    }

    @Override
    public MCH_AircraftInfo getAircraftInfo() {
        return MCP_PlaneInfoManager.getFromItem(this);
    }

    @Nullable
    public MCH_EntityPlane createAircraft(World world, double x, double y, double z, ItemStack itemStack) {
        MCH_PlaneInfo info = MCP_PlaneInfoManager.getFromItem(this);
        if (info == null) {
            MCH_Lib.Log(world, "##### MCP_EntityPlane Plane info null %s", this.getTranslationKey());
            return null;
        } else {
            MCH_EntityPlane plane = new MCH_EntityPlane(world);
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
