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

    public void onUpdate() {
        super.onUpdate();

        final var info = this.getInfo();
        if (info == null) return;

        if (this.getCountOnUpdate() > 4 && !info.disableSmoke) {
            this.spawnParticle(info.trajectoryParticleName, 3, 7.0F * info.smokeSize * 0.5F);
        }

        if (world.isRemote) return;

        if (shootingEntity != null && targetEntity != null && !targetEntity.isDead) {
            processGuidance(info);
        } else if (info.activeRadar && ticksExisted % info.scanInterval == 0) {
            scanForTargets();
        }
    }

    private void processGuidance(MCH_WeaponInfo info) {
        double distSq = this.getDistanceSq(targetEntity);

        if (distSq > 3422500.0D) {
            this.setDead();
            return;
        }

        if (this.getCountOnUpdate() <= info.rigidityTime) return;

        if (this.guidanceType == 1) {
            handleTopAttack(info);
        } else {
            handleDirectGuidance(info, distSq);
        }
    }

    private void handleTopAttack(MCH_WeaponInfo info) {
        int age = this.getCountOnUpdate();
        int startTime = info.rigidityTime;
        float acceleration = age < startTime + info.trajectoryParticleStartTick ? 0.5F : 1.0F;

        if (age <= startTime + 20) {
            doingTopAttack = true;
            this.guidanceToTarget(targetEntity.posX, shootingEntity.posY + 100.0D, targetEntity.posZ, acceleration);
        } else if (age <= startTime + 30) {
            this.guidanceToTarget(targetEntity.posX, shootingEntity.posY, targetEntity.posZ, acceleration);
        } else {
            if (age == startTime + 35) {
                this.setPower((int) (this.getPower() * 1.2F));
                if (this.explosionPower > 0) this.explosionPower++;
            }
            doingTopAttack = false;
            this.guidanceToTarget(targetEntity.posX, targetEntity.posY, targetEntity.posZ, acceleration);
        }
    }

    private void handleDirectGuidance(MCH_WeaponInfo info, double distSq) {
        if (info.proximityFuseDist >= 0.1F && distSq < Math.pow(info.proximityFuseDist, 2)) {
            this.posX = (targetEntity.posX + this.posX) / 2.0D;
            this.posY = (targetEntity.posY + this.posY) / 2.0D;
            this.posZ = (targetEntity.posZ + this.posZ) / 2.0D;

            this.onImpact(new RayTraceResult(targetEntity), 1.0F);
        } else {
            this.guidanceToTarget(targetEntity.posX, targetEntity.posY, targetEntity.posZ);
        }
    }

    public MCH_BulletModel getDefaultBulletModel() {
        return MCH_DefaultBulletModels.ATMissile;
    }
}
