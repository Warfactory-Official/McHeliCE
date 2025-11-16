package com.norwood.mcheli;

import com.norwood.mcheli.tool.rangefinder.MCH_ItemRangeFinder;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemRedstone;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.registries.IForgeRegistryEntry.Impl;
import org.jetbrains.annotations.NotNull;

public class MCH_RecipeReloadRangeFinder extends Impl<IRecipe> implements IRecipe {

    public boolean matches(InventoryCrafting inv, @NotNull World var2) {
        int jcnt = 0;
        int ccnt = 0;

        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack is = inv.getStackInSlot(i);
            if (!is.isEmpty()) {
                if (is.getItem() instanceof MCH_ItemRangeFinder) {
                    if (is.getMetadata() == 0) {
                        return false;
                    }

                    if (++jcnt > 1) {
                        return false;
                    }
                } else {
                    if (!(is.getItem() instanceof ItemRedstone) || is.getCount() <= 0) {
                        return false;
                    }

                    if (++ccnt > 1) {
                        return false;
                    }
                }
            }
        }

        return jcnt == 1 && ccnt > 0;
    }

    public @NotNull ItemStack getCraftingResult(@NotNull InventoryCrafting inv) {
        return new ItemStack(MCH_MOD.itemRangeFinder);
    }

    public boolean canFit(int width, int height) {
        return width >= 2 && height >= 2;
    }

    public @NotNull ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }
}
