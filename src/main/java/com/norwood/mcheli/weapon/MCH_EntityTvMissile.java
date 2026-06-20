package com.norwood.mcheli.weapon;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.wingman.config.WingmanConfig;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Objects;

public class MCH_EntityTvMissile extends MCH_EntityBaseBullet {

    public boolean isSpawnParticle = true;
    public double targetPosX;
    public double targetPosY;
    public double targetPosZ;

    public MCH_EntityTvMissile(World world) {
        super(world);
    }

    public MCH_EntityTvMissile(World world, double posX, double posY, double posZ, double targetX, double targetY, double targetZ, float yaw, float pitch, double acceleration) {
        super(world, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
    }

    public void setMotion(double targetX, double targetY, double targetZ) {
        double distance = Math.sqrt(targetX * targetX + targetY * targetY + targetZ * targetZ);
        this.motionX = (targetX * this.acceleration) / distance;
        this.motionY = (targetY * this.acceleration) / distance;
        this.motionZ = (targetZ * this.acceleration) / distance;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        this.onUpdateBomblet();

        final var info = this.getInfo();
        if (this.isSpawnParticle && info != null && !info.disableSmoke) {
            this.spawnParticle(info.trajectoryParticleName, 3, 5.0F * info.smokeSize * 0.5F);
        }

        if (this.shootingEntity != null) {
            int maxRange = WingmanConfig.tvMissileMaxRange;
            if (maxRange >= 0 && this.getDistanceSq(this.shootingEntity) > (double) maxRange * maxRange) {
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
        Entity shooter = this.shootingEntity;
        if (shooter == null || shooter.isDead) return;

        MCH_EntityAircraft ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(shooter);
        final var info = this.getInfo();

        if (info == null) return;

        if (!info.laserGuidance) {
            handleWireGuidance(shooter, ac);
        } else {
            handleLaserGuidance(shooter, ac, info);
        }
    }

    private void handleWireGuidance(Entity shooter, MCH_EntityAircraft ac) {
        if (ac != null && ac.getTVMissile() == this) {
            float yaw = shooter.rotationYaw;
            float pitch = shooter.rotationPitch;

            double tX = -Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));
            double tZ = Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));
            double tY = -Math.sin(Math.toRadians(pitch));

            this.setMotion(tX, tY, tZ);
            this.setRotation(yaw, pitch);
        }
    }

    private void handleLaserGuidance(Entity shooter, MCH_EntityAircraft ac, MCH_WeaponInfo info) {
        if (ac != null && ac.getCurrentWeapon(shooter).getCurrentWeapon() instanceof MCH_WeaponTvMissile weapon) {
            if (weapon.guidanceSystem != null && !weapon.guidanceSystem.targeting) {
                return;
            }
        }

        float yaw = info.hasLaserGuidancePod ? shooter.rotationYaw : this.shootingAircraft.rotationYaw;
        float pitch = info.hasLaserGuidancePod ? shooter.rotationPitch : this.shootingAircraft.rotationPitch;

        Vec3d src = this.world.isRemote ? clientTarget() : getServerLaserSource(shooter, ac);
        Vec3d lookDir = Vec3d.fromPitchYaw(pitch, yaw);
        double maxDist = 1500.0;
        Vec3d dst = src.add(lookDir.scale(maxDist));

        RayTraceResult hitResult = this.world.rayTraceBlocks(src, dst, false, true, true);

        if (!this.world.isRemote) {
            Vec3d hit = (hitResult != null && hitResult.typeOfHit != RayTraceResult.Type.MISS) ? hitResult.hitVec : dst;
            this.targetPosX = hit.x;
            this.targetPosY = hit.y;
            this.targetPosZ = hit.z;
        }

        this.onLaserGuide();
    }

    private Vec3d getServerLaserSource(Entity shooter, MCH_EntityAircraft ac) {
        Entity origin = (ac != null && ac.isUAV()) ? ac : shooter;
        double interpX = origin.prevPosX + (origin.posX - origin.prevPosX) * 0.5;
        double interpY = origin.prevPosY + (origin.posY - origin.prevPosY) * 0.5;
        double interpZ = origin.prevPosZ + (origin.posZ - origin.prevPosZ) * 0.5;

        return new Vec3d(interpX, interpY + (origin == shooter ? origin.getEyeHeight() : 0), interpZ);
    }

    @SideOnly(Side.CLIENT)
    private Vec3d clientTarget() {
        var rm = Minecraft.getMinecraft().getRenderManager();
        return new Vec3d(rm.viewerPosX, rm.viewerPosY, rm.viewerPosZ);
    }

    public void onLaserGuide() {
        BlockPos targetPos = new BlockPos(this.targetPosX, this.targetPosY, this.targetPosZ);
        IBlockState targetState = this.world.getBlockState(targetPos);

        if (targetState.getMaterial().isSolid()) {
            double deltaX = this.targetPosX - this.posX;
            double deltaY = this.targetPosY - this.posY;
            double deltaZ = this.targetPosZ - this.posZ;

            if (this.getGravity() == 0.0D) {
                double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
                if (distance < 0.001) return;

                double targetMotionX = (deltaX * this.acceleration) / distance;
                double targetMotionY = (deltaY * this.acceleration) / distance;
                double targetMotionZ = (deltaZ * this.acceleration) / distance;

                float turn = (float) Objects.requireNonNull(this.getInfo()).turningFactor;
                // Ease speed and heading separately so a hard turn doesn't bleed speed: the per-axis
                // velocity lerp interpolates along a chord, shortening the vector mid-turn. Cruise speed
                // stays capped at the configured maxSpeed.
                double maxSpeed = this.getInfo().acceleration;
                double curSpeed = Math.sqrt(this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ);
                if (curSpeed < 1.0e-6D) {
                    this.motionX += (targetMotionX - this.motionX) * turn;
                    this.motionY += (targetMotionY - this.motionY) * turn;
                    this.motionZ += (targetMotionZ - this.motionZ) * turn;
                } else {
                    double newSpeed = curSpeed + (this.acceleration - curSpeed) * turn;
                    if (newSpeed > maxSpeed) newSpeed = maxSpeed;
                    double cdx = this.motionX / curSpeed, cdy = this.motionY / curSpeed, cdz = this.motionZ / curSpeed;
                    double tdx = deltaX / distance, tdy = deltaY / distance, tdz = deltaZ / distance;
                    double ndx = cdx + (tdx - cdx) * turn;
                    double ndy = cdy + (tdy - cdy) * turn;
                    double ndz = cdz + (tdz - cdz) * turn;
                    double nlen = Math.sqrt(ndx * ndx + ndy * ndy + ndz * ndz);
                    if (nlen < 1.0e-6D) { ndx = tdx; ndy = tdy; ndz = tdz; nlen = 1.0D; }
                    this.motionX = ndx / nlen * newSpeed;
                    this.motionY = ndy / nlen * newSpeed;
                    this.motionZ = ndz / nlen * newSpeed;
                }
            } else {
                deltaY *= 0.3D; // Gravity scaling
                double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
                if (distance < 0.001) return;

                this.motionX = (deltaX * this.acceleration) / distance;
                this.motionZ = (deltaZ * this.acceleration) / distance;
            }
        }

        // Update missile orientation based on motion vectors
        double horizontalSpeed = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
        this.rotationYaw = (float) Math.toDegrees(Math.atan2(this.motionZ, this.motionX)) - 90.0F;
        this.rotationPitch = -(float) Math.toDegrees(Math.atan2(this.motionY, horizontalSpeed));
    }

    @Override
    public void sprinkleBomblet() {
        if (this.world.isRemote) return;

        MCH_EntityRocket rocket = new MCH_EntityRocket(this.world, this.posX, this.posY, this.posZ, this.motionX, this.motionY, this.motionZ, this.rotationYaw, this.rotationPitch, this.acceleration);
        rocket.setName(this.getName());
        rocket.setParameterFromWeapon(this, this.shootingAircraft, this.shootingEntity);

        float spread = this.getInfo().bombletDiff;
        rocket.motionX += (this.rand.nextFloat() - 0.5D) * spread;
        rocket.motionY += (this.rand.nextFloat() - 0.5D) * spread;
        rocket.motionZ += (this.rand.nextFloat() - 0.5D) * spread;

        rocket.setBomblet();
        this.world.spawnEntity(rocket);
    }

    @Override
    public MCH_BulletModel getDefaultBulletModel() {
        return MCH_DefaultBulletModels.ATMissile;
    }
}
