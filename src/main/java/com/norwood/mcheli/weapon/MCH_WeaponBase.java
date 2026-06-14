package com.norwood.mcheli.weapon;

import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.sound.MCH_SoundEvents;
import com.norwood.mcheli.tank.MCH_EntityTank;
import com.norwood.mcheli.vehicle.MCH_EntityVehicle;
import com.norwood.mcheli.wrapper.W_McClient;
import lombok.Setter;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Random;

public abstract class MCH_WeaponBase {

    protected static final Random rand = new Random();
    public final World world;
    public final Vec3d position;
    public final float fixRotationYaw;
    public final float fixRotationPitch;
    public final String name;
    public final MCH_WeaponInfo weaponInfo;
    public final int lockTime;
    public String displayName;
    public int power;
    public float acceleration;
    public int explosionPower;
    public int explosionPowerInWater;
    public int interval;
    public int delayedInterval;
    public int numMode;
    public int piercing;
    public int heatCount;
    public MCH_Cartridge cartridge;
    public boolean onTurret;
    public MCH_EntityAircraft aircraft;
    public int tick;
    public int optionParameter1;
    public int optionParameter2;
    public boolean canPlaySound;
    public int airburstDist = 0;
    @Setter
    private int currentMode;

    public MCH_WeaponBase(World w, Vec3d v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
        this.world = w;
        this.position = v;
        this.fixRotationYaw = yaw;
        this.fixRotationPitch = pitch;
        this.name = nm;
        this.weaponInfo = wi;
        this.displayName = wi != null ? wi.displayName : "";
        this.power = 0;
        this.acceleration = 0.0F;
        this.explosionPower = 0;
        this.explosionPowerInWater = 0;
        this.interval = 1;
        this.numMode = 0;
        this.lockTime = 0;
        this.heatCount = 0;
        this.cartridge = null;
        this.tick = 0;
        this.optionParameter1 = 0;
        this.optionParameter2 = 0;
        this.setCurrentMode(0);
        this.canPlaySound = true;
    }

    public MCH_WeaponInfo getInfo() {
        return this.weaponInfo;
    }

    public void setAirburstDist(int dist) {
        this.airburstDist = dist;
    }

    public boolean lock(MCH_WeaponParam prm) {
        return false;
    }

    public void onUnlock(MCH_WeaponParam prm) {
    }



    public String getName() {
        return this.displayName;
    }

    public abstract boolean shot(MCH_WeaponParam var1);

    public void setLockChecker(MCH_IEntityLockChecker checker) {
    }

    public int getLockCount() {
        return 0;
    }

    public int getLockCountMax() {
        return 0;
    }

    public void setLockCountMax(int n) {
    }

    public final int getNumAmmoMax() {
        return this.getInfo().round;
    }

    public int getCurrentMode() {
        return this.getInfo() != null && this.getInfo().fixMode > 0 ? this.getInfo().fixMode : this.currentMode;
    }

    public final int getAllAmmoNum() {
        return this.getInfo().maxAmmo;
    }

    public final int getReloadCount() {
        return this.getInfo().reloadTime;
    }

    public final MCH_SightType getSightType() {
        return this.getInfo().sight;
    }

    public MCH_WeaponGuidanceSystem getGuidanceSystem() {
        return null;
    }

    public void update(int countWait) {
        if (countWait != 0) {
            this.tick++;
        }
    }

    public boolean isCooldownCountReloadTime() {
        return false;
    }

    public void modifyCommonParameters() {
        this.modifyParameters();
    }

    public void modifyParameters() {
    }

    public boolean switchMode() {
        if (this.getInfo() != null && this.getInfo().fixMode > 0) {
            return false;
        } else {
            int beforeMode = this.getCurrentMode();
            if (this.numMode > 0) {
                this.setCurrentMode((this.getCurrentMode() + 1) % this.numMode);
            } else {
                this.setCurrentMode(0);
            }

            if (beforeMode != this.getCurrentMode()) {
                this.onSwitchMode();
            }

            return beforeMode != this.getCurrentMode();
        }
    }

