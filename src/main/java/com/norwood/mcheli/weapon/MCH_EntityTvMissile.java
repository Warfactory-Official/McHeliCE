package com.norwood.mcheli.weapon;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class MCH_EntityTvMissile extends MCH_EntityBaseBullet {

    public boolean isSpawnParticle = true;
    public double targetPosX, targetPosY, targetPosZ;

    public MCH_EntityTvMissile(World world) {
        super(world);
    }

    public MCH_EntityTvMissile(World world, double x, double y, double z, double tx, double ty, double tz, float yaw, float pitch, double accel) {
        super(world, x, y, z, tx, ty, tz, yaw, pitch, accel);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        this.onUpdateBomblet();

        final var info = getInfo();
        if (this.isSpawnParticle && info != null && !info.disableSmoke) {
            this.spawnParticle(info.trajectoryParticleName, 3, 2.5F * info.smokeSize);
        }

        if (shootingEntity == null) {
            if (!world.isRemote) setDead();
            return;
        }

        if (getDistanceSq(shootingEntity) > 1440000.0D) {
            setDead();
            return;
        }

        if (!world.isRemote && !isDead) {
            onUpdateMotion();
        }
    }

    public void onUpdateMotion() {
        final var info = getInfo();
        if (info == null) return;

        Entity shooter = shootingEntity;
        MCH_EntityAircraft ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(shooter);

        if (!info.laserGuidance) {
            handleWireGuidance(shooter, ac);
        } else {
            handleLaserGuidance(shooter, ac, info);
        }
    }

    private void handleWireGuidance(Entity shooter, MCH_EntityAircraft ac) {
        if (ac == null || ac.getTVMissile() != this) return;

        float yaw = shooter.rotationYaw;
        float pitch = shooter.rotationPitch;

        double tX = -Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));
        double tZ = Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));
        double tY = -Math.sin(Math.toRadians(pitch));

        this.setMotion(tX, tY, tZ);
        this.setRotation(yaw, pitch);
    }

    private void handleLaserGuidance(Entity shooter, MCH_EntityAircraft ac, MCH_WeaponInfo info) {
        if (ac != null && ac.getCurrentWeapon(shooter).getCurrentWeapon() instanceof MCH_WeaponTvMissile weapon) {
            if (weapon.guidanceSystem != null && !weapon.guidanceSystem.targeting) return;
        }

        float yaw = info.hasLaserGuidancePod ? shooter.rotationYaw : shootingAircraft.rotationYaw;
        float pitch = info.hasLaserGuidancePod ? shooter.rotationPitch : shootingAircraft.rotationPitch;

        Vec3d lookDir = new Vec3d(
                -Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)),
                -Math.sin(Math.toRadians(pitch)),
                Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch))
        );

        updateLaserTargetPoint(shooter, ac, lookDir);
        applyLaserSteering();
    }

    private void updateLaserTargetPoint(Entity shooter, MCH_EntityAircraft ac, Vec3d lookDir) {
        Vec3d src = world.isRemote ? clientTarget() : getServerLaserSource(shooter, ac);
        double maxDist = 1500.0;
        Vec3d dst = src.add(lookDir.scale(maxDist));

        RayTraceResult result = world.rayTraceBlocks(src, dst, false, true, true);

        if (!world.isRemote) {
            Vec3d hit = (result != null) ? result.hitVec : dst;
            targetPosX = hit.x;
            targetPosY = hit.y;
            targetPosZ = hit.z;
        }
    }

    private void applyLaserSteering() {
        BlockPos targetPos = new BlockPos(targetPosX, targetPosY, targetPosZ);
        if (!world.isBlockLoaded(targetPos) || world.isAirBlock(targetPos)) return;

        double dx = targetPosX - posX;
        double dy = targetPosY - posY;
        double dz = targetPosZ - posZ;

        if (getGravity() != 0.0f) dy *= 0.3D; // Gravity-assisted drop logic

        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 0.001) return;

        double targetVelX = (dx * acceleration) / dist;
        double targetVelY = (dy * acceleration) / dist;
        double targetVelZ = (dz * acceleration) / dist;

        // Turning interpolation
        motionX += (targetVelX - motionX) * getInfo().turningFactor;
        motionY += (targetVelY - motionY) * getInfo().turningFactor;
        motionZ += (targetVelZ - motionZ) * getInfo().turningFactor;

        limitSpeed(getInfo().acceleration);
        updateMissileRotation();
    }

    private void limitSpeed(double max) {
        double speed = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        if (speed > max) {
            double scale = max / speed;
            motionX *= scale; motionY *= scale; motionZ *= scale;
        }
    }

    private void updateMissileRotation() {
        double hSpeed = Math.sqrt(motionX * motionX + motionZ * motionZ);
        rotationYaw = (float) Math.toDegrees(Math.atan2(motionZ, motionX)) - 90.0F;
        rotationPitch = -(float) Math.toDegrees(Math.atan2(motionY, hSpeed));
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
        // Use Minecraft instance for 1.12.2 compatibility
        var rm = Minecraft.getMinecraft().getRenderManager();
        return new Vec3d(rm.viewerPosX, rm.viewerPosY, rm.viewerPosZ);
    }

    public void setMotion(double tx, double ty, double tz) {
        double d = Math.sqrt(tx * tx + ty * ty + tz * tz);
        motionX = tx * acceleration / d;
        motionY = ty * acceleration / d;
        motionZ = tz * acceleration / d;
    }

    @Override
    public void sprinkleBomblet() {
        if (world.isRemote) return;
        MCH_EntityRocket e = new MCH_EntityRocket(world, posX, posY, posZ, motionX, motionY, motionZ, rotationYaw, rotationPitch, acceleration);
        e.setName(getName());
        e.setParameterFromWeapon(this, shootingAircraft, shootingEntity);

        double spread = getInfo().bombletDiff;
        e.motionX += (rand.nextDouble() - 0.5) * spread;
        e.motionY += (rand.nextDouble() - 0.5) * spread;
        e.motionZ += (rand.nextDouble() - 0.5) * spread;

        e.setBomblet();
        world.spawnEntity(e);
    }

    @Override
    public MCH_BulletModel getDefaultBulletModel() {
        return MCH_DefaultBulletModels.ATMissile;
    }
}
