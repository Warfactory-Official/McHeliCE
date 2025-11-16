package com.norwood.mcheli.weapon;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class MCH_EntityTvMissile extends MCH_EntityBaseBullet {

    public boolean isSpawnParticle = true;

    public MCH_EntityTvMissile(World par1World) {
        super(par1World);
    }

    public MCH_EntityTvMissile(
                               World par1World, double posX, double posY, double posZ, double targetX, double targetY,
                               double targetZ, float yaw, float pitch, double acceleration) {
        super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.isSpawnParticle && this.getInfo() != null && !this.getInfo().disableSmoke) {
            this.spawnParticle(this.getInfo().trajectoryParticleName, 3, 5.0F * this.getInfo().smokeSize * 0.5F);
        }

        if (this.shootingEntity != null) {
            double x = this.posX - this.shootingEntity.posX;
            double y = this.posY - this.shootingEntity.posY;
            double z = this.posZ - this.shootingEntity.posZ;
            if (x * x + y * y + z * z > 1440000.0) {
                this.setDead();
            }

            if (!this.world.isRemote && !this.isDead) {
                this.onUpdateMotion();
            }
        } else if (!this.world.isRemote) {
            this.setDead();
        }
    }

    public void onUpdateMotion() {
        Entity e = this.shootingEntity;
        if (e != null && !e.isDead) {
            MCH_EntityAircraft ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(e);
            if (ac != null && ac.getTVMissile() == this) {
                float yaw = e.rotationYaw;
                float pitch = e.rotationPitch;
                double tX = -MathHelper.sin(yaw / 180.0F * (float) Math.PI) *
                        MathHelper.cos(pitch / 180.0F * (float) Math.PI);
                double tZ = MathHelper.cos(yaw / 180.0F * (float) Math.PI) *
                        MathHelper.cos(pitch / 180.0F * (float) Math.PI);
                double tY = -MathHelper.sin(pitch / 180.0F * (float) Math.PI);
                this.setMotion(tX, tY, tZ);
                this.setRotation(yaw, pitch);
            }
        }
    }

    @Override
    public MCH_BulletModel getDefaultBulletModel() {
        return MCH_DefaultBulletModels.ATMissile;
    }
}
