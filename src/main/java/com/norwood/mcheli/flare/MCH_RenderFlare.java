package com.norwood.mcheli.flare;

import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.jetbrains.annotations.NotNull;

import com.norwood.mcheli.wrapper.W_Render;

@SideOnly(Side.CLIENT)
public class MCH_RenderFlare extends W_Render<MCH_EntityFlare> {

    public static final IRenderFactory<MCH_EntityFlare> FACTORY = MCH_RenderFlare::new;
    protected final MCH_ModelFlare model = new MCH_ModelFlare();

    public MCH_RenderFlare(RenderManager renderManager) {
        super(renderManager);
    }

    public void doRender(MCH_EntityFlare entity, double posX, double posY, double posZ, float yaw,
                         float partialTickTime) {
        GlStateManager.pushMatrix();
        GlStateManager.enableCull();
        GlStateManager.translate(posX, posY, posZ);
        GlStateManager.rotate(-entity.rotationYaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(entity.rotationPitch, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(45.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.color(1.0F, 1.0F, 0.5F, 1.0F);
        this.bindTexture("textures/flare.png");
        this.model.renderModel(0.0, 0.0, 0.0625F);
        GlStateManager.popMatrix();
    }

    protected ResourceLocation getEntityTexture(@NotNull MCH_EntityFlare entity) {
        return TEX_DEFAULT;
    }
}
