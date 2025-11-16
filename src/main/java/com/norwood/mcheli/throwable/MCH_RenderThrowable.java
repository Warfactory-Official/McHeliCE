package com.norwood.mcheli.throwable;

import com.norwood.mcheli.wrapper.W_Render;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

@SideOnly(Side.CLIENT)
public class MCH_RenderThrowable extends W_Render<MCH_EntityThrowable> {

    public static final IRenderFactory<MCH_EntityThrowable> FACTORY = MCH_RenderThrowable::new;

    public MCH_RenderThrowable(RenderManager renderManager) {
        super(renderManager);
        this.shadowSize = 0.0F;
    }

    public void doRender(MCH_EntityThrowable entity, double posX, double posY, double posZ, float par8,
                         float tickTime) {
        MCH_ThrowableInfo info = entity.getInfo();
        if (info != null) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(posX, posY, posZ);
            GlStateManager.rotate(entity.rotationYaw, 0.0F, -1.0F, 0.0F);
            GlStateManager.rotate(entity.rotationPitch, 1.0F, 0.0F, 0.0F);
            this.setCommonRenderParam(true, entity.getBrightnessForRender());
            if (info.model != null) {
                this.bindTexture("textures/throwable/" + info.name + ".png");
                info.model.renderAll();
            }

            this.restoreCommonRenderParam();
            GlStateManager.popMatrix();
        }
    }

    protected ResourceLocation getEntityTexture(@NotNull MCH_EntityThrowable entity) {
        return TEX_DEFAULT;
    }
}
