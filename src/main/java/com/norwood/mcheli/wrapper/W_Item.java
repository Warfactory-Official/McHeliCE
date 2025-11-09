package com.norwood.mcheli.wrapper;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import org.jetbrains.annotations.NotNull;

public class W_Item extends Item {

    public W_Item(int par1) {}

    public W_Item() {}

    public static int getIdFromItem(@NotNull Item i) {
        return REGISTRY.getIDForObject(i);
    }

    public static @NotNull Item getItemById(int i) {
        return Item.getItemById(i);
    }

    public static Item getItemByName(String nm) {
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation(nm));
    }

    public static String getNameForItem(Item item) {
        return ForgeRegistries.ITEMS.getKey(item).toString();
    }

    public static @NotNull Item getItemFromBlock(@NotNull Block block) {
        return Item.getItemFromBlock(block);
    }

    public boolean canApplyAtEnchantingTable(ItemStack stack, net.minecraft.enchantment.Enchantment enchantment) {
        return false;
    }
}