    public void onSwitchMode() {
    }

    public boolean use(@NotNull MCH_WeaponParam prm) {
        Vec3d v = this.getShotPos(prm.entity);
        prm.posX = prm.posX + v.x;
        prm.posY = prm.posY + v.y;
        prm.posZ = prm.posZ + v.z;
        if (this.shot(prm)) {
            this.tick = 0;
            return true;
        } else {
            return false;
        }
    }
    protected Vec2f calculateShotRotation(MCH_WeaponParam prm, boolean applyYawLimit, boolean useSeatPitch) {
        float yaw = prm.rotYaw;
        float pitch = prm.rotPitch;

        if (yaw == 0.0F && pitch == 0.0F) {
            yaw = getInfo().enableOffAxis ? prm.user.rotationYaw + this.fixRotationYaw : prm.entity.rotationYaw + this.fixRotationYaw;
            pitch = getInfo().enableOffAxis ? prm.user.rotationPitch + this.fixRotationPitch : prm.entity.rotationPitch + this.fixRotationPitch;
        }

        if (prm.entity instanceof MCH_EntityTank tank) {
            float minPitch, maxPitch;
            if (useSeatPitch) {
                var seat = tank.getSeatInfo(prm.entity);
                minPitch = (seat == null) ? tank.getAcInfo().minRotationPitch : seat.minPitch;
                maxPitch = (seat == null) ? tank.getAcInfo().maxRotationPitch : seat.maxPitch;
            } else {
                var weapon = tank.getAcInfo().getWeaponById(tank.getCurrentWeaponID(prm.user));
                minPitch = (weapon == null) ? tank.getAcInfo().minRotationPitch : weapon.minPitch;
                maxPitch = (weapon == null) ? tank.getAcInfo().maxRotationPitch : weapon.maxPitch;
            }

            float playerYawRel = MathHelper.wrapDegrees(tank.getYaw() - yaw);
            float radYaw = (float) Math.toRadians(playerYawRel);
            float playerPitch = tank.getPitch() * MathHelper.cos(radYaw) - tank.getRoll() * MathHelper.sin(radYaw);

            if (applyYawLimit) {
                var weapon = tank.getAcInfo().getWeaponById(tank.getCurrentWeaponID(prm.user));
                float yawLimit = (weapon == null) ? 360F : weapon.maxYaw;
                float relativeYaw = MathHelper.clamp(MathHelper.wrapDegrees(yaw - tank.getYaw()), -yawLimit, yawLimit);
                yaw = tank.getYaw() + relativeYaw;
            }

            pitch = MathHelper.clamp(pitch, playerPitch + minPitch, playerPitch + maxPitch);
            pitch = MathHelper.clamp(pitch, -90.0F, 90.0F);
        }

        return new Vec2f(MathHelper.wrapDegrees(yaw), pitch);
    }

    public Vec3d getShotPos(Entity entity) {
        if (entity instanceof MCH_EntityVehicle vehicle && vehicle.isDetachedWeaponAimActive()) {
            return vehicle.getCurrentWeaponShotPos(this.position, null);
        }
        if (entity instanceof MCH_EntityAircraft && this.onTurret) {
            return ((MCH_EntityAircraft) entity).calcOnTurretPos(this.position);
        } else {
            Vec3d v = new Vec3d(this.position.x, this.position.y, this.position.z);
            float roll = entity instanceof MCH_EntityAircraft ? ((MCH_EntityAircraft) entity).getRoll() : 0.0F;
            return MCH_Lib.RotVec3(v, -entity.rotationYaw, -entity.rotationPitch, -roll);
        }
    }

    public void playSound(Entity e) {
        this.playSound(e, this.getInfo().fireSound);
    }

