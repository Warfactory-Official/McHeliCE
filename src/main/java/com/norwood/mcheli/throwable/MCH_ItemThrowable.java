package com.norwood.mcheli.throwable;

import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.wrapper.W_Item;
import net.minecraft.block.BlockDispenser;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public class MCH_ItemThrowable extends W_Item {

    public MCH_ItemThrowable(int par1) {
        super(par1);
        this.setMaxStackSize(1);
    }

    public static void registerDispenseBehavior(Item item) {
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(item, new MCH_ItemThrowableDispenseBehavior());
    }

    public @NotNull ActionResult<ItemStack> onItemRightClick(@NotNull World world, EntityPlayer player,
                                                             @NotNull EnumHand handIn) {
        ItemStack itemstack = player.getHeldItem(handIn);
        player.setActiveHand(handIn);
        return ActionResult.newResult(EnumActionResult.SUCCESS, itemstack);
    }

    public void onPlayerStoppedUsing(@NotNull ItemStack itemStack, @NotNull World world,
                                     @NotNull EntityLivingBase entityLiving, int par4) {
        if (entityLiving instanceof EntityPlayer player) {
            if (!itemStack.isEmpty() && itemStack.getCount() > 0) {
                MCH_ThrowableInfo info = MCH_ThrowableInfoManager.get(itemStack.getItem());
                if (info != null) {
                    if (!player.capabilities.isCreativeMode) {
                        itemStack.shrink(1);
                        if (itemStack.getCount() <= 0) {
                            player.inventory.setInventorySlotContents(player.inventory.currentItem, ItemStack.EMPTY);
                        }
                    }

                    world.playSound(
                            null,
                            player.posX,
                            player.posY,
                            player.posZ,
                            SoundEvents.ENTITY_ARROW_SHOOT,
                            SoundCategory.PLAYERS,
                            0.5F,
                            0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));
                    if (!world.isRemote) {
                        float acceleration = 1.0F;
                        par4 = itemStack.getMaxItemUseDuration() - par4;
                        if (par4 <= 35) {
                            if (par4 < 5) {
                                par4 = 5;
                            }

                            acceleration = par4 / 25.0F;
                        }

                        MCH_Lib.DbgLog(world, "MCH_ItemThrowable.onPlayerStoppedUsing(%d)", par4);
                        MCH_EntityThrowable entity = new MCH_EntityThrowable(world, player, acceleration);
                        entity.shoot(player, player.rotationPitch, player.rotationYaw, 0.0F, acceleration, 1.0F);
                        entity.setInfo(info);
                        world.spawnEntity(entity);
                    }
                }
            }
        }
    }

    public int getMaxItemUseDuration(@NotNull ItemStack par1ItemStack) {
        return 72000;
    }

    public @NotNull EnumAction getItemUseAction(@NotNull ItemStack par1ItemStack) {
        return EnumAction.BOW;
    }
}
