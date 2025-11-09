package com.norwood.mcheli;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;

public class MCH_TEST_ModelRenderer extends ModelRenderer {

    public MCH_TEST_ModelRenderer(ModelBase par1ModelBase) {
        super(par1ModelBase);
    }

    public void render(float par1) {
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.2, -0.2, 0.2);
        MCH_ModelManager.render("helicopters", "ah-64");
        GlStateManager.popMatrix();
    }
}
