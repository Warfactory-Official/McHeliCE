package com.norwood.mcheli.flare;

import com.norwood.mcheli.aircraft.MCH_RenderAircraft;
import com.norwood.mcheli.wrapper.W_Render;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;

public class MCH_RenderChaff extends W_Render<MCH_EntityChaff> {
    public static final IRenderFactory<MCH_EntityChaff> FACTORY = MCH_RenderChaff::new;
    protected MCH_ModelFlare model = new MCH_ModelFlare();

    protected MCH_RenderChaff(RenderManager renderManager) {
        super(renderManager);
    }


    @Override
    public void doRender(MCH_EntityChaff entity, double x, double y, double z, float entityYaw, float partialTicks) {
        GlStateManager.pushMatrix();
        GlStateManager.enableCull();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(-entity.rotationYaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(entity.rotationPitch, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(45.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.scale(0.5F, 0.5F, 0.5F);
        this.bindTexture(new ResourceLocation("mcheli", "textures/chaff.png"));
        this.model.renderModel(0.0D, 0.0D, 0.0625F);
        GlStateManager.popMatrix();
        MCH_RenderAircraft.renderEntityMarker(entity);
    }


}
