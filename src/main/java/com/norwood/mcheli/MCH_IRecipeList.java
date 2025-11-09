package com.norwood.mcheli;

import net.minecraft.item.crafting.IRecipe;

public interface MCH_IRecipeList {

    int getRecipeListSize();

    IRecipe getRecipe(int var1);
}
