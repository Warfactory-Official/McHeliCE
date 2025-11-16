package com.norwood.mcheli.particles;

import com.norwood.mcheli.Tags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

public class MCH_EntityParticleExplode extends MCH_EntityParticleBase {

    public static final ResourceLocation FLASH = new ResourceLocation(Tags.MODID, "textures/flash.png");
    private int nowCount;
    private final int endCount;
    private final TextureManager theRenderEngine = Minecraft.getMinecraft().renderEngine;
    private final float size;

    public MCH_EntityParticleExplode(World w, double x, double y, double z, float size, double age, double mz) {
        super(w, x, y, z, 0.0, 0.0, 0.0);
        this.endCount = (1 + (int) age) % 7;
        this.particleAngle = -45F + rand.nextFloat() * 90F;
        this.size = size + rand.nextFloat() * 0.2f;
        this.prevParticleAngle = particleAngle;
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
                               float rotationXZ) {
        int currentFrame = (int) ((this.nowCount + partialTicks) * 7.0F / this.endCount);
        if (currentFrame > 7) {
            return;
        }

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableCull();
        this.theRenderEngine.bindTexture(FLASH);

        final int framesPerRow = 4;
        final int framesPerColumn = 2;

        final float frameWidth = 1.0F / framesPerRow;
        final float frameHeight = 1.0F / framesPerColumn;

        int frameRow = currentFrame / framesPerRow;
        int frameCol = currentFrame % framesPerRow;

        float uMin = frameCol * frameWidth;
        float uMax = uMin + frameWidth;
        float vMin = frameRow * frameHeight;
        float vMax = vMin + frameHeight;

        float scaledSize = this.size * 1.2f;

        float relX = (float) (this.prevPosX + (this.posX - this.prevPosX) * partialTicks - interpPosX);
        float relY = (float) (this.prevPosY + (this.posY - this.prevPosY) * partialTicks - interpPosY);
        float relZ = (float) (this.prevPosZ + (this.posZ - this.prevPosZ) * partialTicks - interpPosZ);

        RenderHelper.disableStandardItemLighting();

        int i = this.getBrightnessForRender(partialTicks);
        int j = i >> 16 & 65535;
        int k = i & 65535;
        Vec3d[] avec3d = new Vec3d[] {
                new Vec3d(-rotationX * scaledSize - rotationXY * scaledSize, -rotationZ * scaledSize,
                        -rotationYZ * scaledSize - rotationXZ * scaledSize),
                new Vec3d(-rotationX * scaledSize + rotationXY * scaledSize, rotationZ * scaledSize,
                        -rotationYZ * scaledSize + rotationXZ * scaledSize),
                new Vec3d(rotationX * scaledSize + rotationXY * scaledSize, rotationZ * scaledSize,
                        rotationYZ * scaledSize + rotationXZ * scaledSize),
                new Vec3d(rotationX * scaledSize - rotationXY * scaledSize, -rotationZ * scaledSize,
                        rotationYZ * scaledSize - rotationXZ * scaledSize) };

        if (this.particleAngle != 0.0F) {
            float f8 = this.particleAngle + (this.particleAngle - this.prevParticleAngle) * partialTicks;
            float f9 = MathHelper.cos(f8 * 0.5F);
            float f10 = MathHelper.sin(f8 * 0.5F) * (float) cameraViewDir.x;
            float f11 = MathHelper.sin(f8 * 0.5F) * (float) cameraViewDir.y;
            float f12 = MathHelper.sin(f8 * 0.5F) * (float) cameraViewDir.z;
            Vec3d vec3d = new Vec3d(f10, f11, f12);

            for (int l = 0; l < 4; ++l) {
                avec3d[l] = vec3d.scale(2.0D * avec3d[l].dotProduct(vec3d))
                        .add(avec3d[l].scale((double) (f9 * f9) - vec3d.dotProduct(vec3d)))
                        .add(vec3d.crossProduct(avec3d[l]).scale(2.0F * f9));
            }
        }

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
        buffer.pos((double) relX + avec3d[0].x, (double) relY + avec3d[0].y, (double) relZ + avec3d[0].z)
                .tex(uMax, vMax).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha)
                .lightmap(j, k).endVertex();
        buffer.pos((double) relX + avec3d[1].x, (double) relY + avec3d[1].y, (double) relZ + avec3d[1].z)
                .tex(uMax, vMin).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha)
                .lightmap(j, k).endVertex();
        buffer.pos((double) relX + avec3d[2].x, (double) relY + avec3d[2].y, (double) relZ + avec3d[2].z)
                .tex(uMin, vMin).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha)
                .lightmap(j, k).endVertex();
        buffer.pos((double) relX + avec3d[3].x, (double) relY + avec3d[3].y, (double) relZ + avec3d[3].z)
                .tex(uMin, vMax).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha)
                .lightmap(j, k).endVertex();

        Tessellator.getInstance().draw();

        GlStateManager.enableLighting();
        GlStateManager.enableCull();
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
