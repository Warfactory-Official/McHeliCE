package com.norwood.mcheli.helper.client.renderer.item;

import com.norwood.mcheli.MCH_ModelManager;
import com.norwood.mcheli.tool.rangefinder.MCH_ItemRangeFinder;
import com.norwood.mcheli.wrapper.W_McClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class BuiltInRangeFinderItemRenderer implements IItemModelRenderer {

    @Override
    public boolean shouldRenderer(ItemStack itemStack, TransformType transformType) {
        return IItemModelRenderer.isFirstPerson(transformType) || IItemModelRenderer.isThirdPerson(transformType) ||
                transformType == TransformType.GROUND;
    }

    @Override
    public void renderItem(ItemStack itemStack, EntityLivingBase entity, TransformType transformType,
                           float partialTicks) {
        GlStateManager.pushMatrix();
        W_McClient.MOD_bindTexture("textures/rangefinder.png");
        boolean flag = true;
        if (IItemModelRenderer.isFirstPerson(transformType)) {
            flag = entity instanceof EntityPlayer && !MCH_ItemRangeFinder.isUsingScope((EntityPlayer) entity);
            if (entity.isHandActive() && entity.getActiveHand() == EnumHand.MAIN_HAND) {
                GlStateManager.translate(0.6563F, 0.3438F, 0.01F);
            }
        }

        if (flag) {
            MCH_ModelManager.render("rangefinder");
        }

        GlStateManager.popMatrix();
    }
}
