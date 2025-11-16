package com.norwood.mcheli.weapon;

import com.norwood.mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

public class MCH_EntityASMissile extends MCH_EntityBaseBullet {

    public double targetPosX;
    public double targetPosY;
    public double targetPosZ;

    public MCH_EntityASMissile(World par1World) {
        super(par1World);
        this.targetPosX = 0.0;
        this.targetPosY = 0.0;
        this.targetPosZ = 0.0;
    }

    public MCH_EntityASMissile(
                               World par1World, double posX, double posY, double posZ, double targetX, double targetY,
                               double targetZ, float yaw, float pitch, double acceleration) {
        super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
    }

    @Override
    public float getGravity() {
        return this.getBomblet() == 1 ? -0.03F : super.getGravity();
    }

    @Override
    public float getGravityInWater() {
        return this.getBomblet() == 1 ? -0.03F : super.getGravityInWater();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.getInfo() != null && !this.getInfo().disableSmoke && this.getBomblet() == 0) {
            this.spawnParticle(this.getInfo().trajectoryParticleName, 3, 10.0F * this.getInfo().smokeSize * 0.5F);
        }

        if (this.getInfo() != null && !this.world.isRemote && this.isBomblet != 1) {
            Block block = W_WorldFunc.getBlock(this.world, (int) this.targetPosX, (int) this.targetPosY,
                    (int) this.targetPosZ);
            if (block.isCollidable()) {
                double dist = this.getDistance(this.targetPosX, this.targetPosY, this.targetPosZ);
                if (dist < this.getInfo().proximityFuseDist) {
                    if (this.getInfo().bomblet > 0) {
                        for (int i = 0; i < this.getInfo().bomblet; i++) {
                            this.sprinkleBomblet();
                        }
                    } else {
                        RayTraceResult mop = new RayTraceResult(this);
                        this.onImpact(mop, 1.0F);
                    }

                    this.setDead();
                } else if (this.getGravity() == 0.0) {
                    double up = 0.0;
                    if (this.getCountOnUpdate() < 10) {
                        up = 20.0;
                    }

                    double x = this.targetPosX - this.posX;
                    double y = this.targetPosY + up - this.posY;
                    double z = this.targetPosZ - this.posZ;
                    double d = MathHelper.sqrt(x * x + y * y + z * z);
                    this.motionX = x * this.acceleration / d;
                    this.motionY = y * this.acceleration / d;
                    this.motionZ = z * this.acceleration / d;
                } else {
                    double x = this.targetPosX - this.posX;
                    double y = this.targetPosY - this.posY;
                    y *= 0.3;
                    double z = this.targetPosZ - this.posZ;
                    double d = MathHelper.sqrt(x * x + y * y + z * z);
                    this.motionX = x * this.acceleration / d;
                    this.motionZ = z * this.acceleration / d;
                }
            }
        }

        double a = (float) Math.atan2(this.motionZ, this.motionX);
        this.rotationYaw = (float) (a * 180.0 / Math.PI) - 90.0F;
        double r = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
        this.rotationPitch = -((float) (Math.atan2(this.motionY, r) * 180.0 / Math.PI));
        this.onUpdateBomblet();
    }

    @Override
    public void sprinkleBomblet() {
        if (!this.world.isRemote) {
            MCH_EntityASMissile e = new MCH_EntityASMissile(
                    this.world, this.posX, this.posY, this.posZ, this.motionX, this.motionY, this.motionZ,
                    this.rand.nextInt(360), 0.0F, this.acceleration);
            e.setParameterFromWeapon(this, this.shootingAircraft, this.shootingEntity);
            e.setName(this.getName());
            float RANDOM = this.getInfo().bombletDiff;
            e.motionX = this.motionX * 0.5 + (this.rand.nextFloat() - 0.5F) * RANDOM;
            e.motionY = this.motionY * 0.5 / 2.0 + (this.rand.nextFloat() - 0.5F) * RANDOM / 2.0F;
            e.motionZ = this.motionZ * 0.5 + (this.rand.nextFloat() - 0.5F) * RANDOM;
            e.setBomblet();
            this.world.spawnEntity(e);
        }
    }

    @Override
    public MCH_BulletModel getDefaultBulletModel() {
        return MCH_DefaultBulletModels.ASMissile;
    }
}
