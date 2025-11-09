package com.norwood.mcheli.helper.client.renderer.item;

import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class BuiltInInvisibleItemRenderer implements IItemModelRenderer {

    @Override
    public boolean shouldRenderer(ItemStack itemStack, TransformType transformType) {
        return IItemModelRenderer.isFirstPerson(transformType) || IItemModelRenderer.isThirdPerson(transformType);
    }

    @Override
    public void renderItem(ItemStack itemStack, EntityLivingBase entityLivingBase, TransformType transformType,
                           float partialTicks) {}
}
