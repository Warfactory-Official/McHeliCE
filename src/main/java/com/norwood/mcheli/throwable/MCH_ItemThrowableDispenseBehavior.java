package com.norwood.mcheli.throwable;

import com.norwood.mcheli.MCH_Lib;
import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import org.jetbrains.annotations.NotNull;

public class MCH_ItemThrowableDispenseBehavior extends BehaviorDefaultDispenseItem {

    public @NotNull ItemStack dispenseStack(IBlockSource bs, ItemStack itemStack) {
        EnumFacing enumfacing = bs.getBlockState().getValue(BlockDispenser.FACING);
        double x = bs.getX() + enumfacing.getXOffset() * 2.0;
        double y = bs.getY() + enumfacing.getYOffset() * 2.0;
        double z = bs.getZ() + enumfacing.getZOffset() * 2.0;
        if (itemStack.getItem() instanceof MCH_ItemThrowable) {
            MCH_ThrowableInfo info = MCH_ThrowableInfoManager.get(itemStack.getItem());
            if (info != null) {
                bs.getWorld()
                        .playSound(x, y, z, SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.BLOCKS, 0.5F,
                                0.4F / (bs.getWorld().rand.nextFloat() * 0.4F + 0.8F), false);
                if (!bs.getWorld().isRemote) {
                    MCH_Lib.DbgLog(bs.getWorld(), "MCH_ItemThrowableDispenseBehavior.dispenseStack(%s)", info.name);
                    MCH_EntityThrowable entity = new MCH_EntityThrowable(bs.getWorld(), x, y, z);
                    entity.motionX = enumfacing.getXOffset() * info.dispenseAcceleration;
                    entity.motionY = enumfacing.getYOffset() * info.dispenseAcceleration;
                    entity.motionZ = enumfacing.getZOffset() * info.dispenseAcceleration;
                    entity.setInfo(info);
                    bs.getWorld().spawnEntity(entity);
                    itemStack.splitStack(1);
                }
            }
        }

        return itemStack;
    }
}
