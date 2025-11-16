package com.norwood.mcheli.aircraft;

import com.norwood.mcheli.wrapper.W_Item;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemCoal;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MCH_ItemFuel extends W_Item {

    public MCH_ItemFuel(int itemID) {
        super(itemID);
        this.setMaxDamage(600);
        this.setMaxStackSize(1);
        this.setNoRepair();
        this.setFull3D();
    }

    public @NotNull ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player,
                                                             @NotNull EnumHand handIn) {
        ItemStack stack = player.getHeldItem(handIn);
        if (!world.isRemote && stack.isItemDamaged() && !player.capabilities.isCreativeMode) {
            this.refuel(stack, player, 1);
            this.refuel(stack, player, 0);
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        } else {
            return new ActionResult<>(EnumActionResult.PASS, stack);
        }
    }

    private void refuel(ItemStack stack, EntityPlayer player, int coalType) {
        List<ItemStack> list = player.inventory.mainInventory;

        for (int i = 0; i < list.size(); i++) {
            ItemStack is = list.get(i);
            if (!is.isEmpty() && is.getItem() instanceof ItemCoal && is.getMetadata() == coalType) {
                for (int j = 0; is.getCount() > 0 && stack.isItemDamaged() && j < 64; j++) {
                    int damage = stack.getMetadata() - (coalType == 1 ? 75 : 100);
                    if (damage < 0) {
                        damage = 0;
                    }

                    stack.setItemDamage(damage);
                    is.shrink(1);
                }

                if (is.getCount() <= 0) {
                    list.set(i, ItemStack.EMPTY);
                }
            }
        }
    }
}
