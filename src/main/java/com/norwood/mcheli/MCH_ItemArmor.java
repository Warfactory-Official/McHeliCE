package com.norwood.mcheli;

import com.norwood.mcheli.wrapper.W_ItemArmor;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class MCH_ItemArmor extends W_ItemArmor {

    public static final String HELMET_TEXTURE = "mcheli:textures/helicopters/ah-64.png";
    public static final String CHESTPLATE_TEXTURE = "mcheli:textures/armor/plate.png";
    public static final String LEGGINGS_TEXTURE = "mcheli:textures/armor/leg.png";
    public static final String BOOTS_TEXTURE = "mcheli:textures/armor/boots.png";
    public static MCH_TEST_ModelBiped model = null;

    public MCH_ItemArmor(int par1, int par3, int par4) {
        super(par1, par3, par4);
    }

    public String getArmorTexture(@NotNull ItemStack stack, @NotNull Entity entity, @NotNull EntityEquipmentSlot slot,
                                  @NotNull String type) {
        if (slot == EntityEquipmentSlot.HEAD) {
            return "mcheli:textures/helicopters/ah-64.png";
        } else if (slot == EntityEquipmentSlot.CHEST) {
            return "mcheli:textures/armor/plate.png";
        } else if (slot == EntityEquipmentSlot.LEGS) {
            return "mcheli:textures/armor/leg.png";
        } else {
            return slot == EntityEquipmentSlot.FEET ? "mcheli:textures/armor/boots.png" : "none";
        }
    }

    @Nullable
    @SideOnly(Side.CLIENT)
    public ModelBiped getArmorModel(@NotNull EntityLivingBase entityLiving, @NotNull ItemStack itemStack,
                                    @NotNull EntityEquipmentSlot armorSlot, @NotNull ModelBiped _default) {
        if (model == null) {
            model = new MCH_TEST_ModelBiped();
        }

        return armorSlot == EntityEquipmentSlot.HEAD ? model : null;
    }
}
