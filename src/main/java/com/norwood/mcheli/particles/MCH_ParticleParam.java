package com.norwood.mcheli.particles;

import net.minecraft.world.World;

public class MCH_ParticleParam {

    public final World world;
    public final String name;
    public double posX;
    public double posY;
    public double posZ;
    public double motionX = 0.0;
    public double motionY = 0.0;
    public double motionZ = 0.0;
    public float size = 1.0F;
    public float a = 1.0F;
    public float r = 1.0F;
    public float g = 1.0F;
    public float b = 1.0F;
    public boolean isEffectWind = false;
    public int age = 0;
    public boolean diffusible = false;
    public boolean toWhite = false;
    public float gravity = 0.0F;
    public float motionYUpAge = 2.0F;

    public MCH_ParticleParam(World w, String name, double x, double y, double z) {
        this.world = w;
        this.name = name;
        this.posX = x;
        this.posY = y;
        this.posZ = z;
    }

    public MCH_ParticleParam(World w, String name, double x, double y, double z, double mx, double my, double mz,
                             float size) {
        this(w, name, x, y, z);
        this.motionX = mx;
        this.motionY = my;
        this.motionZ = mz;
        this.size = size;
    }

    public void setColor(float a, float r, float g, float b) {
        this.a = a;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public void setMotion(double x, double y, double z) {
        this.motionX = x;
        this.motionY = y;
        this.motionZ = z;
    }
}
