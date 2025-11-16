package com.norwood.mcheli.particles;

import com.norwood.mcheli.wrapper.W_EntityFX;
import net.minecraft.world.World;

public abstract class MCH_EntityParticleBase extends W_EntityFX {

    public boolean isEffectedWind;
    public boolean diffusible;
    public boolean toWhite;
    public float particleMaxScale;
    public float gravity;
    public float moutionYUpAge;

    public MCH_EntityParticleBase(World par1World, double x, double y, double z, double mx, double my, double mz) {
        super(par1World, x, y, z, mx, my, mz);
        this.motionX = mx;
        this.motionY = my;
        this.motionZ = mz;
        this.isEffectedWind = false;
        this.particleMaxScale = this.particleScale;
    }

    public MCH_EntityParticleBase setParticleScale(float scale) {
        this.particleScale = scale;
        return this;
    }

    public void setParticleMaxAge(int age) {
        this.particleMaxAge = age;
    }

    public void setParticleTextureIndex(int par1) {
        this.particleTextureIndexX = par1 % 8;
        this.particleTextureIndexY = par1 / 8;
    }

    public int getFXLayer() {
        return 2;
    }
}
