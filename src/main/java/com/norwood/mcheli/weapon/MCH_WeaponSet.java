package com.norwood.mcheli.weapon;

import com.norwood.mcheli.helper.MCH_Logger;
import com.norwood.mcheli.mob.MCH_EntityGunner;
import com.norwood.mcheli.wrapper.W_McClient;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

@Getter
public class MCH_WeaponSet {

    private static final Random rand = new Random();
    public final MCH_WeaponSet.Recoil[] recoilBuffer;
    protected final MCH_WeaponBase[] weapons;
    protected final int[] lastUsedCount;
    private final String name;
    private int currentHeat;
    public int cooldownSpeed;
    public int cooldown;
    public int reloadCooldown;
    public int soundWait;
    @Setter
    private float rotBarrelSpd, rotBarrel, prevRotBarrel;
    @Setter
    protected int ammo;
    protected int ammoReserve;
    @Setter
    private float yaw, prevYaw, defYaw, turretYaw;
    @Setter
    private float pitch, prevPitch;
    private int currentWeaponIndex;
    private int optionParm1 = 0;
    private int optionParm2 = 0;

    public MCH_WeaponSet(MCH_WeaponBase[] weapon) {
        this.name = weapon[0].name;
        this.weapons = weapon;
        this.currentWeaponIndex = 0;
        this.cooldown = 0;
        this.reloadCooldown = 0;
        this.lastUsedCount = new int[weapon.length];
        this.yaw = 0.0F;
        this.prevYaw = 0.0F;
        this.pitch = 0.0F;
        this.prevPitch = 0.0F;
        this.setAmmo(0);
        this.setReserveAmmo(0);
        this.currentHeat = 0;
        this.soundWait = 0;
        this.cooldownSpeed = 1;
        this.rotBarrelSpd = 0.0F;
        this.rotBarrel = 0.0F;
        this.prevRotBarrel = 0.0F;
        this.recoilBuffer = new MCH_WeaponSet.Recoil[weapon.length];

        for (int i = 0; i < this.recoilBuffer.length; i++) {
            MCH_WeaponInfo info = weapon[i].getInfo();
            this.recoilBuffer[i] = new Recoil(info.recoilBufCount, info.recoilBufCountSpeed);
        }

        this.defYaw = 0.0F;
    }

    public MCH_WeaponSet(MCH_WeaponBase weapon) {
        this(new MCH_WeaponBase[]{weapon});
    }

    public boolean isName(String s) {
        return this.name.equalsIgnoreCase(s);
    }

    public int getMagSize() {
        return this.getFirstWeapon().getNumAmmoMax();
    }

    public int getRestAllAmmoNum() {
        return this.ammoReserve;
    }

    public void setReserveAmmo(int amount) {
        int debugBefore = this.ammoReserve;
        int capacityRemaining = this.getInfo().maxAmmo - this.getAmmo();
        this.ammoReserve = Math.min(amount, capacityRemaining);

        MCH_Logger.debugLog(this.getFirstWeapon().world,
                "MCH_WeaponSet.setRestAllAmmoNum:%s %d->%d (%d)",
                this.getDisplayName(), debugBefore, this.ammoReserve, amount);
    }

    public int getMaxAmmo() {
        return this.getFirstWeapon().getAllAmmoNum();
    }

    public void supplyRestAllAmmo() {
        MCH_WeaponInfo info = this.getInfo();
        if (this.getRestAllAmmoNum() + this.getAmmo() < info.maxAmmo) {
            this.setReserveAmmo(this.getRestAllAmmoNum() + this.getAmmo() + info.suppliedNum);
        }
    }

    public boolean isBusy() {
        return this.cooldown < 0 || this.reloadCooldown > 0;
    }

    public String getDisplayName() {
        MCH_WeaponBase current = this.getCurrentWeapon();
        return current != null ? current.getName() : "";
    }

    public boolean canFire() {
        return this.cooldown == 0 && this.reloadCooldown == 0;
    }

    public boolean hasLongDelay() {
        return this.getInfo().delay > 4;
    }

