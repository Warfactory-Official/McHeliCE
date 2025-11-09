package com.norwood.mcheli.weapon;

import net.minecraft.world.World;

public class MCH_EntityRocket extends MCH_EntityBaseBullet {

    public MCH_EntityRocket(World par1World) {
        super(par1World);
    }

    public MCH_EntityRocket(
                            World par1World, double posX, double posY, double posZ, double targetX, double targetY,
                            double targetZ, float yaw, float pitch, double acceleration) {
        super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        this.onUpdateBomblet();
        if (this.isBomblet <= 0 && this.getInfo() != null && !this.getInfo().disableSmoke) {
            this.spawnParticle(this.getInfo().trajectoryParticleName, 3, 5.0F * this.getInfo().smokeSize * 0.5F);
        }
    }

    @Override
    public void sprinkleBomblet() {
        if (!this.world.isRemote) {
            MCH_EntityRocket e = new MCH_EntityRocket(
                    this.world, this.posX, this.posY, this.posZ, this.motionX, this.motionY, this.motionZ,
                    this.rotationYaw, this.rotationPitch, this.acceleration);
            e.setName(this.getName());
            e.setParameterFromWeapon(this, this.shootingAircraft, this.shootingEntity);
            float MOTION = this.getInfo().bombletDiff;
            e.motionX = e.motionX + (this.rand.nextFloat() - 0.5) * MOTION;
            e.motionY = e.motionY + (this.rand.nextFloat() - 0.5) * MOTION;
            e.motionZ = e.motionZ + (this.rand.nextFloat() - 0.5) * MOTION;
            e.setBomblet();
            this.world.spawnEntity(e);
        }
    }

    @Override
    public MCH_BulletModel getDefaultBulletModel() {
        return MCH_DefaultBulletModels.Rocket;
    }
}
