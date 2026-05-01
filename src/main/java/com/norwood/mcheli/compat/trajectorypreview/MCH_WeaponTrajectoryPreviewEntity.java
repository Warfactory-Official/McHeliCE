package com.norwood.mcheli.compat.trajectorypreview;

import alexiy.projectile.preview.api.InvisibleEntity;
import alexiy.projectile.preview.api.PreviewEntity;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.weapon.MCH_WeaponBase;
import com.norwood.mcheli.weapon.MCH_WeaponBomb;
import com.norwood.mcheli.weapon.MCH_WeaponInfo;
import com.norwood.mcheli.weapon.MCH_WeaponMachineGun1;
import com.norwood.mcheli.weapon.MCH_WeaponMachineGun2;
import com.norwood.mcheli.weapon.MCH_WeaponRocket;
import com.norwood.mcheli.weapon.MCH_WeaponSet;
import com.norwood.mcheli.weapon.MCH_WeaponTorpedo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@InvisibleEntity
public class MCH_WeaponTrajectoryPreviewEntity extends Entity implements PreviewEntity {

    private static final int MAX_STEPS = 512;

    private MCH_WeaponInfo weaponInfo;
    private double accelerationFactor = 1.0;
    private int ticksSimulated = 0;

    public MCH_WeaponTrajectoryPreviewEntity(World world) {
        super(world);
        this.noClip = true;
        this.setSize(0.1F, 0.1F);
    }

    @Override
    public Entity initializeEntity(EntityPlayer player, ItemStack itemStack) {
        MCH_EntityAircraft aircraft = MCH_EntityAircraft.getAircraft_RiddenOrControl(player);
        if (aircraft == null || aircraft.isDestroyed()) {
            return null;
        }

        MCH_WeaponSet weaponSet = aircraft.getCurrentWeapon(player);
        MCH_WeaponBase weapon = weaponSet != null ? weaponSet.getCurrentWeapon() : null;
        if (weapon == null || weapon.getInfo() == null) {
            return null;
        }

        this.weaponInfo = weapon.getInfo();
        this.ticksSimulated = 0;
        this.accelerationFactor = computeAccelerationFactor(weapon);

        float yaw = aircraft.getCurrentWeaponShotYaw(player);
        float pitch = aircraft.getCurrentWeaponShotPitch(player);
        Vec3d shotPos = weapon.getShotPos(aircraft).add(aircraft.posX, aircraft.posY, aircraft.posZ);
        Vec3d look = MCH_Lib.RotVec3(0.0, 0.0, 1.0, -yaw, -pitch, 0.0F).normalize();
        Vec3d motion = computeInitialMotion(weapon, aircraft, look);

        this.setPosition(shotPos.x, shotPos.y, shotPos.z);
        this.prevPosX = this.lastTickPosX = shotPos.x;
        this.prevPosY = this.lastTickPosY = shotPos.y;
        this.prevPosZ = this.lastTickPosZ = shotPos.z;
        this.motionX = motion.x;
        this.motionY = motion.y;
        this.motionZ = motion.z;
        this.rotationYaw = this.prevRotationYaw = yaw;
        this.rotationPitch = this.prevRotationPitch = pitch;

        if (weapon instanceof MCH_WeaponMachineGun1 || weapon instanceof MCH_WeaponMachineGun2) {
            this.posX += this.motionX * 0.5;
            this.posY += this.motionY * 0.5;
            this.posZ += this.motionZ * 0.5;
            this.setPosition(this.posX, this.posY, this.posZ);
        }

        return this;
    }

    private double computeAccelerationFactor(MCH_WeaponBase weapon) {
        if (weapon.getInfo() == null) {
            return 1.0;
        }

        if ((weapon instanceof MCH_WeaponMachineGun1 || weapon instanceof MCH_WeaponMachineGun2 ||
                weapon instanceof MCH_WeaponRocket) && weapon.getInfo().acceleration > 4.0F) {
            return weapon.getInfo().acceleration / 4.0F;
        }

        return 1.0;
    }

    private Vec3d computeInitialMotion(MCH_WeaponBase weapon, MCH_EntityAircraft aircraft, Vec3d look) {
        if (weapon instanceof MCH_WeaponBomb) {
            return new Vec3d(aircraft.motionX, aircraft.motionY, aircraft.motionZ);
        }

        if (weapon instanceof MCH_WeaponTorpedo && weapon.getInfo() != null) {
            return new Vec3d(
                    look.x * weapon.getInfo().acceleration + aircraft.motionX,
                    look.y * weapon.getInfo().acceleration + aircraft.motionY,
                    look.z * weapon.getInfo().acceleration + aircraft.motionZ);
        }

        double accel = Math.min(weapon.acceleration, 3.9F);
        return new Vec3d(look.x * accel, look.y * accel, look.z * accel);
    }

    @Override
    public void simulateShot(Entity ignored) {
        if (this.isDead || this.weaponInfo == null) {
            this.setDead();
            return;
        }

        if (++this.ticksSimulated > getMaxLifetime()) {
            this.setDead();
            return;
        }

        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        if (!this.isInWater()) {
            this.motionY += this.weaponInfo.gravity;
        } else {
            this.motionY += this.weaponInfo.gravityInWater;
            if (this.weaponInfo.velocityInWater > 0.0F) {
                this.motionX *= this.weaponInfo.velocityInWater;
                this.motionY *= this.weaponInfo.velocityInWater;
                this.motionZ *= this.weaponInfo.velocityInWater;
            }
        }

        Vec3d start = new Vec3d(this.posX, this.posY, this.posZ);
        Vec3d end = start.add(
                this.motionX * this.accelerationFactor,
                this.motionY * this.accelerationFactor,
                this.motionZ * this.accelerationFactor);
        RayTraceResult hit = this.world.rayTraceBlocks(start, end, false, true, false);
        if (hit != null && hit.hitVec != null) {
            this.setPosition(hit.hitVec.x, hit.hitVec.y, hit.hitVec.z);
            this.setDead();
            return;
        }

        this.setPosition(end.x, end.y, end.z);
        updateRotationFromMotion();
    }

    private int getMaxLifetime() {
        return this.weaponInfo != null && this.weaponInfo.timeFuse > 0 ?
                Math.min(MAX_STEPS, this.weaponInfo.timeFuse) : MAX_STEPS;
    }

    private void updateRotationFromMotion() {
        double horizontal = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
        this.rotationYaw = (float) (Math.atan2(this.motionZ, this.motionX) * 180.0 / Math.PI) - 90.0F;
        this.rotationPitch = -((float) (Math.atan2(this.motionY, horizontal) * 180.0 / Math.PI));
        this.rotationYaw = MathHelper.wrapDegrees(this.rotationYaw);
        this.rotationPitch = MathHelper.wrapDegrees(this.rotationPitch);
    }

    @Override
    protected void entityInit() {}

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {}

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {}
}
