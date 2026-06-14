package com.norwood.mcheli.weapon;

import com.norwood.mcheli.MCH_Color;
import com.norwood.mcheli.Tags;
import com.norwood.mcheli.wrapper.W_Entity;
import com.norwood.mcheli.wrapper.W_Render;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

public abstract class MCH_RenderBulletBase<T extends W_Entity> extends W_Render<T> {

    public static final ResourceLocation TEX_FLAME = new ResourceLocation(Tags.MODID, "textures/exhaust_flame.png");
    public static final ResourceLocation TEX_FLAME_1 = new ResourceLocation(Tags.MODID, "textures/exhaust_flame_1.png");

    protected MCH_RenderBulletBase(RenderManager renderManager) {
        super(renderManager);
    }

    protected void renderMissileFlame(MCH_EntityBaseBullet entity, double x, double y, double z, float partialTicks, double tailOffset, float flameSize) {
        double mx = entity.prevMotionX + (entity.motionX - entity.prevMotionX) * partialTicks;
        double my = entity.prevMotionY + (entity.motionY - entity.prevMotionY) * partialTicks;
        double mz = entity.prevMotionZ + (entity.motionZ - entity.prevMotionZ) * partialTicks;
        double motionLen = Math.sqrt(mx * mx + my * my + mz * mz);

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);

        if (motionLen > 0.0001D) {
            GlStateManager.translate((-mx / motionLen) * tailOffset, (-my / motionLen) * tailOffset, (-mz / motionLen) * tailOffset);
        }

        float viewYaw = this.renderManager.playerViewY;
        float viewPitch = this.renderManager.playerViewX;
        GlStateManager.rotate(180.0F - viewYaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-viewPitch, 1.0F, 0.0F, 0.0F);

        ResourceLocation texture = (entity.ticksExisted / 5) % 2 == 0 ? TEX_FLAME : TEX_FLAME_1;
        this.bindTexture(texture);

        GlStateManager.alphaFunc(516, 0.001F); // GL_GREATER
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.depthMask(false);
        GlStateManager.disableLighting();

        float lastBrightnessX = OpenGlHelper.lastBrightnessX;
        float lastBrightnessY = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(7, DefaultVertexFormats.POSITION_TEX_NORMAL); // 7 = GL_QUADS
        buffer.pos(-flameSize, -flameSize, 0.0D).tex(0.0D, 1.0D).normal(0.0F, 1.0F, 0.0F).endVertex();
        buffer.pos(flameSize, -flameSize, 0.0D).tex(1.0D, 1.0D).normal(0.0F, 1.0F, 0.0F).endVertex();
        buffer.pos(flameSize, flameSize, 0.0D).tex(1.0D, 0.0D).normal(0.0F, 1.0F, 0.0F).endVertex();
        buffer.pos(-flameSize, flameSize, 0.0D).tex(0.0D, 0.0D).normal(0.0F, 1.0F, 0.0F).endVertex();
        tessellator.draw();

        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastBrightnessX, lastBrightnessY);
        GlStateManager.enableLighting();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    @Override
    public void doRender(@NotNull T entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (entity instanceof MCH_EntityBaseBullet bullet) {
            final var info = bullet.getInfo();
            if (info != null) {
                MCH_Color color = info.color;

                BlockPos basePos = new BlockPos(entity.posX, entity.posY, entity.posZ);
                for (int offset = 0; offset < 3; offset++) {
                    if (entity.getEntityWorld().getBlockState(basePos.up(1 - offset)).getMaterial() == Material.WATER) {
                        color = info.colorInWater;
                        break;
                    }
                }
                GlStateManager.color(color.r, color.g, color.b, color.a);
            }
        } else {
            GlStateManager.color(0.75F, 0.75F, 0.75F, 1.0F);
        }

        GlStateManager.alphaFunc(516, 0.001F);
        GlStateManager.enableCull();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );

        this.renderBullet(entity, x, y, z, entityYaw, partialTicks);

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
    }

    public void renderModel(MCH_EntityBaseBullet bullet) {
        MCH_BulletModel model = bullet.getBulletModel();
        if (model != null && model.model != null) {
            this.bindTexture("textures/bullets/" + model.name + ".png");
            model.model.renderAll();
        }
    }

    public abstract void renderBullet(T entity, double x, double y, double z, float yaw, float partialTicks);

    @Override
    public void doRenderShadowAndFire(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        super.doRenderShadowAndFire(entity, x, y, z, yaw, partialTicks);
    }
}
