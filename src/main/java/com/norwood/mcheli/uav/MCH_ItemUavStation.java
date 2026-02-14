package com.norwood.mcheli.uav;

import com.norwood.mcheli.MCH_Lib;
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

public class MCH_ItemUavStation extends W_Item {
    public final IUavStation.StationType type;

    public MCH_ItemUavStation(int par1, IUavStation.StationType type) {
        super(par1);
        this.maxStackSize = 1;
        this.type = type;
    }

    @Override
    public @NotNull ActionResult<ItemStack> onItemRightClick(@NotNull World world, EntityPlayer player, @NotNull EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        RayTraceResult raytraceresult = this.rayTrace(world, player, true);
        if (!W_MovingObjectPosition.isHitTypeTile(raytraceresult)) {
            return ActionResult.newResult(EnumActionResult.PASS, stack);
        }

        double reach = 5.0;
        Vec3d look = player.getLook(1.0F);
        AxisAlignedBB checkArea = player.getEntityBoundingBox().expand(look.x * reach, look.y * reach, look.z * reach).grow(1.0);

        boolean isObstructed = world.getEntitiesWithinAABBExcludingEntity(player, checkArea).stream()
                .anyMatch(e -> e.canBeCollidedWith() && e.getEntityBoundingBox().grow(e.getCollisionBorderSize()).contains(player.getPositionEyes(1.0F)));

        if (isObstructed) {
            return ActionResult.newResult(EnumActionResult.FAIL, stack);
        }

        BlockPos pos = raytraceresult.getBlockPos();
        MCH_EntityUavStation uavStation =new MCH_EntityUavStation(world);
        uavStation.setPosition(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        uavStation.setType(type);

        int rot = (int) (MCH_Lib.getRotate360(player.rotationYaw) + 45.0);
        uavStation.rotationYaw = (float) (rot / 90 * 90 - 180);
        uavStation.initUavPostion();

        if (!world.getCollisionBoxes(uavStation, uavStation.getEntityBoundingBox().grow(-0.1)).isEmpty()) {
            return ActionResult.newResult(EnumActionResult.FAIL, stack);
        }

        if (!world.isRemote) {
            world.spawnEntity(uavStation);
        }

        if (!player.capabilities.isCreativeMode) {
            stack.shrink(1);
        }

        return ActionResult.newResult(EnumActionResult.SUCCESS, stack);
    }
}
