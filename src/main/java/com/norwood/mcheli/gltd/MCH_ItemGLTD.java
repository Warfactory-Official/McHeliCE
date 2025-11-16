package com.norwood.mcheli.gltd;

import com.norwood.mcheli.wrapper.W_Item;
import com.norwood.mcheli.wrapper.W_MovingObjectPosition;
import com.norwood.mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MCH_ItemGLTD extends W_Item {

    public MCH_ItemGLTD(int par1) {
        super(par1);
        this.maxStackSize = 1;
    }

    public @NotNull ActionResult<ItemStack> onItemRightClick(@NotNull World par2World, EntityPlayer par3EntityPlayer,
                                                             @NotNull EnumHand handIn) {
        ItemStack itemstack = par3EntityPlayer.getHeldItem(handIn);
        float f = 1.0F;
        float f1 = par3EntityPlayer.prevRotationPitch +
                (par3EntityPlayer.rotationPitch - par3EntityPlayer.prevRotationPitch) * f;
        float f2 = par3EntityPlayer.prevRotationYaw +
                (par3EntityPlayer.rotationYaw - par3EntityPlayer.prevRotationYaw) * f;
        double d0 = par3EntityPlayer.prevPosX + (par3EntityPlayer.posX - par3EntityPlayer.prevPosX) * f;
        double d1 = par3EntityPlayer.prevPosY + (par3EntityPlayer.posY - par3EntityPlayer.prevPosY) * f +
                par3EntityPlayer.getEyeHeight();
        double d2 = par3EntityPlayer.prevPosZ + (par3EntityPlayer.posZ - par3EntityPlayer.prevPosZ) * f;
        Vec3d vec3 = new Vec3d(d0, d1, d2);
        float f3 = MathHelper.cos(-f2 * (float) (Math.PI / 180.0) - (float) Math.PI);
        float f4 = MathHelper.sin(-f2 * (float) (Math.PI / 180.0) - (float) Math.PI);
        float f5 = -MathHelper.cos(-f1 * (float) (Math.PI / 180.0));
        float f6 = MathHelper.sin(-f1 * (float) (Math.PI / 180.0));
        float f7 = f4 * f5;
        float f8 = f3 * f5;
        double d3 = 5.0;
        Vec3d vec31 = vec3.add(f7 * d3, f6 * d3, f8 * d3);
        RayTraceResult movingobjectposition = W_WorldFunc.clip(par2World, vec3, vec31, true);
        if (movingobjectposition == null) {
            return ActionResult.newResult(EnumActionResult.PASS, itemstack);
        } else {
            Vec3d vec32 = par3EntityPlayer.getLook(f);
            boolean flag = false;
            float f9 = 1.0F;
            List<Entity> list = par2World.getEntitiesWithinAABBExcludingEntity(
                    par3EntityPlayer, par3EntityPlayer.getEntityBoundingBox()
                            .expand(vec32.x * d3, vec32.y * d3, vec32.z * d3).grow(f9, f9, f9));

            for (Entity entity : list) {
                if (entity.canBeCollidedWith()) {
                    float f10 = entity.getCollisionBorderSize();
                    AxisAlignedBB axisalignedbb = entity.getEntityBoundingBox().grow(f10, f10, f10);
                    if (axisalignedbb.contains(vec3)) {
                        flag = true;
                    }
                }
            }

            if (flag) {
                return ActionResult.newResult(EnumActionResult.FAIL, itemstack);
            } else {
                if (W_MovingObjectPosition.isHitTypeTile(movingobjectposition)) {
                    BlockPos blockpos = movingobjectposition.getBlockPos();
                    int ix = blockpos.getX();
                    int j = blockpos.getY();
                    int k = blockpos.getZ();
                    MCH_EntityGLTD entityboat = new MCH_EntityGLTD(par2World, ix + 0.5F, j + 1.0F, k + 0.5F);
                    entityboat.rotationYaw = par3EntityPlayer.rotationYaw;
                    if (!par2World
                            .getCollisionBoxes(entityboat, entityboat.getEntityBoundingBox().grow(-0.1, -0.1, -0.1))
                            .isEmpty()) {
                        return ActionResult.newResult(EnumActionResult.FAIL, itemstack);
                    }

                    if (!par2World.isRemote) {
                        par2World.spawnEntity(entityboat);
                    }

                    if (!par3EntityPlayer.capabilities.isCreativeMode) {
                        itemstack.shrink(1);
                    }
                }

                return ActionResult.newResult(EnumActionResult.SUCCESS, itemstack);
            }
        }
    }
}