    public void playSound(Entity e, ResourceLocation snd) {
        if (!e.world.isRemote && this.canPlaySound && this.getInfo() != null) {
            float prnd = this.getInfo().soundPitchRandom;
            float pitch = this.getInfo().soundPitch * (1.0F - prnd) + rand.nextFloat() * prnd;
            MCH_SoundEvents.playSound(this.world, e.posX, e.posY, e.posZ, snd, this.getInfo().soundVolume, pitch);
        }
    }

    public void playSoundClient(Entity e, float volume, float pitch) {
        if (e.world.isRemote && this.getInfo() != null) {
            W_McClient.playSound(this.getInfo().fireSound, volume, pitch);
        }
    }

    public double getLandInDistance(MCH_WeaponParam prm) {
        Vec3d impact = getImpactPos(prm);
        if (impact == null) return -1.0;
        double dx = impact.x - prm.posX;
        double dz = impact.z - prm.posZ;
        return Math.sqrt(dx * dx + dz * dz);
    }

    @Nullable
    public Vec3d getImpactPos(MCH_WeaponParam prm) {
        if (this.weaponInfo == null) return null;

        double accelFactor = 1.0;
        if ((this instanceof MCH_WeaponMachineGun1 || this instanceof MCH_WeaponMachineGun2 ||
                this instanceof MCH_WeaponRocket) && this.weaponInfo.acceleration > 4.0F) {
            accelFactor = this.weaponInfo.acceleration / 4.0F;
        }

        Vec3d look = MCH_Lib.RotVec3(0.0, 0.0, 1.0, -prm.rotYaw, -prm.rotPitch, -prm.rotRoll).normalize();

        Vec3d motion;
        if (this instanceof MCH_WeaponBomb) {
            motion = new Vec3d(prm.entity.motionX, prm.entity.motionY, prm.entity.motionZ);
        } else if (this instanceof MCH_WeaponTorpedo) {
            motion = new Vec3d(
                    look.x * this.weaponInfo.acceleration + prm.entity.motionX,
                    look.y * this.weaponInfo.acceleration + prm.entity.motionY,
                    look.z * this.weaponInfo.acceleration + prm.entity.motionZ);
        } else {
            double accel = Math.min(this.acceleration, 3.9F);
            motion = new Vec3d(look.x * accel, look.y * accel, look.z * accel);
        }

        double spx = prm.posX;
        double spy = prm.posY;
        double spz = prm.posZ;

        if (this instanceof MCH_WeaponMachineGun1 || this instanceof MCH_WeaponMachineGun2) {
            spx += motion.x * 0.5;
            spy += motion.y * 0.5;
            spz += motion.z * 0.5;
        }

        double mx = motion.x;
        double my = motion.y;
        double mz = motion.z;

        int maxSteps = this.weaponInfo.timeFuse > 0 ? Math.min(512, this.weaponInfo.timeFuse) : 512;

        for (int i = 0; i < maxSteps; i++) {
            Vec3d start = new Vec3d(spx, spy, spz);

            if (this.world.isBlockLoaded(new BlockPos(spx, spy, spz))) {
                if (this.world.getBlockState(new BlockPos(spx, spy, spz)).getMaterial() != Material.WATER) {
                    my += this.weaponInfo.gravity;
                } else {
                    my += this.weaponInfo.gravityInWater;
                    if (this.weaponInfo.velocityInWater > 0.0F) {
                        mx *= this.weaponInfo.velocityInWater;
                        my *= this.weaponInfo.velocityInWater;
                        mz *= this.weaponInfo.velocityInWater;
                    }
                }
            } else {
                my += this.weaponInfo.gravity;
            }

            Vec3d end = start.add(mx * accelFactor, my * accelFactor, mz * accelFactor);
            RayTraceResult hit = this.world.rayTraceBlocks(start, end, false, true, false);
            if (hit != null && hit.typeOfHit == Type.BLOCK) {
                return hit.hitVec;
            }

            spx = end.x;
            spy = end.y;
            spz = end.z;

            if (spy < prm.posY - 100) return null;
        }
        return null;
    }
}
