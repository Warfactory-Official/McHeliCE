package com.norwood.mcheli.weapon;

import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

public class MCH_EntityAAMissile extends MCH_EntityBaseBullet {

    public MCH_EntityAAMissile(World par1World) {
        super(par1World);
        this.targetEntity = null;
    }

    public MCH_EntityAAMissile(
                               World par1World, double posX, double posY, double posZ, double targetX, double targetY,
                               double targetZ, float yaw, float pitch, double acceleration) {
        super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.getCountOnUpdate() > 4 && this.getInfo() != null && !this.getInfo().disableSmoke) {
            this.spawnParticle(this.getInfo().trajectoryParticleName, 3, 7.0F * this.getInfo().smokeSize * 0.5F);
        }

        if (!this.world.isRemote && this.getInfo() != null) {
            if (this.shootingEntity != null && this.targetEntity != null && !this.targetEntity.isDead) {
                double x = this.posX - this.targetEntity.posX;
                double y = this.posY - this.targetEntity.posY;
                double z = this.posZ - this.targetEntity.posZ;
                double d = x * x + y * y + z * z;
                if (d > 3422500.0) {
                    this.setDead();
                } else if (this.getCountOnUpdate() > this.getInfo().rigidityTime) {
                    if (this.usingFlareOfTarget(this.targetEntity)) {
                        this.setDead();
                        return;
                    }

                    if (this.getInfo().proximityFuseDist >= 0.1F && d < this.getInfo().proximityFuseDist) {
                        RayTraceResult mop = new RayTraceResult(this.targetEntity);
                        this.posX = (this.targetEntity.posX + this.posX) / 2.0;
                        this.posY = (this.targetEntity.posY + this.posY) / 2.0;
                        this.posZ = (this.targetEntity.posZ + this.posZ) / 2.0;
                        this.onImpact(mop, 1.0F);
                    } else {
                        this.guidanceToTarget(this.targetEntity.posX, this.targetEntity.posY, this.targetEntity.posZ);
                    }
                }
            } else {
                this.setDead();
            }
        }
    }

    @Override
    public MCH_BulletModel getDefaultBulletModel() {
        return MCH_DefaultBulletModels.AAMissile;
    }
}
