package com.norwood.mcheli.aircraft;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.helper.MCH_CriteriaTriggers;
import com.norwood.mcheli.weapon.MCH_WeaponSet;
import com.norwood.mcheli.wrapper.W_EntityPlayer;
import com.norwood.mcheli.wrapper.W_Item;
import com.norwood.mcheli.wrapper.W_MovingObjectPosition;
import com.norwood.mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockSponge;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecartEmpty;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.*;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class MCH_ItemAircraft extends W_Item {

    private static final boolean isRegistedDispenseBehavior = false;

    boolean BLOCK = true;

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        if (worldIn == null) return;
        MCH_AircraftInfo info = this.getAircraftInfo().category.equals("zzz") ? null : this.getAircraftInfo();
        MCH_EntityAircraft ac = createAircraft(worldIn, -1.0D, -1.0D, -1.0D, stack);

        if (info != null && ac != null) {
            tooltip.add(TextFormatting.YELLOW + "Category: " + info.category);
            tooltip.add(Arrays.stream(ac.weapons).map(MCH_WeaponSet::getName).collect(Collectors.joining(", ")));
        }

        if (ac != null && ac.isNewUAV()) {
            tooltip.add(TextFormatting.RED + "DANGER!");
            tooltip.add(TextFormatting.RED + "This drone has a new UAV mechanic!");
            tooltip.add(TextFormatting.RED + "It may contain a lot of bugs!");
            tooltip.add(TextFormatting.RED + "Clear your inventory before use!");
        }

        super.addInformation(stack, worldIn, tooltip, flagIn);
    }

    public MCH_ItemAircraft(int i) {
        super(i);
    }

    public static void registerDispenseBehavior(Item item) {
        if (!isRegistedDispenseBehavior) {
            BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(item, new MCH_ItemAircraftDispenseBehavior());
        }
    }

    @Nullable
    public abstract MCH_AircraftInfo getAircraftInfo();

    @Nullable
    public abstract MCH_EntityAircraft createAircraft(World var1, double var2, double var4, double var6,
                                                      ItemStack var8);

    public MCH_EntityAircraft onTileClick(ItemStack itemStack, World world, float rotationYaw, int x, int y, int z) {
        MCH_EntityAircraft ac = this.createAircraft(world, x + 0.5F, y + 1.0F, z + 0.5F, itemStack);
        if (ac == null) {
            return null;
        } else {
            ac.initRotationYaw(((MathHelper.floor(rotationYaw * 4.0F / 360.0F + 0.5) & 3) - 1) * 90);
            return !world.getCollisionBoxes(ac, ac.getEntityBoundingBox().grow(-0.1, -0.1, -0.1)).isEmpty() ? null : ac;
        }
    }

    public String toString() {
        MCH_AircraftInfo info = this.getAircraftInfo();
        return info != null ? super.toString() + "(" + info.getDirectoryName() + ":" + info.name + ")" :
                super.toString() + "(null)";
    }

    public @NotNull ActionResult<ItemStack> onItemRightClick(@NotNull World world, EntityPlayer player,
                                                             @NotNull EnumHand handIn) {
        ItemStack itemstack = player.getHeldItem(handIn);
        float f = 1.0F;
        float f1 = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * f;
        float f2 = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * f;
        double d0 = player.prevPosX + (player.posX - player.prevPosX) * f;
        double d1 = player.prevPosY + (player.posY - player.prevPosY) * f + 1.62;
        double d2 = player.prevPosZ + (player.posZ - player.prevPosZ) * f;
        Vec3d vec3 = new Vec3d(d0, d1, d2);
        float f3 = MathHelper.cos(-f2 * (float) (Math.PI / 180.0) - (float) Math.PI);
        float f4 = MathHelper.sin(-f2 * (float) (Math.PI / 180.0) - (float) Math.PI);
        float f5 = -MathHelper.cos(-f1 * (float) (Math.PI / 180.0));
        float f6 = MathHelper.sin(-f1 * (float) (Math.PI / 180.0));
        float f7 = f4 * f5;
        float f8 = f3 * f5;
        double d3 = 5.0;
        Vec3d vec31 = vec3.add(f7 * d3, f6 * d3, f8 * d3);
        RayTraceResult mop = W_WorldFunc.clip(world, vec3, vec31, true);
        if (mop == null) {
            return ActionResult.newResult(EnumActionResult.PASS, itemstack);
        } else {
            Vec3d playerLook = player.getLook(f);
            boolean flag = false;
            float f9 = 1.0F;
            List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(
                    player, player.getEntityBoundingBox()
                            .expand(playerLook.x * d3, playerLook.y * d3, playerLook.z * d3).grow(f9, f9, f9));

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
                return ActionResult.newResult(EnumActionResult.PASS, itemstack);
            } else {
                if (W_MovingObjectPosition.isHitTypeTile(mop)) {
                    if (MCH_Config.PlaceableOnSpongeOnly.prmBool) {
                        MCH_AircraftInfo acInfo = this.getAircraftInfo();
                        if (acInfo != null && acInfo.isFloat && !acInfo.canMoveOnGround) {
                            if (world.getBlockState(mop.getBlockPos().down()).getBlock() != Blocks.SPONGE) {
                                return ActionResult.newResult(EnumActionResult.FAIL, itemstack);
                            }

                            for (int x = -1; x <= 1; x++) {
                                for (int z = -1; z <= 1; z++) {
                                    if (world.getBlockState(mop.getBlockPos().add(x, 0, z)).getBlock() !=
                                            Blocks.WATER) {
                                        return ActionResult.newResult(EnumActionResult.FAIL, itemstack);
                                    }
                                }
                            }
                        } else {
                            Block block = world.getBlockState(mop.getBlockPos()).getBlock();
                            if (!(block instanceof BlockSponge)) {
                                return ActionResult.newResult(EnumActionResult.FAIL, itemstack);
                            }
                        }
                    }

                    this.spawnAircraft(itemstack, world, player, mop.getBlockPos());
                }

                return ActionResult.newResult(EnumActionResult.SUCCESS, itemstack);
            }
        }
    }

    public MCH_EntityAircraft spawnAircraft(ItemStack itemStack, World world, EntityPlayer player, BlockPos blockpos) {
        MCH_EntityAircraft ac = this.onTileClick(
                itemStack, world, player.rotationYaw, blockpos.getX(), blockpos.getY(), blockpos.getZ());
        if (ac != null) {
            if (ac.getAcInfo() != null && ac.getAcInfo().creativeOnly && !player.capabilities.isCreativeMode) {
                return null;
            }

            if (ac.isUAV()) {
                if (world.isRemote) {
                    if (ac.isSmallUAV()) {
                        W_EntityPlayer.addChatMessage(player, "Please use the UAV station OR Portable Controller");
                    } else {
                        W_EntityPlayer.addChatMessage(player, "Please use the UAV station");
                    }
                }

                ac = null;
            } else {
                if (!world.isRemote) {
                    ac.getAcDataFromItem(itemStack);
                    world.spawnEntity(ac);
                    MCH_CriteriaTriggers.PUT_AIRCRAFT.trigger((EntityPlayerMP) player);
                }

                if (!player.capabilities.isCreativeMode) {
                    itemStack.shrink(1);
                }
            }
        }

        return ac;
    }

    public void rideEntity(ItemStack item, Entity target, EntityPlayer player) {
        if (!MCH_Config.PlaceableOnSpongeOnly.prmBool && target instanceof EntityMinecartEmpty &&
                target.getPassengers().isEmpty()) {
            BlockPos blockpos = new BlockPos((int) target.posX, (int) target.posY + 2, (int) target.posZ);
            MCH_EntityAircraft ac = this.spawnAircraft(item, player.world, player, blockpos);
            if (!player.world.isRemote && ac != null) {
                ac.startRiding(target);
            }
        }
    }
}
