package com.norwood.mcheli.wrapper;

import java.nio.FloatBuffer;

import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_MOD;

public abstract class W_Render<T extends Entity> extends Render<T> {

    protected static final ResourceLocation TEX_DEFAULT = new ResourceLocation(MCH_MOD.DOMAIN, "textures/default.png");
    private static final FloatBuffer colorBuffer = GLAllocation.createDirectFloatBuffer(16);

    protected W_Render(RenderManager renderManager) {
        super(renderManager);
    }

    public static FloatBuffer setColorBuffer(float p_74521_0_, float p_74521_1_, float p_74521_2_, float p_74521_3_) {
        colorBuffer.clear();
        colorBuffer.put(p_74521_0_).put(p_74521_1_).put(p_74521_2_).put(p_74521_3_);
        colorBuffer.flip();
        return colorBuffer;
    }

    protected void bindTexture(String path) {
        super.bindTexture(new ResourceLocation(MCH_MOD.DOMAIN, path));
    }

    protected ResourceLocation getEntityTexture(@NotNull T entity) {
        return TEX_DEFAULT;
    }

    public void setCommonRenderParam(boolean smoothShading, int lighting) {
        if (smoothShading && MCH_Config.SmoothShading.prmBool) {
            GlStateManager.shadeModel(GL11.GL_SMOOTH); // 7425
        }

        GlStateManager.enableLighting();
        GlStateManager.enableLight(0);
        GlStateManager.enableLight(1);
        GlStateManager.enableColorMaterial();
        GlStateManager.colorMaterial(1032, 5634);
        GlStateManager.enableCull(); // 2884
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.001F);

        int j = lighting % 65536;
        int k = lighting / 65536;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, j, k);

        GlStateManager.color(0.75F, 0.75F, 0.75F, 1.0F);

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    public void restoreCommonRenderParam() {
        GlStateManager.disableBlend();
        GlStateManager.shadeModel(GL11.GL_FLAT);
    }
}
