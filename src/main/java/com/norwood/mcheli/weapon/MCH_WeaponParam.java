package com.norwood.mcheli.weapon;

import net.minecraft.entity.Entity;

public class MCH_WeaponParam {

    public Entity entity = null;
    public Entity user = null;
    public double posX = 0.0;
    public double posY = 0.0;
    public double posZ = 0.0;
    public float rotYaw = 0.0F;
    public float rotPitch = 0.0F;
    public final float rotRoll = 0.0F;
    public int option1 = 0;
    public int option2 = 0;
    public boolean isInfinity = false;
    public boolean isTurret = false;
    public boolean result;
    public boolean reload;

    public void setPosAndRot(double x, double y, double z, float yaw, float pitch) {
        this.setPosition(x, y, z);
        this.setRotation(yaw, pitch);
    }

    public void setPosition(double x, double y, double z) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
    }

    public void setRotation(float y, float p) {
        this.rotYaw = y;
        this.rotPitch = p;
    }
}