    public boolean manualReload() {
        MCH_WeaponBase current = this.getCurrentWeapon();
        if (current == null || current.getInfo() == null) {
            return false;
        }

        int inMag = this.getAmmo();
        int inReserve = this.getRestAllAmmoNum();
        boolean hasRoom = inMag < this.getMagSize();
        boolean hasAmmoToLoad = inReserve > 0;

        if (!this.isBusy() && hasRoom && hasAmmoToLoad) {
            this.setAmmo(0);
            this.setReserveAmmo(inReserve + inMag);

            this.reload();

            if (current.world.isRemote) {
                W_McClient.playSoundClick(1.0F, 1.0F);
            }

            return true;
        }

        return false;
    }

    public void reload() {
        MCH_WeaponBase current = this.getCurrentWeapon();
        boolean hasMag = this.getMagSize() > 0;
        boolean needsAmmo = this.getAmmo() < this.getMagSize();

        if (hasMag && needsAmmo && current.getReloadCount() > 0) {
            this.reloadCooldown = current.getReloadCount();

            if (current.world.isRemote) {
                this.setAmmo(0);
            } else {
                this.reloadCooldown = Math.max(1, this.reloadCooldown - 20);
            }
        }
    }

    public void reloadMag() {
        int needed = this.getMagSize() - this.getAmmo();
        if (needed > 0) {
            int transfer = Math.min(needed, this.getRestAllAmmoNum());
            this.setAmmo(this.getAmmo() + transfer);
            this.setReserveAmmo(this.getRestAllAmmoNum() - transfer);
        }
    }

    public void switchMode() {
        boolean isChanged = false;
        for (MCH_WeaponBase w : this.weapons) {
            if (w != null) {
                isChanged |= w.switchMode();
            }
        }

        if (isChanged) {
            applySwitchCooldown(15);
            if (this.getCurrentWeapon().world.isRemote) {
                W_McClient.playSoundClick(1.0F, 1.0F);
            }
        }
    }

    public void onSwitchWeapon(boolean isRemote, boolean isCreative) {
        int cooldownValue = isRemote ? 25 : 15;
        applySwitchCooldown(cooldownValue);

        this.currentWeaponIndex = 0;
        if (isCreative) {
            this.setAmmo(this.getMagSize());
        }
    }

    private void applySwitchCooldown(int threshold) {
        if (this.cooldown >= -threshold) {
            this.cooldown = (this.cooldown > threshold) ? -this.cooldown : -threshold;
        }
    }

    public boolean isUsed(int index) {
        MCH_WeaponBase w = this.getFirstWeapon();
        if (w == null || index >= this.lastUsedCount.length) return false;

        int count = this.lastUsedCount[index];
        return (w.interval >= 4 && count > w.interval / 2) || count >= 4;
    }

    public void update(Entity shooter, boolean isSelected, boolean isUsed) {
        MCH_WeaponInfo info = this.getCurrentWeapon().getInfo();
        if (info == null) return;

        if (this.reloadCooldown > 0) {
            if (--this.reloadCooldown == 0) {
                this.reloadMag();
            }
        }

        updateLastUsedCounts();
        updateHeatMechanics(info);
        updateCooldowns();

        this.prevYaw = this.yaw;
        this.prevPitch = this.pitch;

        if (this.weapons != null) {
            for (MCH_WeaponBase w : this.weapons) {
                if (w != null) w.update(this.cooldown);
            }
        }

        if (this.soundWait > 0) this.soundWait--;

        updateBarrelRotation(isUsed);
    }

    private void updateLastUsedCounts() {
        for (int i = 0; i < this.lastUsedCount.length; i++) {
            if (this.lastUsedCount[i] <= 0) continue;

            if (this.lastUsedCount[i] == 4) {
                boolean canReset = this.currentWeaponIndex == 0 && this.canFire() &&
                        (this.getAmmo() > 0 || this.getMaxAmmo() <= 0);
                if (canReset) this.lastUsedCount[i]--;
            } else {
                this.lastUsedCount[i]--;
            }
        }
    }

