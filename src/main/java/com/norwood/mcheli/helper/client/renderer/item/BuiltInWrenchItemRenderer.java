package com.norwood.mcheli.helper.client.renderer.item;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.norwood.mcheli.MCH_ModelManager;
import com.norwood.mcheli.tool.MCH_ItemWrench;
import com.norwood.mcheli.wrapper.W_McClient;

@SideOnly(Side.CLIENT)
public class BuiltInWrenchItemRenderer implements IItemModelRenderer {

    @Override
    public boolean shouldRenderer(ItemStack itemStack, TransformType transformType) {
        return IItemModelRenderer.isFirstPerson(transformType) || IItemModelRenderer.isThirdPerson(transformType);
    }

    @Override
    public void renderItem(ItemStack itemStack, EntityLivingBase entity, TransformType transformType,
                           float partialTicks) {
        GlStateManager.pushMatrix();
        W_McClient.MOD_bindTexture("textures/wrench.png");
        if (IItemModelRenderer.isFirstPerson(transformType) && entity.isHandActive() &&
                entity.getActiveHand() == EnumHand.MAIN_HAND) {
            float f = MCH_ItemWrench.getUseAnimSmooth(itemStack, partialTicks);
            GlStateManager.rotate(65.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(f + 20.0F, 1.0F, 0.0F, 0.0F);
        }

        MCH_ModelManager.render("wrench");
        GlStateManager.popMatrix();
    }
}
