package com.norwood.mcheli.container;

import com.norwood.mcheli.wrapper.W_Item;
import com.norwood.mcheli.wrapper.W_MovingObjectPosition;
import com.norwood.mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MCH_ItemContainer extends W_Item {

    public MCH_ItemContainer(int par1) {
        super(par1);
        this.setMaxStackSize(1);
    }

    public @NotNull ActionResult<ItemStack> onItemRightClick(@NotNull World worldIn, EntityPlayer playerIn,
                                                             @NotNull EnumHand handIn) {
        ItemStack itemstack = playerIn.getHeldItem(handIn);
        float f = 1.0F;
        float f1 = playerIn.prevRotationPitch + (playerIn.rotationPitch - playerIn.prevRotationPitch) * f;
        float f2 = playerIn.prevRotationYaw + (playerIn.rotationYaw - playerIn.prevRotationYaw) * f;
        double d0 = playerIn.prevPosX + (playerIn.posX - playerIn.prevPosX) * f;
        double d1 = playerIn.prevPosY + (playerIn.posY - playerIn.prevPosY) * f + playerIn.getEyeHeight();
        double d2 = playerIn.prevPosZ + (playerIn.posZ - playerIn.prevPosZ) * f;
        Vec3d vec3 = new Vec3d(d0, d1, d2);
        float f3 = MathHelper.cos(-f2 * (float) (Math.PI / 180.0) - (float) Math.PI);
        float f4 = MathHelper.sin(-f2 * (float) (Math.PI / 180.0) - (float) Math.PI);
        float f5 = -MathHelper.cos(-f1 * (float) (Math.PI / 180.0));
        float f6 = MathHelper.sin(-f1 * (float) (Math.PI / 180.0));
        float f7 = f4 * f5;
        float f8 = f3 * f5;
        double d3 = 5.0;
        Vec3d vec31 = vec3.add(f7 * d3, f6 * d3, f8 * d3);
        RayTraceResult movingobjectposition = W_WorldFunc.clip(worldIn, vec3, vec31, true);
        if (movingobjectposition == null) {
            return ActionResult.newResult(EnumActionResult.PASS, itemstack);
        } else {
            Vec3d vec32 = playerIn.getLook(f);
            boolean flag = false;
            float f9 = 1.0F;
            List<Entity> list = worldIn.getEntitiesWithinAABBExcludingEntity(
                    playerIn,
                    playerIn.getEntityBoundingBox().expand(vec32.x * d3, vec32.y * d3, vec32.z * d3).grow(f9, f9, f9));

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
                    int ix = movingobjectposition.getBlockPos().getX();
                    int j = movingobjectposition.getBlockPos().getY();
                    int k = movingobjectposition.getBlockPos().getZ();
                    MCH_EntityContainer entityboat = new MCH_EntityContainer(worldIn, ix + 0.5F, j + 1.0F, k + 0.5F);
                    entityboat.rotationYaw = ((MathHelper.floor(playerIn.rotationYaw * 4.0F / 360.0F + 0.5) & 3) - 1) *
                            90;
                    if (!worldIn.getCollisionBoxes(entityboat, entityboat.getEntityBoundingBox().grow(-0.1, -0.1, -0.1))
                            .isEmpty()) {
                        return ActionResult.newResult(EnumActionResult.FAIL, itemstack);
                    }

                    if (!worldIn.isRemote) {
                        worldIn.spawnEntity(entityboat);
                    }

                    if (!playerIn.capabilities.isCreativeMode) {
                        itemstack.shrink(1);
                    }
                }

                return ActionResult.newResult(EnumActionResult.SUCCESS, itemstack);
            }
        }
    }
}
