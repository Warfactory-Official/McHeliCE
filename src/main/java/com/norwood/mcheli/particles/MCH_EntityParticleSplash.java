package com.norwood.mcheli.particles;

import com.norwood.mcheli.wrapper.W_Block;
import com.norwood.mcheli.wrapper.W_McClient;
import com.norwood.mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

public class MCH_EntityParticleSplash extends MCH_EntityParticleBase {

    public MCH_EntityParticleSplash(World par1World, double x, double y, double z, double mx, double my, double mz) {
        super(par1World, x, y, z, mx, my, mz);
        this.particleRed = this.particleGreen = this.particleBlue = this.rand.nextFloat() * 0.3F + 0.7F;
        this.setParticleScale(this.rand.nextFloat() * 0.5F + 5.0F);
        this.setParticleMaxAge((int) (80.0 / (this.rand.nextFloat() * 0.8 + 0.2)) + 2);
    }

    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        if (this.particleAge < this.particleMaxAge) {
            this.setParticleTextureIndex((int) (8.0 * this.particleAge / this.particleMaxAge));
            this.particleAge++;
        } else {
            this.setExpired();
        }

        this.motionY -= 0.06F;
        Block block = W_WorldFunc.getBlock(this.world, (int) (this.posX + 0.5), (int) (this.posY + 0.5),
                (int) (this.posZ + 0.5));
        boolean beforeInWater = W_Block.isEqualTo(block, W_Block.getWater());
        this.move(this.motionX, this.motionY, this.motionZ);
        block = W_WorldFunc.getBlock(this.world, (int) (this.posX + 0.5), (int) (this.posY + 0.5),
                (int) (this.posZ + 0.5));
        boolean nowInWater = W_Block.isEqualTo(block, W_Block.getWater());
        if (this.motionY < -0.6 && !beforeInWater && nowInWater) {
            double p = -this.motionY * 10.0;

            for (int i = 0; i < p; i++) {
                this.world
                        .spawnParticle(
                                EnumParticleTypes.WATER_SPLASH,
                                this.posX + 0.5 + (this.rand.nextDouble() - 0.5) * 2.0,
                                this.posY + this.rand.nextDouble(),
                                this.posZ + 0.5 + (this.rand.nextDouble() - 0.5) * 2.0,
                                (this.rand.nextDouble() - 0.5) * 2.0,
                                4.0,
                                (this.rand.nextDouble() - 0.5) * 2.0);
                this.world
                        .spawnParticle(
                                EnumParticleTypes.WATER_BUBBLE,
                                this.posX + 0.5 + (this.rand.nextDouble() - 0.5) * 2.0,
                                this.posY - this.rand.nextDouble(),
                                this.posZ + 0.5 + (this.rand.nextDouble() - 0.5) * 2.0,
                                (this.rand.nextDouble() - 0.5) * 2.0,
                                -0.5,
                                (this.rand.nextDouble() - 0.5) * 2.0);
            }
        } else if (this.onGround) {
            this.setExpired();
        }

        this.motionX *= 0.9;
        this.motionZ *= 0.9;
    }

    @Override
    public void renderParticle(
                               BufferBuilder buffer,
                               @NotNull Entity entity,
                               float partialTicks,
                               float rotationX,
                               float rotationZ,
                               float rotationYZ,
                               float rotationXY,
                               float rotationXZ) {
        // Bind smoke particle texture
        W_McClient.MOD_bindTexture("textures/particles/smoke.png");

        // Compute texture coordinates (8x8 atlas)
        float textureUStart = this.particleTextureIndexX / 8.0F;
        float textureUEnd = textureUStart + 0.125F;
        float textureVStart = 0.0F;
        float textureVEnd = 1.0F;

        // Particle scaling
        float scale = 0.1F * this.particleScale;

        // Interpolated position relative to camera
        float renderX = (float) (this.prevPosX + (this.posX - this.prevPosX) * partialTicks - interpPosX);
        float renderY = (float) (this.prevPosY + (this.posY - this.prevPosY) * partialTicks - interpPosY);
        float renderZ = (float) (this.prevPosZ + (this.posZ - this.prevPosZ) * partialTicks - interpPosZ);

        // Light and color setup
        float brightnessFactor = 1.0F;
        int brightness = this.getBrightnessForRender(partialTicks);
        int lightU = (brightness >> 16) & 0xFFFF;
        int lightV = brightness & 0xFFFF;

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);

        buffer.pos(renderX - rotationX * scale - rotationXY * scale,
                renderY - rotationZ * scale,
                renderZ - rotationYZ * scale - rotationXZ * scale)
                .tex(textureUEnd, textureVEnd)
                .color(this.particleRed * brightnessFactor, this.particleGreen * brightnessFactor,
                        this.particleBlue * brightnessFactor, this.particleAlpha)
                .lightmap(lightU, lightV)
                .endVertex();

        buffer.pos(renderX - rotationX * scale + rotationXY * scale,
                renderY + rotationZ * scale,
                renderZ - rotationYZ * scale + rotationXZ * scale)
                .tex(textureUEnd, textureVStart)
                .color(this.particleRed * brightnessFactor, this.particleGreen * brightnessFactor,
                        this.particleBlue * brightnessFactor, this.particleAlpha)
                .lightmap(lightU, lightV)
                .endVertex();

        buffer.pos(renderX + rotationX * scale + rotationXY * scale,
                renderY + rotationZ * scale,
                renderZ + rotationYZ * scale + rotationXZ * scale)
                .tex(textureUStart, textureVStart)
                .color(this.particleRed * brightnessFactor, this.particleGreen * brightnessFactor,
                        this.particleBlue * brightnessFactor, this.particleAlpha)
                .lightmap(lightU, lightV)
                .endVertex();

        buffer.pos(renderX + rotationX * scale - rotationXY * scale,
                renderY - rotationZ * scale,
                renderZ + rotationYZ * scale - rotationXZ * scale)
                .tex(textureUStart, textureVEnd)
                .color(this.particleRed * brightnessFactor, this.particleGreen * brightnessFactor,
                        this.particleBlue * brightnessFactor, this.particleAlpha)
                .lightmap(lightU, lightV)
                .endVertex();
        Tessellator.getInstance().draw();
    }

    @Override
    public int getFXLayer() {
        return 3;
    }
}
