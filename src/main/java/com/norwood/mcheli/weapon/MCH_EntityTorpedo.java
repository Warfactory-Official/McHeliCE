package com.norwood.mcheli.weapon;

import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class MCH_EntityTorpedo extends MCH_EntityBaseBullet {

    public double targetPosX;
    public double targetPosY;
    public double targetPosZ;
    public double accelerationInWater = 2.0;

    public MCH_EntityTorpedo(World par1World) {
        super(par1World);
        this.targetPosX = 0.0;
        this.targetPosY = 0.0;
        this.targetPosZ = 0.0;
    }

    public MCH_EntityTorpedo(
                             World par1World, double posX, double posY, double posZ, double targetX, double targetY,
                             double targetZ, float yaw, float pitch, double acceleration) {
        super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.getInfo() != null && this.getInfo().isGuidedTorpedo) {
            this.onUpdateGuided();
        } else {
            this.onUpdateNoGuided();
        }

        if (this.isInWater() && this.getInfo() != null && !this.getInfo().disableSmoke) {
            this.spawnParticle(this.getInfo().trajectoryParticleName, 3, 5.0F * this.getInfo().smokeSize * 0.5F);
        }
    }

    private void onUpdateNoGuided() {
        if (!this.world.isRemote && this.isInWater()) {
            this.motionY *= 0.8F;
            if (this.acceleration < this.accelerationInWater) {
                this.acceleration += 0.1;
            } else if (this.acceleration > this.accelerationInWater + 0.2F) {
                this.acceleration -= 0.1;
            }

            double x = this.motionX;
            double y = this.motionY;
            double z = this.motionZ;
            double d = MathHelper.sqrt(x * x + y * y + z * z);
            this.motionX = x * this.acceleration / d;
            this.motionY = y * this.acceleration / d;
            this.motionZ = z * this.acceleration / d;
        }

        if (this.isInWater()) {
            double a = (float) Math.atan2(this.motionZ, this.motionX);
            this.rotationYaw = (float) (a * 180.0 / Math.PI) - 90.0F;
        }
    }

    private void onUpdateGuided() {
        if (!this.world.isRemote && this.isInWater()) {
            if (this.acceleration < this.accelerationInWater) {
                this.acceleration += 0.1;
            } else if (this.acceleration > this.accelerationInWater + 0.2F) {
                this.acceleration -= 0.1;
            }

            double x = this.targetPosX - this.posX;
            double y = this.targetPosY - this.posY;
            double z = this.targetPosZ - this.posZ;
            double d = MathHelper.sqrt(x * x + y * y + z * z);
            this.motionX = x * this.acceleration / d;
            this.motionY = y * this.acceleration / d;
            this.motionZ = z * this.acceleration / d;
        }

        if (this.isInWater()) {
            double a = (float) Math.atan2(this.motionZ, this.motionX);
            this.rotationYaw = (float) (a * 180.0 / Math.PI) - 90.0F;
            double r = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
            this.rotationPitch = -((float) (Math.atan2(this.motionY, r) * 180.0 / Math.PI));
        }
    }

    @Override
    public MCH_BulletModel getDefaultBulletModel() {
        return MCH_DefaultBulletModels.Torpedo;
    }
}