    private void updateHeatMechanics(MCH_WeaponInfo info) {
        if (this.currentHeat > 0) {
            if (this.currentHeat < info.maxHeatCount) {
                this.cooldownSpeed++;
            }
            this.currentHeat = Math.max(0, this.currentHeat - (this.cooldownSpeed / 20 + 1));
        }
    }

    private void updateCooldowns() {
        if (this.cooldown > 0) this.cooldown--;
        else if (this.cooldown < 0) this.cooldown++;
    }

    private void updateBarrelRotation(boolean isBeingUsed) {
        if (isBeingUsed && this.rotBarrelSpd < 75.0F) {
            this.rotBarrelSpd = Math.min(74.0F, this.rotBarrelSpd + (25 + rand.nextInt(3)));
        }

        this.prevRotBarrel = this.rotBarrel;
        this.rotBarrel += this.rotBarrelSpd;

        if (this.rotBarrel >= 360.0F) {
            this.rotBarrel -= 360.0F;
            this.prevRotBarrel -= 360.0F;
        }

        if (this.rotBarrelSpd > 0.0F) {
            this.rotBarrelSpd = Math.max(0.0F, this.rotBarrelSpd - 1.0F);
        }
    }

    public void updateWeapon(Entity shooter, boolean isUsed, int index) {
        MCH_WeaponBase current = this.getWeapon(index);

        if (isUsed && shooter.world.isRemote && current != null && current.cartridge != null) {
            Vec3d offset = current.getShotPos(shooter);
            MCH_EntityCartridge.spawnCartridge(
                    shooter.world, current.cartridge,
                    shooter.posX + offset.x, shooter.posY + offset.y, shooter.posZ + offset.z,
                    shooter.motionX, shooter.motionY, shooter.motionZ,
                    shooter.rotationYaw + this.yaw, shooter.rotationPitch + this.pitch);
        }

        if (index < this.recoilBuffer.length) {
            updateRecoil(this.recoilBuffer[index], isUsed);
        }
    }
    private void updateRecoil(Recoil r, boolean isUsed) {
        r.prevRecoilBuf = r.recoilBuf;
        if (isUsed && r.recoilBufCount <= 0) {
            r.recoilBufCount = r.recoilBufCountMax;
        }

        if (r.recoilBufCount > 0) {
            if (r.recoilBufCountMax <= 1) {
                r.recoilBuf = 1.0F;
            } else if (r.recoilBufCountMax == 2) {
                r.recoilBuf = (r.recoilBufCount == 2) ? 1.0F : 0.6F;
            } else {
                if (r.recoilBufCount > r.recoilBufCountMax / 2) {
                    r.recoilBufCount -= r.recoilBufCountSpeed;
                }
                float progress = (float) r.recoilBufCount / r.recoilBufCountMax;
                r.recoilBuf = MathHelper.sin(progress * (float) Math.PI);
            }
            r.recoilBufCount--;
        } else {
            r.recoilBuf = 0.0F;
        }
    }

    public boolean use(MCH_WeaponParam prm) {
        MCH_WeaponBase current = this.getCurrentWeapon();
        if (current == null || current.getInfo() == null) return false;

        MCH_WeaponInfo info = current.getInfo();
        boolean hasAmmo = this.getMagSize() <= 0 || this.getAmmo() > 0;
        boolean notOverheated = info.maxHeatCount <= 0 || this.currentHeat < info.maxHeatCount;

        if (hasAmmo && notOverheated) {
            setupWeaponParam(prm, current, info);

            if (current.use(prm)) {
                handlePostFire(prm, current, info);
                return true;
            }
        }
        return false;
    }

    private void setupWeaponParam(MCH_WeaponParam prm, MCH_WeaponBase current, MCH_WeaponInfo info) {
        current.canPlaySound = (this.soundWait == 0);
        float baseYaw = prm.entity != null ? prm.entity.rotationYaw : 0.0F;
        float basePitch = prm.entity != null ? prm.entity.rotationPitch : 0.0F;

        prm.rotYaw = baseYaw + this.yaw + current.fixRotationYaw;
        prm.rotPitch = basePitch + this.pitch + current.fixRotationPitch;

        if (info.accuracy > 0.0F) {
            prm.rotYaw += (rand.nextFloat() - 0.5F) * info.accuracy;
            prm.rotPitch += (rand.nextFloat() - 0.5F) * info.accuracy;
        }

        prm.rotYaw = MathHelper.wrapDegrees(prm.rotYaw);
        prm.rotPitch = MathHelper.wrapDegrees(prm.rotPitch);
    }

