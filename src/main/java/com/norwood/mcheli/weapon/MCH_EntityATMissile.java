package com.norwood.mcheli.weapon;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

public class MCH_EntityATMissile extends MCH_EntityBaseBullet {

    public int guidanceType = 0;

    public MCH_EntityATMissile(World par1World) {
        super(par1World);
    }

    public MCH_EntityATMissile(
                               World par1World, double posX, double posY, double posZ, double targetX, double targetY,
                               double targetZ, float yaw, float pitch, double acceleration) {
        super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.getInfo() != null && !this.getInfo().disableSmoke &&
                this.ticksExisted >= this.getInfo().trajectoryParticleStartTick) {
            this.spawnParticle(this.getInfo().trajectoryParticleName, 3, 5.0F * this.getInfo().smokeSize * 0.5F);
        }

        if (!this.world.isRemote) {
            if (this.shootingEntity != null && this.targetEntity != null && !this.targetEntity.isDead) {
                if (this.usingFlareOfTarget(this.targetEntity)) {
                    this.setDead();
                    return;
                }

                this.onUpdateMotion();
            } else {
                this.setDead();
            }
        }

        double a = (float) Math.atan2(this.motionZ, this.motionX);
        this.rotationYaw = (float) (a * 180.0 / Math.PI) - 90.0F;
        double r = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
        this.rotationPitch = -((float) (Math.atan2(this.motionY, r) * 180.0 / Math.PI));
    }

    public void onUpdateMotion() {
        double x = this.targetEntity.posX - this.posX;
        double y = this.targetEntity.posY - this.posY;
        double z = this.targetEntity.posZ - this.posZ;
        double d = x * x + y * y + z * z;
        if (d > 2250000.0 || this.targetEntity.isDead) {
            this.setDead();
        } else if (this.getInfo().proximityFuseDist >= 0.1F && d < this.getInfo().proximityFuseDist) {
            RayTraceResult mop = new RayTraceResult(this.targetEntity);
            mop.entityHit = null;
            this.onImpact(mop, 1.0F);
        } else {
            int rigidityTime = this.getInfo().rigidityTime;
            float af = this.getCountOnUpdate() < rigidityTime + this.getInfo().trajectoryParticleStartTick ? 0.5F :
                    1.0F;
            if (this.getCountOnUpdate() > rigidityTime) {
                if (this.guidanceType == 1) {
                    if (this.getCountOnUpdate() <= rigidityTime + 20) {
                        this.guidanceToTarget(this.targetEntity.posX, this.shootingEntity.posY + 150.0,
                                this.targetEntity.posZ, af);
                    } else if (this.getCountOnUpdate() <= rigidityTime + 30) {
                        this.guidanceToTarget(this.targetEntity.posX, this.shootingEntity.posY, this.targetEntity.posZ,
                                af);
                    } else {
                        if (this.getCountOnUpdate() == rigidityTime + 35) {
                            this.setPower((int) (this.getPower() * 1.2F));
                            if (this.explosionPower > 0) {
                                this.explosionPower++;
                            }
                        }

                        this.guidanceToTarget(this.targetEntity.posX, this.targetEntity.posY, this.targetEntity.posZ,
                                af);
                    }
                } else {
                    d = MathHelper.sqrt(d);
                    this.motionX = x * this.acceleration / d * af;
                    this.motionY = y * this.acceleration / d * af;
                    this.motionZ = z * this.acceleration / d * af;
                }
            }
        }
    }

    @Override
    public MCH_BulletModel getDefaultBulletModel() {
        return MCH_DefaultBulletModels.ATMissile;
    }
}
