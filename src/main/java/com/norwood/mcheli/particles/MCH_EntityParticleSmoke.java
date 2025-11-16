package com.norwood.mcheli.particles;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.wrapper.W_McClient;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class MCH_EntityParticleSmoke extends MCH_EntityParticleBase {

    private static final VertexFormat VERTEX_FORMAT = new VertexFormat()
            .addElement(DefaultVertexFormats.POSITION_3F)
            .addElement(DefaultVertexFormats.TEX_2F)
            .addElement(DefaultVertexFormats.COLOR_4UB)
            .addElement(DefaultVertexFormats.TEX_2S)
            .addElement(DefaultVertexFormats.NORMAL_3B)
            .addElement(DefaultVertexFormats.PADDING_1B);

    public MCH_EntityParticleSmoke(World par1World, double x, double y, double z, double mx, double my, double mz) {
        super(par1World, x, y, z, mx, my, mz);
        this.particleRed = this.particleGreen = this.particleBlue = this.rand.nextFloat() * 0.3F + 0.7F;
        this.setParticleScale(this.rand.nextFloat() * 0.5F + 5.0F);
        this.setParticleMaxAge((int) (16.0 / (this.rand.nextFloat() * 0.8 + 0.2)) + 2);
    }

    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        if (this.particleAge < this.particleMaxAge) {
            this.setParticleTextureIndex((int) (8.0 * this.particleAge / this.particleMaxAge));
            this.particleAge++;
            if (this.diffusible && this.particleScale < this.particleMaxScale) {
                this.particleScale += 0.8F;
            }

            if (this.toWhite) {
                float mn = this.getMinColor();
                float mx = this.getMaxColor();
                float dist = mx - mn;
                if (dist > 0.2) {
                    this.particleRed = this.particleRed + (mx - this.particleRed) * 0.016F;
                    this.particleGreen = this.particleGreen + (mx - this.particleGreen) * 0.016F;
                    this.particleBlue = this.particleBlue + (mx - this.particleBlue) * 0.016F;
                }
            }

            this.effectWind();
            if ((float) this.particleAge / this.particleMaxAge > this.moutionYUpAge) {
                this.motionY += 0.02;
            } else {
                this.motionY = this.motionY + this.gravity;
            }

            this.move(this.motionX, this.motionY, this.motionZ);
            if (this.diffusible) {
                this.motionX *= 0.96;
                this.motionZ *= 0.96;
                this.motionY *= 0.96;
            } else {
                this.motionX *= 0.9;
                this.motionZ *= 0.9;
            }
        } else {
            this.setExpired();
        }
    }

    public float getMinColor() {
        return this.min(this.min(this.particleBlue, this.particleGreen), this.particleRed);
    }

    public float getMaxColor() {
        return this.max(this.max(this.particleBlue, this.particleGreen), this.particleRed);
    }

    public float min(float a, float b) {
        return Math.min(a, b);
    }

    public float max(float a, float b) {
        return Math.max(a, b);
    }

    public void effectWind() {
        if (this.isEffectedWind) {
            List<MCH_EntityAircraft> list = this.world.getEntitiesWithinAABB(MCH_EntityAircraft.class,
                    this.getCollisionBoundingBox().grow(15.0, 15.0, 15.0));

            for (MCH_EntityAircraft ac : list) {
                if (ac.getThrottle() > 0.1F) {
                    float dist = this.getDistance(ac);
                    double vel = (23.0 - dist) * 0.01F * ac.getThrottle();
                    double mx = ac.posX - this.posX;
                    double mz = ac.posZ - this.posZ;
                    this.motionX -= mx * vel;
                    this.motionZ -= mz * vel;
                }
            }
        }
    }

    @Override
    public int getFXLayer() {
        return 3;
    }

    @SideOnly(Side.CLIENT)
    public int getBrightnessForRender(float p_70070_1_) {
        double y = this.posY;
        this.posY += 3000.0;
        int i = super.getBrightnessForRender(p_70070_1_);
        this.posY = y;
        return i;
    }

    @Override
    public void renderParticle(
                               @NotNull BufferBuilder buffer,
                               @NotNull Entity entity,
                               float partialTicks,
                               float rotationX,
                               float rotationZ,
                               float rotationYZ,
                               float rotationXY,
                               float rotationXZ) {
        W_McClient.MOD_bindTexture("textures/particles/smoke.png");
        GlStateManager.enableBlend();


        GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableLighting();
        GlStateManager.disableCull();

        float texUStart = this.particleTextureIndexX / 8.0F;
        float texUEnd = texUStart + 0.125F;
        float texVStart = 0.0F;
        float texVEnd = 1.0F;

        float scale = 0.1F * this.particleScale;
        float renderX = (float) (this.prevPosX + (this.posX - this.prevPosX) * partialTicks - interpPosX);
        float renderY = (float) (this.prevPosY + (this.posY - this.prevPosY) * partialTicks - interpPosY);
        float renderZ = (float) (this.prevPosZ + (this.posZ - this.prevPosZ) * partialTicks - interpPosZ);

        // Lighting information
        int brightness = this.getBrightnessForRender(partialTicks);
        int lightU = (brightness >> 16) & 0xFFFF;
        int lightV = brightness & 0xFFFF;

        // Start drawing a quad
        buffer.begin(GL11.GL_QUADS, VERTEX_FORMAT);

        buffer.pos(renderX - rotationX * scale - rotationXY * scale,
                renderY - rotationZ * scale,
                renderZ - rotationYZ * scale - rotationXZ * scale)
                .tex(texUEnd, texVEnd)
                .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha)
                .lightmap(lightU, lightV)
                .normal(0.0F, 1.0F, 0.0F)
                .endVertex();

        buffer.pos(renderX - rotationX * scale + rotationXY * scale,
                renderY + rotationZ * scale,
                renderZ - rotationYZ * scale + rotationXZ * scale)
                .tex(texUEnd, texVStart)
                .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha)
                .lightmap(lightU, lightV)
                .normal(0.0F, 1.0F, 0.0F)
                .endVertex();

        buffer.pos(renderX + rotationX * scale + rotationXY * scale,
                renderY + rotationZ * scale,
                renderZ + rotationYZ * scale + rotationXZ * scale)
                .tex(texUStart, texVStart)
                .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha)
                .lightmap(lightU, lightV)
                .normal(0.0F, 1.0F, 0.0F)
                .endVertex();

        buffer.pos(renderX + rotationX * scale - rotationXY * scale,
                renderY - rotationZ * scale,
                renderZ + rotationYZ * scale - rotationXZ * scale)
                .tex(texUStart, texVEnd)
                .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha)
                .lightmap(lightU, lightV)
                .normal(0.0F, 1.0F, 0.0F)
                .endVertex();

        Tessellator.getInstance().draw();

        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
    }

    private float getDistance(MCH_EntityAircraft entity) {
        float f = (float) (this.posX - entity.posX);
        float f1 = (float) (this.posY - entity.posY);
        float f2 = (float) (this.posZ - entity.posZ);
        return MathHelper.sqrt(f * f + f1 * f1 + f2 * f2);
    }
}
