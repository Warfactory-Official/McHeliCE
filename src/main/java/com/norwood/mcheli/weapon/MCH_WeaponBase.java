package com.norwood.mcheli.weapon;

import java.util.Random;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.wrapper.W_McClient;
import com.norwood.mcheli.wrapper.W_WorldFunc;

public abstract class MCH_WeaponBase {

    protected static final Random rand = new Random();
    public final World worldObj;
    public final Vec3d position;
    public final float fixRotationYaw;
    public final float fixRotationPitch;
    public final String name;
    public final MCH_WeaponInfo weaponInfo;
    public String displayName;
    public int power;
    public float acceleration;
    public int explosionPower;
    public int explosionPowerInWater;
    public int interval;
    public int delayedInterval;
    public int numMode;
    public final int lockTime;
    public int piercing;
    public int heatCount;
    public MCH_Cartridge cartridge;
    public boolean onTurret;
    public MCH_EntityAircraft aircraft;
    public int tick;
    public int optionParameter1;
    public int optionParameter2;
    public boolean canPlaySound;
    private int currentMode;
    public int nukeYield;

    public MCH_WeaponBase(World w, Vec3d v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
        this.worldObj = w;
        this.position = v;
        this.fixRotationYaw = yaw;
        this.fixRotationPitch = pitch;
        this.name = nm;
        this.weaponInfo = wi;
        this.displayName = wi != null ? wi.displayName : "";
        this.power = 0;
        this.acceleration = 0.0F;
        this.nukeYield = 0; // REMOVE ME
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

    public String getName() {
        return this.displayName;
    }

    public abstract boolean shot(MCH_WeaponParam var1);

    public void setLockChecker(MCH_IEntityLockChecker checker) {}

    public int getLockCount() {
        return 0;
    }

    public int getLockCountMax() {
        return 0;
    }

    public void setLockCountMax(int n) {}

    public final int getNumAmmoMax() {
        return this.getInfo().round;
    }

    public int getCurrentMode() {
        return this.getInfo() != null && this.getInfo().fixMode > 0 ? this.getInfo().fixMode : this.currentMode;
    }

    public void setCurrentMode(int currentMode) {
        this.currentMode = currentMode;
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

    public void modifyParameters() {}

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

    public void onSwitchMode() {}

    public boolean use(MCH_WeaponParam prm) {
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

    public Vec3d getShotPos(Entity entity) {
        if (entity instanceof MCH_EntityAircraft && this.onTurret) {
            return ((MCH_EntityAircraft) entity).calcOnTurretPos(this.position);
        } else {
            Vec3d v = new Vec3d(this.position.x, this.position.y, this.position.z);
            float roll = entity instanceof MCH_EntityAircraft ? ((MCH_EntityAircraft) entity).getRotRoll() : 0.0F;
            return MCH_Lib.RotVec3(v, -entity.rotationYaw, -entity.rotationPitch, -roll);
        }
    }

    public void playSound(Entity e) {
        this.playSound(e, this.getInfo().soundFileName);
    }

    public void playSound(Entity e, String snd) {
        if (!e.world.isRemote && this.canPlaySound && this.getInfo() != null) {
            float prnd = this.getInfo().soundPitchRandom;
            W_WorldFunc.MOD_playSoundEffect(
                    this.worldObj, e.posX, e.posY, e.posZ, snd, this.getInfo().soundVolume,
                    this.getInfo().soundPitch * (1.0F - prnd) + rand.nextFloat() * prnd);
        }
    }

    public void playSoundClient(Entity e, float volume, float pitch) {
        if (e.world.isRemote && this.getInfo() != null) {
            W_McClient.MOD_playSoundFX(this.getInfo().soundFileName, volume, pitch);
        }
    }

    public double getLandInDistance(MCH_WeaponParam prm) {
        if (this.weaponInfo == null) {
            return -1.0;
        } else if (this.weaponInfo.gravity >= 0.0F) {
            return -1.0;
        } else {
            Vec3d v = MCH_Lib.RotVec3(0.0, 0.0, 1.0, -prm.rotYaw, -prm.rotPitch, -prm.rotRoll);
            double s = Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
            double acc = this.acceleration < 4.0F ? this.acceleration : 4.0;
            double accFac = this.acceleration / acc;
            double my = v.y * this.acceleration / s;
            if (!(my <= 0.0)) {
                double mx = v.x * this.acceleration / s;
                double mz = v.z * this.acceleration / s;
                double ls = my / this.weaponInfo.gravity;
                double gravity = this.weaponInfo.gravity * accFac;
                if (ls < -12.0) {
                    double f = ls / -12.0;
                    mx *= f;
                    my *= f;
                    mz *= f;
                    gravity *= f * f * 0.95;
                }

                double spx = prm.posX;
                double spy = prm.posY + 3.0;
                double spz = prm.posZ;

                for (int i = 0; i < 50; i++) {
                    Vec3d vs = new Vec3d(spx, spy, spz);
                    Vec3d ve = new Vec3d(spx + mx, spy + my, spz + mz);
                    RayTraceResult mop = this.worldObj.rayTraceBlocks(vs, ve);
                    if (mop != null && mop.typeOfHit == Type.BLOCK) {
                        double dx = mop.getBlockPos().getX() - prm.posX;
                        double dz = mop.getBlockPos().getZ() - prm.posZ;
                        return Math.sqrt(dx * dx + dz * dz);
                    }

                    my += gravity;
                    spx += mx;
                    spy += my;
                    spz += mz;
                    if (spy < prm.posY) {
                        double dx = spx - prm.posX;
                        double dz = spz - prm.posZ;
                        return Math.sqrt(dx * dx + dz * dz);
                    }
                }

            }
            return -1.0;
        }
    }
}
