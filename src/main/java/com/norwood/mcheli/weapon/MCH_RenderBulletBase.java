package com.norwood.mcheli.weapon;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

import com.norwood.mcheli.MCH_Color;
import com.norwood.mcheli.wrapper.W_Block;
import com.norwood.mcheli.wrapper.W_Entity;
import com.norwood.mcheli.wrapper.W_Render;
import com.norwood.mcheli.wrapper.W_WorldFunc;

public abstract class MCH_RenderBulletBase<T extends W_Entity> extends W_Render<T> {

    protected MCH_RenderBulletBase(RenderManager renderManager) {
        super(renderManager);
    }

    public void doRender(@NotNull T e, double var2, double var4, double var6, float var8, float var9) {
        if (e instanceof MCH_EntityBaseBullet && ((MCH_EntityBaseBullet) e).getInfo() != null) {
            MCH_Color c = ((MCH_EntityBaseBullet) e).getInfo().color;

            for (int y = 0; y < 3; y++) {
                Block b = W_WorldFunc.getBlock(e.world, (int) (e.posX + 0.5), (int) (e.posY + 1.5 - y),
                        (int) (e.posZ + 0.5));
                if (b == W_Block.getWater()) {
                    c = ((MCH_EntityBaseBullet) e).getInfo().colorInWater;
                    break;
                }
            }

            GlStateManager.color(c.r, c.g, c.b, c.a);
        } else {
            GlStateManager.color(0.75F, 0.75F, 0.75F, 1.0F);
        }

        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.001F);
        GlStateManager.enableCull();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        this.renderBullet(e, var2, var4, var6, var8, var9);
        GlStateManager.color(0.75F, 0.75F, 0.75F, 1.0F);
        GlStateManager.disableBlend();
    }

    public void renderModel(MCH_EntityBaseBullet e) {
        MCH_BulletModel model = e.getBulletModel();
        if (model != null) {
            this.bindTexture("textures/bullets/" + model.name + ".png");
            model.model.renderAll();
        }
    }

    public abstract void renderBullet(T var1, double var2, double var4, double var6, float var8, float var9);
}
