package com.norwood.mcheli.helper.client.renderer.item;

import javax.annotation.Nullable;

import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IItemModelRenderer {

    static boolean isFirstPerson(TransformType type) {
        return type == TransformType.FIRST_PERSON_LEFT_HAND || type == TransformType.FIRST_PERSON_RIGHT_HAND;
    }

    static boolean isThirdPerson(TransformType type) {
        return type == TransformType.THIRD_PERSON_LEFT_HAND || type == TransformType.THIRD_PERSON_RIGHT_HAND;
    }

    boolean shouldRenderer(ItemStack var1, TransformType var2);

    void renderItem(ItemStack var1, @Nullable EntityLivingBase var2, TransformType var3, float var4);
}
