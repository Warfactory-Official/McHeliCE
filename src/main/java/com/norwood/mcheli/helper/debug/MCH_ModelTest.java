package com.norwood.mcheli.helper.debug;

import net.minecraft.client.model.ModelRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.norwood.mcheli.wrapper.W_ModelBase;

@SideOnly(Side.CLIENT)
public class MCH_ModelTest extends W_ModelBase {

    public final ModelRenderer test = new ModelRenderer(this, 0, 0);

    public MCH_ModelTest() {
        this.test.addBox(-5.0F, -5.0F, -5.0F, 10, 10, 10, 0.0F);
    }

    public void renderModel(double yaw, double pitch, float par7) {
        this.test.render(par7);
    }
}
