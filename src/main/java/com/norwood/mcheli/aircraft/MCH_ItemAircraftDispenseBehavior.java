package com.norwood.mcheli.aircraft;

import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

import org.jetbrains.annotations.NotNull;

import com.norwood.mcheli.MCH_Lib;

public class MCH_ItemAircraftDispenseBehavior extends BehaviorDefaultDispenseItem {

    public @NotNull ItemStack dispenseStack(IBlockSource bs, ItemStack itemStack) {
        EnumFacing enumfacing = bs.getBlockState().getValue(BlockDispenser.FACING);
        double x = bs.getX() + enumfacing.getXOffset() * 2.0;
        double y = bs.getY() + enumfacing.getYOffset() * 2.0;
        double z = bs.getZ() + enumfacing.getZOffset() * 2.0;
        if (itemStack.getItem() instanceof MCH_ItemAircraft) {
            MCH_EntityAircraft ac = ((MCH_ItemAircraft) itemStack.getItem()).onTileClick(itemStack, bs.getWorld(), 0.0F,
                    (int) x, (int) y, (int) z);
            if (ac != null && ac.getAcInfo() != null && !ac.getAcInfo().creativeOnly && !ac.isUAV()) {
                if (!bs.getWorld().isRemote) {
                    ac.getAcDataFromItem(itemStack);
                    bs.getWorld().spawnEntity(ac);
                }

                itemStack.splitStack(1);
                MCH_Lib.DbgLog(bs.getWorld(),
                        "dispenseStack:x=%.1f,y=%.1f,z=%.1f;dir=%s:item=" + itemStack.getDisplayName(), x, y, z,
                        enumfacing.toString());
            }
        }

        return itemStack;
    }
}
