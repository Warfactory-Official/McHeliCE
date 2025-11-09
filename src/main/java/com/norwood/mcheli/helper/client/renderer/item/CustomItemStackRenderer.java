package com.norwood.mcheli.helper.client.renderer.item;

import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.model.animation.Animation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.norwood.mcheli.helper.client.MCH_ItemModelRenderers;
import com.norwood.mcheli.helper.client.model.PooledModelParameters;

@SideOnly(Side.CLIENT)
public class CustomItemStackRenderer extends TileEntityItemStackRenderer {

    private static CustomItemStackRenderer instance;

    public static CustomItemStackRenderer getInstance() {
        if (instance == null) {
            instance = new CustomItemStackRenderer();
        }

        return instance;
    }

    public void renderByItem(ItemStack p_192838_1_, float partialTicks) {
        IItemModelRenderer renderer = MCH_ItemModelRenderers.getRenderer(p_192838_1_.getItem());
        if (renderer != null) {
            renderer.renderItem(p_192838_1_, PooledModelParameters.getEntity(),
                    PooledModelParameters.getTransformType(), Animation.getPartialTickTime());
        }
    }
}
