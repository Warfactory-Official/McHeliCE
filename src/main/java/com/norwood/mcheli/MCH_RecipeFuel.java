package com.norwood.mcheli;

import com.norwood.mcheli.aircraft.MCH_ItemFuel;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemCoal;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.registries.IForgeRegistryEntry.Impl;
import org.jetbrains.annotations.NotNull;

public class MCH_RecipeFuel extends Impl<IRecipe> implements IRecipe {

    public boolean matches(InventoryCrafting inv, @NotNull World var2) {
        int jcnt = 0;
        int ccnt = 0;

        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack is = inv.getStackInSlot(i);
            if (!is.isEmpty()) {
                if (is.getItem() instanceof MCH_ItemFuel) {
                    if (is.getMetadata() == 0) {
                        return false;
                    }

                    if (++jcnt > 1) {
                        return false;
                    }
                } else {
                    if (!(is.getItem() instanceof ItemCoal) || is.getCount() <= 0) {
                        return false;
                    }

                    ccnt++;
                }
            }
        }

        return jcnt == 1 && ccnt > 0;
    }

    public @NotNull ItemStack getCraftingResult(InventoryCrafting inv) {
        ItemStack output = new ItemStack(MCH_MOD.itemFuel);

        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack is = inv.getStackInSlot(i);
            if (!is.isEmpty() && is.getItem() instanceof MCH_ItemFuel) {
                output.setItemDamage(is.getMetadata());
                break;
            }
        }

        for (int ix = 0; ix < inv.getSizeInventory(); ix++) {
            ItemStack is = inv.getStackInSlot(ix);
            if (!is.isEmpty() && is.getItem() instanceof ItemCoal) {
                int sp = 100;
                if (is.getMetadata() == 1) {
                    sp = 75;
                }

                if (output.getMetadata() > sp) {
                    output.setItemDamage(output.getMetadata() - sp);
                } else {
                    output.setItemDamage(0);
                }
            }
        }

        return output;
    }

    public boolean canFit(int width, int height) {
        return width >= 3 && height >= 3;
    }

    public @NotNull ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }
}
