package com.norwood.mcheli.weapon;

import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.mob.MCH_EntityGunner;
import com.norwood.mcheli.vehicle.MCH_EntityVehicle;
import com.norwood.mcheli.wrapper.W_McClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public class MCH_WeaponSet {

    private static final Random rand = new Random();
    private final String name;
    public float rotationYaw;
    public float rotationPitch;
    public float prevRotationYaw;
    public float prevRotationPitch;
    public float defaultRotationYaw;
    public float rotationTurretYaw;
    public float rotBay;
    public float prevRotBay;
    public final MCH_WeaponSet.Recoil[] recoilBuf;
    public int currentHeat;
    public int cooldownSpeed;
    public int countWait;
    public int countReloadWait;
    public int soundWait;
    public float rotBarrelSpd;
    public float rotBarrel;
    public float prevRotBarrel;
    protected final MCH_WeaponBase[] weapons;
    protected int numAmmo;
    protected int numRestAllAmmo;
    protected final int[] lastUsedCount;
    private int currentWeaponIndex;
    private int lastUsedOptionParameter1 = 0;
    private int lastUsedOptionParameter2 = 0;

    public MCH_WeaponSet(MCH_WeaponBase[] weapon) {
        this.name = weapon[0].name;
        this.weapons = weapon;
        this.currentWeaponIndex = 0;
        this.countWait = 0;
        this.countReloadWait = 0;
        this.lastUsedCount = new int[weapon.length];
        this.rotationYaw = 0.0F;
        this.prevRotationYaw = 0.0F;
        this.rotationPitch = 0.0F;
        this.prevRotationPitch = 0.0F;
        this.setAmmoNum(0);
        this.setRestAllAmmoNum(0);
        this.currentHeat = 0;
        this.soundWait = 0;
        this.cooldownSpeed = 1;
        this.rotBarrelSpd = 0.0F;
        this.rotBarrel = 0.0F;
        this.prevRotBarrel = 0.0F;
        this.recoilBuf = new MCH_WeaponSet.Recoil[weapon.length];

        for (int i = 0; i < this.recoilBuf.length; i++) {
            this.recoilBuf[i] = new Recoil(this, weapon[i].getInfo().recoilBufCount,
                    weapon[i].getInfo().recoilBufCountSpeed);
        }

        this.defaultRotationYaw = 0.0F;
    }

    public MCH_WeaponSet(MCH_WeaponBase weapon) {
        this(new MCH_WeaponBase[] { weapon });
    }

    public boolean isEqual(String s) {
        return this.name.equalsIgnoreCase(s);
    }

    public int getAmmoNum() {
        return this.numAmmo;
    }

    public void setAmmoNum(int n) {
        this.numAmmo = n;
    }

    public int getAmmoNumMax() {
        return this.getFirstWeapon().getNumAmmoMax();
    }

    public int getRestAllAmmoNum() {
        return this.numRestAllAmmo;
    }

    public void setRestAllAmmoNum(int n) {
        int debugBefore = this.numRestAllAmmo;
        int m = this.getInfo().maxAmmo - this.getAmmoNum();
        this.numRestAllAmmo = Math.min(n, m);
        MCH_Lib.DbgLog(this.getFirstWeapon().worldObj, "MCH_WeaponSet.setRestAllAmmoNum:%s %d->%d (%d)", this.getName(),
                debugBefore, this.numRestAllAmmo, n);
    }

    public int getAllAmmoNum() {
        return this.getFirstWeapon().getAllAmmoNum();
    }

    public void supplyRestAllAmmo() {
        int m = this.getInfo().maxAmmo;
        if (this.getRestAllAmmoNum() + this.getAmmoNum() < m) {
            this.setRestAllAmmoNum(this.getRestAllAmmoNum() + this.getAmmoNum() + this.getInfo().suppliedNum);
        }
    }

    public boolean isInPreparation() {
        return this.countWait < 0 || this.countReloadWait > 0;
    }

    public String getName() {
        MCH_WeaponBase w = this.getCurrentWeapon();
        return w != null ? w.getName() : "";
    }

    public boolean canUse() {
        return this.countWait == 0;
    }

    public boolean isLongDelayWeapon() {
        return this.getInfo().delay > 4;
    }

    public void reload() {
        MCH_WeaponBase crtWpn = this.getCurrentWeapon();
        if (this.getAmmoNumMax() > 0 && this.getAmmoNum() < this.getAmmoNumMax() && crtWpn.getReloadCount() > 0) {
            this.countReloadWait = crtWpn.getReloadCount();
            if (crtWpn.worldObj.isRemote) {
                this.setAmmoNum(0);
            }

            if (!crtWpn.worldObj.isRemote) {
                this.countReloadWait -= 20;
                if (this.countReloadWait <= 0) {
                    this.countReloadWait = 1;
                }
            }
        }
    }

    public void reloadMag() {
        int restAmmo = this.getRestAllAmmoNum();
        int nAmmo = this.getAmmoNumMax() - this.getAmmoNum();
        if (nAmmo > 0) {
            if (nAmmo > restAmmo) {
                nAmmo = restAmmo;
            }

            this.setAmmoNum(this.getAmmoNum() + nAmmo);
            this.setRestAllAmmoNum(this.getRestAllAmmoNum() - nAmmo);
        }
    }

    public void switchMode() {
        boolean isChanged = false;

        for (MCH_WeaponBase w : this.weapons) {
            if (w != null) {
                isChanged = w.switchMode() || isChanged;
            }
        }

        if (isChanged) {
            int cntSwitch = 15;
            if (this.countWait >= -cntSwitch) {
                if (this.countWait > cntSwitch) {
                    this.countWait = -this.countWait;
                } else {
                    this.countWait = -cntSwitch;
                }
            }

            if (this.getCurrentWeapon().worldObj.isRemote) {
                W_McClient.playSoundClick(1.0F, 1.0F);
            }
        }
    }

    public void onSwitchWeapon(boolean isRemote, boolean isCreative) {
        int cntSwitch = 15;
        if (isRemote) {
            cntSwitch += 10;
        }

        if (this.countWait >= -cntSwitch) {
            if (this.countWait > cntSwitch) {
                this.countWait = -this.countWait;
            } else {
                this.countWait = -cntSwitch;
            }
        }

        this.currentWeaponIndex = 0;
        if (isCreative) {
            this.setAmmoNum(this.getAmmoNumMax());
        }
    }

    public boolean isUsed(int index) {
        MCH_WeaponBase w = this.getFirstWeapon();
        if (w != null && index < this.lastUsedCount.length) {
            int cnt = this.lastUsedCount[index];
            return w.interval >= 4 && cnt > w.interval / 2 || cnt >= 4;
        } else {
            return false;
        }
    }

    public void update(Entity shooter, boolean isSelected, boolean isUsed) {
        if (this.getCurrentWeapon().getInfo() != null) {
            if (this.countReloadWait > 0) {
                this.countReloadWait--;
                if (this.countReloadWait == 0) {
                    this.reloadMag();
                }
            }

            for (int i = 0; i < this.lastUsedCount.length; i++) {
                if (this.lastUsedCount[i] > 0) {
                    if (this.lastUsedCount[i] == 4) {
                        if (0 == this.getCurrentWeaponIndex() && this.canUse() &&
                                (this.getAmmoNum() > 0 || this.getAllAmmoNum() <= 0)) {
                            this.lastUsedCount[i]--;
                        }
                    } else {
                        this.lastUsedCount[i]--;
                    }
                }
            }

            if (this.currentHeat > 0) {
                if (this.currentHeat < this.getCurrentWeapon().getInfo().maxHeatCount) {
                    this.cooldownSpeed++;
                }

                this.currentHeat = this.currentHeat - (this.cooldownSpeed / 20 + 1);
                if (this.currentHeat < 0) {
                    this.currentHeat = 0;
                }
            }

            if (this.countWait > 0) {
                this.countWait--;
            }

            if (this.countWait < 0) {
                this.countWait++;
            }

            this.prevRotationYaw = this.rotationYaw;
            this.prevRotationPitch = this.rotationPitch;
            if (this.weapons != null) {
                for (MCH_WeaponBase w : this.weapons) {
                    if (w != null) {
                        w.update(this.countWait);
                    }
                }
            }

            if (this.soundWait > 0) {
                this.soundWait--;
            }

            if (isUsed && this.rotBarrelSpd < 75.0F) {
                this.rotBarrelSpd = this.rotBarrelSpd + (25 + rand.nextInt(3));
                if (this.rotBarrelSpd > 74.0F) {
                    this.rotBarrelSpd = 74.0F;
                }
            }

            this.prevRotBarrel = this.rotBarrel;
            this.rotBarrel = this.rotBarrel + this.rotBarrelSpd;
            if (this.rotBarrel >= 360.0F) {
                this.rotBarrel -= 360.0F;
                this.prevRotBarrel -= 360.0F;
            }

            if (this.rotBarrelSpd > 0.0F) {
                this.rotBarrelSpd--;
                if (this.rotBarrelSpd < 0.0F) {
                    this.rotBarrelSpd = 0.0F;
                }
            }
        }
    }

    public void updateWeapon(Entity shooter, boolean isUsed, int index) {
        MCH_WeaponBase crtWpn = this.getWeapon(index);
        if (isUsed && shooter.world.isRemote && crtWpn != null && crtWpn.cartridge != null) {
            Vec3d v = crtWpn.getShotPos(shooter);
            float yaw = shooter.rotationYaw;
            float pitch = shooter.rotationPitch;
            if (shooter instanceof MCH_EntityVehicle && shooter.isBeingRidden()) {}

            MCH_EntityCartridge.spawnCartridge(
                    shooter.world,
                    crtWpn.cartridge,
                    shooter.posX + v.x,
                    shooter.posY + v.y,
                    shooter.posZ + v.z,
                    shooter.motionX,
                    shooter.motionY,
                    shooter.motionZ,
                    yaw + this.rotationYaw,
                    pitch + this.rotationPitch);
        }

        if (index < this.recoilBuf.length) {
            MCH_WeaponSet.Recoil r = this.recoilBuf[index];
            r.prevRecoilBuf = r.recoilBuf;
            if (isUsed && r.recoilBufCount <= 0) {
                r.recoilBufCount = r.recoilBufCountMax;
            }

            if (r.recoilBufCount > 0) {
                if (r.recoilBufCountMax <= 1) {
                    r.recoilBuf = 1.0F;
                } else if (r.recoilBufCountMax == 2) {
                    r.recoilBuf = r.recoilBufCount == 2 ? 1.0F : 0.6F;
                } else {
                    if (r.recoilBufCount > r.recoilBufCountMax / 2) {
                        r.recoilBufCount = r.recoilBufCount - r.recoilBufCountSpeed;
                    }

                    float rb = (float) r.recoilBufCount / r.recoilBufCountMax;
                    r.recoilBuf = MathHelper.sin(rb * (float) Math.PI);
                }

                r.recoilBufCount--;
            } else {
                r.recoilBuf = 0.0F;
            }
        }
    }

    public boolean use(MCH_WeaponParam prm) {
        MCH_WeaponBase crtWpn = this.getCurrentWeapon();
        if (crtWpn != null && crtWpn.getInfo() != null) {
            MCH_WeaponInfo info = crtWpn.getInfo();
            if ((this.getAmmoNumMax() <= 0 || this.getAmmoNum() > 0) &&
                    (info.maxHeatCount <= 0 || this.currentHeat < info.maxHeatCount)) {
                crtWpn.canPlaySound = this.soundWait == 0;
                prm.rotYaw = prm.entity != null ? prm.entity.rotationYaw : 0.0F;
                prm.rotPitch = prm.entity != null ? prm.entity.rotationPitch : 0.0F;
                prm.rotYaw = prm.rotYaw + (this.rotationYaw + crtWpn.fixRotationYaw);
                prm.rotPitch = prm.rotPitch + (this.rotationPitch + crtWpn.fixRotationPitch);
                if (info.accuracy > 0.0F) {
                    prm.rotYaw = prm.rotYaw + (rand.nextFloat() - 0.5F) * info.accuracy;
                    prm.rotPitch = prm.rotPitch + (rand.nextFloat() - 0.5F) * info.accuracy;
                }

                prm.rotYaw = MathHelper.wrapDegrees(prm.rotYaw);
                prm.rotPitch = MathHelper.wrapDegrees(prm.rotPitch);
                if (crtWpn.use(prm)) {
                    if (info.maxHeatCount > 0) {
                        this.cooldownSpeed = 1;
                        this.currentHeat = this.currentHeat + crtWpn.heatCount;
                        if (this.currentHeat >= info.maxHeatCount) {
                            this.currentHeat += 30;
                        }
                    }

                    if (info.soundDelay > 0 && this.soundWait == 0) {
                        this.soundWait = info.soundDelay;
                    }

                    this.lastUsedOptionParameter1 = crtWpn.optionParameter1;
                    this.lastUsedOptionParameter2 = crtWpn.optionParameter2;
                    this.lastUsedCount[this.currentWeaponIndex] = crtWpn.interval > 0 ? crtWpn.interval :
                            -crtWpn.interval;
                    if (crtWpn.isCooldownCountReloadTime() &&
                            crtWpn.getReloadCount() - 10 > this.lastUsedCount[this.currentWeaponIndex]) {
                        this.lastUsedCount[this.currentWeaponIndex] = crtWpn.getReloadCount() - 10;
                    }

                    this.currentWeaponIndex = (this.currentWeaponIndex + 1) % this.weapons.length;
                    this.countWait = prm.user instanceof EntityPlayer ? crtWpn.interval : crtWpn.delayedInterval;
                    this.countReloadWait = 0;
                    if (this.getAmmoNum() > 0) {
                        this.setAmmoNum(this.getAmmoNum() - 1);
                    }

                    if (this.getAmmoNum() <= 0) {
                        if (prm.isInfinity && this.getRestAllAmmoNum() < this.getAmmoNumMax()) {
                            this.setRestAllAmmoNum(this.getAmmoNumMax());
                        }

                        this.reload();
                        prm.reload = true;
                        if (prm.user instanceof MCH_EntityGunner) {
                            this.countWait = this.countWait + (this.countWait >= 0 ? 1 : -1) * crtWpn.getReloadCount();
                        }
                    }

                    prm.result = true;
                }
            }
        }

        return prm.result;
    }

    public void waitAndReloadByOther(boolean reload) {
        MCH_WeaponBase crtWpn = this.getCurrentWeapon();
        if (crtWpn != null && crtWpn.getInfo() != null) {
            this.countWait = crtWpn.interval;
            this.countReloadWait = 0;
            if (reload && this.getAmmoNumMax() > 0 && crtWpn.getReloadCount() > 0) {
                this.countReloadWait = crtWpn.getReloadCount();
                if (!crtWpn.worldObj.isRemote) {
                    this.countReloadWait -= 20;
                    if (this.countReloadWait <= 0) {
                        this.countReloadWait = 1;
                    }
                }
            }
        }
    }

    public int getLastUsedOptionParameter1() {
        return this.lastUsedOptionParameter1;
    }

    public int getLastUsedOptionParameter2() {
        return this.lastUsedOptionParameter2;
    }

    public MCH_WeaponBase getFirstWeapon() {
        return this.getWeapon(0);
    }

    public int getCurrentWeaponIndex() {
        return this.currentWeaponIndex;
    }

    public MCH_WeaponBase getCurrentWeapon() {
        return this.getWeapon(this.currentWeaponIndex);
    }

    public MCH_WeaponBase getWeapon(int idx) {
        return this.weapons != null && this.weapons.length > 0 && idx < this.weapons.length ? this.weapons[idx] : null;
    }

    public int getWeaponsCount() {
        return this.weapons != null ? this.weapons.length : 0;
    }

    public MCH_WeaponInfo getInfo() {
        return this.getFirstWeapon().getInfo();
    }

    public double getLandInDistance(MCH_WeaponParam prm) {
        double ret = -1.0;
        MCH_WeaponBase crtWpn = this.getCurrentWeapon();
        if (crtWpn != null && crtWpn.getInfo() != null) {
            prm.rotYaw = prm.entity != null ? prm.entity.rotationYaw : 0.0F;
            prm.rotPitch = prm.entity != null ? prm.entity.rotationPitch : 0.0F;
            prm.rotYaw = prm.rotYaw + (this.rotationYaw + crtWpn.fixRotationYaw);
            prm.rotPitch = prm.rotPitch + (this.rotationPitch + crtWpn.fixRotationPitch);
            prm.rotYaw = MathHelper.wrapDegrees(prm.rotYaw);
            prm.rotPitch = MathHelper.wrapDegrees(prm.rotPitch);
            return crtWpn.getLandInDistance(prm);
        } else {
            return ret;
        }
    }

    public static class Recoil {

        public final int recoilBufCountMax;
        public final int recoilBufCountSpeed;
        public int recoilBufCount;
        public float recoilBuf;
        public float prevRecoilBuf;

        public Recoil(MCH_WeaponSet paramMCH_WeaponSet, int max, int spd) {
            this.recoilBufCountMax = max;
            this.recoilBufCountSpeed = spd;
        }
    }
}
