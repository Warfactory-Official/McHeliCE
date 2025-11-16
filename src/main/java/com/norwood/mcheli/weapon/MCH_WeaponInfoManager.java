package com.norwood.mcheli.weapon;

import com.norwood.mcheli.helper.info.ContentRegistries;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;

public class MCH_WeaponInfoManager {

    private MCH_WeaponInfoManager() {}

    public static void setRoundItems() {
        for (MCH_WeaponInfo mchWeaponInfo : ContentRegistries.weapon().values()) {
            for (MCH_WeaponInfo.RoundItem roundItem : mchWeaponInfo.roundItems) {
                Item item = ForgeRegistries.ITEMS.getValue(roundItem.itemName);
                if (item == null) continue;
                roundItem.itemStack = new ItemStack(item, 1, roundItem.damage);
            }
        }
    }

    @Nullable
    public static MCH_WeaponInfo get(String name) {
        return ContentRegistries.weapon().get(name);
    }

    public static boolean contains(String name) {
        return ContentRegistries.weapon().contains(name);
    }
}
