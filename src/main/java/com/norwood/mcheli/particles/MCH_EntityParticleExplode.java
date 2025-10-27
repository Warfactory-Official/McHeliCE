package com.norwood.mcheli.particles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

public class MCH_EntityParticleExplode extends MCH_EntityParticleBase {
    private static final VertexFormat VERTEX_FORMAT = new VertexFormat()
            .addElement(DefaultVertexFormats.POSITION_3F)
            .addElement(DefaultVertexFormats.TEX_2F)
            .addElement(DefaultVertexFormats.COLOR_4UB)
            .addElement(DefaultVertexFormats.TEX_2S)
            .addElement(DefaultVertexFormats.NORMAL_3B)
            .addElement(DefaultVertexFormats.PADDING_1B);
    private static final ResourceLocation texture = new ResourceLocation("textures/entity/explosion.png");
    private int nowCount;
    private final int endCount;
    private final TextureManager theRenderEngine = Minecraft.getMinecraft().renderEngine;
    private final float size;

    public MCH_EntityParticleExplode(World w, double x, double y, double z, double size, double age, double mz) {
        super(w, x, y, z, 0.0, 0.0, 0.0);
        this.endCount = 1 + (int) age;
        this.size = (float) size;
    }

    @Override
    public void renderParticle(
            @NotNull BufferBuilder buffer,
            @NotNull Entity cameraEntity,
            float partialTicks,
            float rotationX,
            float rotationZ,
            float rotationYZ,
            float rotationXY,
            float rotationXZ
    ) {
        int currentFrame = (int) ((this.nowCount + partialTicks) * 15.0F / this.endCount);
        if (currentFrame > 15) {
            return;
        }

        GlStateManager.enableBlend();
        int prevSrcBlend = GlStateManager.glGetInteger(GL11.GL_BLEND_SRC);
        int prevDstBlend = GlStateManager.glGetInteger(GL11.GL_BLEND_DST);
        GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableCull();
        this.theRenderEngine.bindTexture(texture);

        // Texture coordinates for 4x4 explosion sheet
        final int framesPerRow = 4;
        final float frameSize = 1.0F / framesPerRow;

        int frameRow = currentFrame / framesPerRow;
        int frameCol = currentFrame % framesPerRow;

        float uMin = frameCol * frameSize;
        float uMax = uMin + frameSize;
        float vMin = frameRow * frameSize;
        float vMax = vMin + frameSize;

        float scaledSize = 2.0F * this.size;

        // Interpolated position relative to camera
        float relX = (float) (this.prevPosX + (this.posX - this.prevPosX) * partialTicks - interpPosX);
        float relY = (float) (this.prevPosY + (this.posY - this.prevPosY) * partialTicks - interpPosY);
        float relZ = (float) (this.prevPosZ + (this.posZ - this.prevPosZ) * partialTicks - interpPosZ);

        RenderHelper.disableStandardItemLighting();

        int brightness = 15728880;
        int lightU = brightness >> 16 & 0xFFFF;
        int lightV = brightness & 0xFFFF;

        buffer.begin(GL11.GL_QUADS, VERTEX_FORMAT);

        buffer.pos(relX - rotationX * scaledSize - rotationXY * scaledSize,
                        relY - rotationYZ * scaledSize,
                        relZ - rotationZ * scaledSize - rotationXZ * scaledSize)
                .tex(uMax, vMax)
                .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha)
                .lightmap(lightU, lightV)
                .normal(0.0F, 1.0F, 0.0F)
                .endVertex();

        buffer.pos(relX - rotationX * scaledSize + rotationXY * scaledSize,
                        relY + rotationYZ * scaledSize,
                        relZ - rotationZ * scaledSize + rotationXZ * scaledSize)
                .tex(uMax, vMin)
                .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha)
                .lightmap(lightU, lightV)
                .normal(0.0F, 1.0F, 0.0F)
                .endVertex();

        buffer.pos(relX + rotationX * scaledSize + rotationXY * scaledSize,
                        relY + rotationYZ * scaledSize,
                        relZ + rotationZ * scaledSize + rotationXZ * scaledSize)
                .tex(uMin, vMin)
                .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha)
                .lightmap(lightU, lightV)
                .normal(0.0F, 1.0F, 0.0F)
                .endVertex();

        buffer.pos(relX + rotationX * scaledSize - rotationXY * scaledSize,
                        relY - rotationYZ * scaledSize,
                        relZ + rotationZ * scaledSize - rotationXZ * scaledSize)
                .tex(uMin, vMax)
                .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha)
                .lightmap(lightU, lightV)
                .normal(0.0F, 1.0F, 0.0F)
                .endVertex();

        Tessellator.getInstance().draw();

        GlStateManager.enableLighting();
        GlStateManager.enableCull();
        GlStateManager.blendFunc(prevSrcBlend, prevDstBlend);
        GlStateManager.disableBlend();
    }

    public int getBrightnessForRender(float p_70070_1_) {
        return 15728880;
    }

    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        this.nowCount++;
        if (this.nowCount == this.endCount) {
            this.setExpired();
        }
    }

    @Override
    public int getFXLayer() {
        return 3;
    }
}
