package com.norwood.mcheli.helper.client.renderer.item;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.norwood.mcheli.MCH_ModelManager;
import com.norwood.mcheli.wrapper.W_McClient;

@SideOnly(Side.CLIENT)
public class BuiltInDraftingTableItemRenderer implements IItemModelRenderer {

    @Override
    public boolean shouldRenderer(ItemStack itemStack, TransformType transformType) {
        return true;
    }

    @Override
    public void renderItem(ItemStack itemStack, EntityLivingBase entityLivingBase, TransformType transformType,
                           float partialTicks) {
        GlStateManager.pushMatrix();
        W_McClient.MOD_bindTexture("textures/blocks/drafting_table.png");
        GlStateManager.enableRescaleNormal();;
        switch (transformType) {
            case GROUND:
                GlStateManager.translate(0.0F, 0.5F, 0.0F);
                GlStateManager.scale(1.5F, 1.5F, 1.5F);
                break;
            case GUI:
            case FIXED:
                GlStateManager.translate(0.0F, -0.5F, 0.0F);
                GlStateManager.scale(0.75F, 0.75F, 0.75F);
                break;
            case THIRD_PERSON_LEFT_HAND:
            case THIRD_PERSON_RIGHT_HAND:
                GlStateManager.translate(0.0F, 0.0F, 0.5F);
                GlStateManager.scale(1.0F, 1.0F, 1.0F);
                break;
            case FIRST_PERSON_LEFT_HAND:
            case FIRST_PERSON_RIGHT_HAND:
                GlStateManager.translate(0.75F, 0.0F, 0.0F);
                GlStateManager.scale(1.0F, 1.0F, 1.0F);
                GlStateManager.rotate(90.0F, 0.0F, -1.0F, 0.0F);
        }

        MCH_ModelManager.render("blocks", "drafting_table");
        GlStateManager.popMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
    }
}
