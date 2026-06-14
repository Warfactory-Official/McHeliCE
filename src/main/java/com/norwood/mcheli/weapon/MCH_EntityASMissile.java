package com.norwood.mcheli.weapon;

import com.norwood.mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

public class MCH_EntityASMissile extends MCH_EntityBaseBullet {

    public double targetPosX;
    public double targetPosY;
    public double targetPosZ;

    @SuppressWarnings("unused")
    public MCH_EntityASMissile(World world) {
        super(world);
        this.targetPosX = 0.0;
        this.targetPosY = 0.0;
        this.targetPosZ = 0.0;
    }

    public MCH_EntityASMissile(World world, double posX, double posY, double posZ,
                               double targetX, double targetY, double targetZ,
                               float yaw, float pitch, double acceleration) {
        super(world, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
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

        final var info = this.getInfo();
        if (info == null) return;

        // Trajectory particles for the main missile
        if (!info.disableSmoke && this.getBomblet() == 0) {
            this.spawnParticle(info.trajectoryParticleName, 3, 10.0F * info.smokeSize * 0.5F);
        }

        if (!this.world.isRemote && this.isBomblet != 1) {
            guideMissile(info);
        }

        updateRotation();
        this.onUpdateBomblet();
    }

    private void guideMissile(MCH_WeaponInfo info) {
        BlockPos targetPos = new BlockPos(this.targetPosX, this.targetPosY, this.targetPosZ);
        IBlockState targetState = this.world.getBlockState(targetPos);

        if (targetState.getMaterial().isSolid()) {
            double distanceToTarget = this.getDistance(this.targetPosX, this.targetPosY, this.targetPosZ);

            // Proximity fuse logic
            if (distanceToTarget < info.proximityFuseDist) {
                if (info.bomblet > 0) {
                    repeat(info.bomblet, this::sprinkleBomblet);
                } else {
                    this.onImpact(new RayTraceResult(this), 1.0F);
                }
                this.setDead();
                return;
            }

            // Directional guidance
            double dirX = this.targetPosX - this.posX;
            double dirY = this.targetPosY - this.posY;
            double dirZ = this.targetPosZ - this.posZ;

            if (this.getGravity() == 0.0) {
                // Initial climb logic for specific missile profiles
                double climbOffset = (this.getCountOnUpdate() < 10) ? 20.0 : 0.0;
                dirY += climbOffset;

                double length = MathHelper.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
                this.motionX = (dirX * this.acceleration) / length;
                this.motionY = (dirY * this.acceleration) / length;
                this.motionZ = (dirZ * this.acceleration) / length;
            } else {
                // Standard ballistic trajectory adjustment
                double horizontalLength = MathHelper.sqrt(dirX * dirX + dirZ * dirZ);
                this.motionX = (dirX * this.acceleration) / horizontalLength;
                this.motionZ = (dirZ * this.acceleration) / horizontalLength;
            }
        }
    }

    private void updateRotation() {
        double horizontalMotion = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
        this.rotationYaw = (float) Math.toDegrees(Math.atan2(this.motionZ, this.motionX)) - 90.0F;
        this.rotationPitch = -(float) Math.toDegrees(Math.atan2(this.motionY, horizontalMotion));
    }

    @Override
    public void sprinkleBomblet() {
        if (this.world.isRemote) return;

        MCH_EntityASMissile bomblet = new MCH_EntityASMissile(
                this.world, this.posX, this.posY, this.posZ,
                this.motionX, this.motionY, this.motionZ,
                this.rand.nextInt(360), 0.0F, this.acceleration);

        bomblet.setParameterFromWeapon(this, this.shootingAircraft, this.shootingEntity);
        bomblet.setName(this.getName());

        float spread = this.getInfo().bombletDiff;
        bomblet.motionX = this.motionX * 0.5 + (this.rand.nextFloat() - 0.5F) * spread;
        bomblet.motionY = (this.motionY * 0.5 / 2.0) + (this.rand.nextFloat() - 0.5F) * spread / 2.0F;
        bomblet.motionZ = this.motionZ * 0.5 + (this.rand.nextFloat() - 0.5F) * spread;

        bomblet.setBomblet();
        this.world.spawnEntity(bomblet);
    }

    private void repeat(int times, Runnable action) {
        for (int i = 0; i < times; i++) action.run();
    }

    @Override
    public MCH_BulletModel getDefaultBulletModel() {
        return MCH_DefaultBulletModels.ASMissile;
    }
}