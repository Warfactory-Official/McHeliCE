package com.norwood.mcheli.wrapper;

import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;

public class W_ItemArmor extends ItemArmor {

    public W_ItemArmor(int par1, int par3, int par4) {
        super(ArmorMaterial.LEATHER, par3, EntityEquipmentSlot.values()[par4]);
    }
}