    private void handlePostFire(MCH_WeaponParam prm, MCH_WeaponBase current, MCH_WeaponInfo info) {
        if (info.maxHeatCount > 0) {
            this.cooldownSpeed = 1;
            this.currentHeat += current.heatCount;
            if (this.currentHeat >= info.maxHeatCount) this.currentHeat += 30;
        }

        if (info.soundDelay > 0 && this.soundWait == 0) {
            this.soundWait = info.soundDelay;
        }

        this.optionParm1 = current.optionParameter1;
        this.optionParm2 = current.optionParameter2;

        int interval = current.interval > 0 ? current.interval : -current.interval;
        if (current.isCooldownCountReloadTime()) {
            interval = Math.max(interval, current.getReloadCount() - 10);
        }
        this.lastUsedCount[this.currentWeaponIndex] = interval;

        this.currentWeaponIndex = (this.currentWeaponIndex + 1) % this.weapons.length;
        this.cooldown = (prm.user instanceof EntityPlayer) ? current.interval : current.delayedInterval;
        this.reloadCooldown = 0;

        if (this.getAmmo() > 0) {
            this.setAmmo(this.getAmmo() - 1);
        }

        if (this.getAmmo() <= 0) {
            if (prm.isInfinity && this.getRestAllAmmoNum() < this.getMagSize()) {
                this.setReserveAmmo(this.getMagSize());
            }
            this.reload();
            prm.reload = true;
            if (prm.user instanceof MCH_EntityGunner) {
                this.cooldown += (this.cooldown >= 0 ? 1 : -1) * current.getReloadCount();
            }
        }
        prm.result = true;
    }

    public void waitAndReloadByOther(boolean reload) {
        MCH_WeaponBase current = this.getCurrentWeapon();
        if (current != null && current.getInfo() != null) {
            this.cooldown = current.interval;
            this.reloadCooldown = 0;
            if (reload && this.getMagSize() > 0 && current.getReloadCount() > 0) {
                this.reloadCooldown = Math.max(1, current.getReloadCount() - (current.world.isRemote ? 0 : 20));
            }
        }
    }

    public MCH_WeaponBase getFirstWeapon() {
        return this.getWeapon(0);
    }

    public MCH_WeaponBase getCurrentWeapon() {
        return this.getWeapon(this.currentWeaponIndex);
    }

    public MCH_WeaponBase getWeapon(int idx) {
        if (this.weapons == null || idx < 0 || idx >= this.weapons.length) return null;
        return this.weapons[idx];
    }

    public int getWeaponsCount() {
        return this.weapons != null ? this.weapons.length : 0;
    }

    public MCH_WeaponInfo getInfo() {
        return this.getFirstWeapon().getInfo();
    }

    public double getImpactDistance(MCH_WeaponParam prm) {
        MCH_WeaponBase current = this.getCurrentWeapon();
        if (current == null || current.getInfo() == null) return -1.0;

        float baseYaw = prm.entity != null ? prm.entity.rotationYaw : 0.0F;
        float basePitch = prm.entity != null ? prm.entity.rotationPitch : 0.0F;

        prm.rotYaw = MathHelper.wrapDegrees(baseYaw + this.yaw + current.fixRotationYaw);
        prm.rotPitch = MathHelper.wrapDegrees(basePitch + this.pitch + current.fixRotationPitch);

        return current.getLandInDistance(prm);
    }

    public static class Recoil {
        public final int recoilBufCountMax;
        public final int recoilBufCountSpeed;
        public int recoilBufCount;
        public float recoilBuf;
        public float prevRecoilBuf;

        public Recoil(int max, int spd) {
            this.recoilBufCountMax = max;
            this.recoilBufCountSpeed = spd;
        }
    }
}
