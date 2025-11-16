package com.norwood.mcheli.parachute;

import com.norwood.mcheli.wrapper.W_Item;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public class MCH_ItemParachute extends W_Item {

    public MCH_ItemParachute(int par1) {
        super(par1);
        this.maxStackSize = 1;
    }

    public @NotNull ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player,
                                                             @NotNull EnumHand handIn) {
        ItemStack itemstack = player.getHeldItem(handIn);
        if (!world.isRemote && player.getRidingEntity() == null && !player.onGround) {
            double x = player.posX + 0.5;
            double y = player.posY + 3.5;
            double z = player.posZ + 0.5;
            MCH_EntityParachute entity = new MCH_EntityParachute(world, x, y, z);
            entity.rotationYaw = player.rotationYaw;
            entity.motionX = player.motionX;
            entity.motionY = player.motionY;
            entity.motionZ = player.motionZ;
            entity.fallDistance = player.fallDistance;
            player.fallDistance = 0.0F;
            entity.user = player;
            entity.setType(1);
            world.spawnEntity(entity);
        }

        if (!player.capabilities.isCreativeMode) {
            itemstack.shrink(1);
        }

        return ActionResult.newResult(EnumActionResult.SUCCESS, itemstack);
    }
}
