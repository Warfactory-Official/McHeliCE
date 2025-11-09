package com.norwood.mcheli.weapon;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import com.norwood.mcheli.wrapper.W_Lib;

public class MCH_EntityBomb extends MCH_EntityBaseBullet {

    public MCH_EntityBomb(World par1World) {
        super(par1World);
    }

    public MCH_EntityBomb(
                          World par1World, double posX, double posY, double posZ, double targetX, double targetY,
                          double targetZ, float yaw, float pitch, double acceleration) {
        super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!this.world.isRemote && this.getInfo() != null) {
            this.motionX *= 0.999;
            this.motionZ *= 0.999;
            if (this.isInWater()) {
                this.motionX = this.motionX * this.getInfo().velocityInWater;
                this.motionY = this.motionY * this.getInfo().velocityInWater;
                this.motionZ = this.motionZ * this.getInfo().velocityInWater;
            }

            float dist = this.getInfo().proximityFuseDist;
            if (dist > 0.1F && this.getCountOnUpdate() % 10 == 0) {
                List<Entity> list = this.world.getEntitiesWithinAABBExcludingEntity(this,
                        this.getEntityBoundingBox().grow(dist, dist, dist));
                for (Entity entity : list) {
                    if (W_Lib.isEntityLivingBase(entity) && this.canBeCollidedEntity(entity)) {
                        RayTraceResult m = new RayTraceResult(
                                new Vec3d(this.posX, this.posY, this.posZ), EnumFacing.DOWN,
                                new BlockPos(this.posX + 0.5, this.posY + 0.5, this.posZ + 0.5));
                        this.onImpact(m, 1.0F);
                        break;
                    }
                }
            }
        }

        this.onUpdateBomblet();
    }

    @Override
    public void sprinkleBomblet() {
        if (!this.world.isRemote) {
            MCH_EntityBomb e = new MCH_EntityBomb(
                    this.world, this.posX, this.posY, this.posZ, this.motionX, this.motionY, this.motionZ,
                    this.rand.nextInt(360), 0.0F, this.acceleration);
            e.setParameterFromWeapon(this, this.shootingAircraft, this.shootingEntity);
            e.setName(this.getName());
            float RANDOM = this.getInfo().bombletDiff;
            e.motionX = this.motionX + (this.rand.nextFloat() - 0.5F) * RANDOM;
            e.motionY = this.motionY / 2.0 + (this.rand.nextFloat() - 0.5F) * RANDOM / 2.0F;
            e.motionZ = this.motionZ + (this.rand.nextFloat() - 0.5F) * RANDOM;
            e.setBomblet();
            this.world.spawnEntity(e);
        }
    }

    @Override
    public MCH_BulletModel getDefaultBulletModel() {
        return MCH_DefaultBulletModels.Bomb;
    }
}
