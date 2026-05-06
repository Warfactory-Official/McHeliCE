package com.norwood.mcheli.aircraft.components;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_MissileDetector;
import com.norwood.mcheli.aircraft.MCH_SeatInfo;
import com.norwood.mcheli.flare.MCH_APS;
import com.norwood.mcheli.flare.MCH_Chaff;
import com.norwood.mcheli.flare.MCH_Flare;
import com.norwood.mcheli.helper.MCH_Logger;
import com.norwood.mcheli.mob.MCH_EntityGunner;
import com.norwood.mcheli.networking.packet.PacketIndNotifyAmmoNum;
import com.norwood.mcheli.networking.packet.PacketSyncWeapon;
import com.norwood.mcheli.particles.MCH_ParticleParam;
import com.norwood.mcheli.particles.MCH_ParticlesUtil;
import com.norwood.mcheli.weapon.*;
import com.norwood.mcheli.wrapper.W_Entity;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class WeaponSystemComponent implements IAircraftComponent {
    public final MCH_MissileDetector missileDetector;
    public final MCH_Flare flareDv;
    public final MCH_APS apsDv;
    @Getter
    protected final MCH_WeaponSet dummyWeapon;
    @Getter
    private final MCH_EntityAircraft parent;
    public MCH_Chaff chaff;
    public int recoilCount;
    public float recoilYaw;
    public float recoilValue;
    @Getter
    @Setter
    protected MCH_WeaponSet[] weapons;
    @Getter
    @Setter
    protected MCH_EntityAircraft.WeaponBay[] weaponBays;
    protected int[] currentWeaponID = new int[0];
    protected int useWeaponStat;
    @Getter
    @Setter
    private int chaffUseTime;
    @Getter
    @Setter
    private int ironCurtainRunningTick;
    @Getter
    @Setter
    private float ironCurtainLastFactor = 0.5f;
    @Getter
    @Setter
    private float ironCurtainCurrentFactor = 0.5f;
    @Getter
    @Setter
    private int ironCurtainWaveTimer;
    @Getter
    @Setter
    private int ecmJammerRunningTick;
    @Getter
    @Setter
    private int ecmJammerWaitTick;
    @Getter
    @Setter
    private int jammingTick;

    @Getter
    private int hitStatus;
    @Getter
    private boolean gunnerMode;
    @Setter
    private boolean gunnerModeOtherSeat;
    private boolean gunnerFreeLookMode;
    @Getter
    private boolean detachedWeaponAimActive;
    @Getter
    private float detachedWeaponAimYaw;
    @Getter
    private float prevDetachedWeaponAimYaw;
    @Getter
    private float detachedWeaponAimPitch;
    @Getter
    private float prevDetachedWeaponAimPitch;
    private int currentFlareIndex;
    private MCH_EntityTvMissile tvMissile;

    public WeaponSystemComponent(MCH_EntityAircraft parent) {
        this.parent = parent;
        this.dummyWeapon = new MCH_WeaponSet(new MCH_WeaponDummy(parent.world, Vec3d.ZERO, 0.0F, 0.0F, "", null));
        this.missileDetector = new MCH_MissileDetector(parent, parent.world);
        this.flareDv = new MCH_Flare(parent.world, parent);
        this.chaff = new MCH_Chaff(parent.world, parent);
        this.apsDv = new MCH_APS(parent.world, parent);
        this.weapons = new MCH_WeaponSet[0];
        this.weaponBays = new MCH_EntityAircraft.WeaponBay[0];
        this.useWeaponStat = 0;
        this.hitStatus = 0;
        this.currentFlareIndex = 0;
        this.recoilCount = 0;
        this.recoilYaw = 0.0F;
        this.recoilValue = 0.0F;
    }

    @Override
    public MCH_EntityAircraft getParent() {
        return parent;
    }

    private static boolean hasIndependentMountedAim(MCH_AircraftInfo.Weapon weapon) {
        return weapon.turret
                || Math.abs(weapon.minYaw) > 0.001F
                || Math.abs(weapon.maxYaw) > 0.001F
                || Math.abs(weapon.minPitch) > 0.001F
                || Math.abs(weapon.maxPitch) > 0.001F;
    }

    private static float stepWrappedAngle(float current, float target, float maxStep) {
        return MathHelper.wrapDegrees(current + MathHelper.clamp(MathHelper.wrapDegrees(target - current), -maxStep, maxStep));
    }

    private static float stepLinearAngle(float current, float target, float maxStep) {
        return current + MathHelper.clamp(target - current, -maxStep, maxStep);
    }

    @Override
    public void init() {
    }

    @Override
    public void onUpdate() {
        missileDetector.update();
        flareDv.update();
        if (chaff != null && parent.getAcInfo() != null) {
            chaff.chaffUseTime = parent.getAcInfo().chaffUseTime;
            chaff.chaffWaitTime = parent.getAcInfo().chaffWaitTime;
            chaff.onUpdate();
        }
        if (parent.getAcInfo() != null) {
            apsDv.useTime = parent.getAcInfo().apsUseTime;
            apsDv.waitTime = parent.getAcInfo().apsWaitTime;
            apsDv.range = parent.getAcInfo().apsRange;
            apsDv.onUpdate();
        }

        updateDefensiveSystems();
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        int[] waList = compound.getIntArray("AcWeaponsAmmo");
        for (int i = 0; i < waList.length && i < weapons.length; i++) {
            weapons[i].setReserveAmmo(waList[i]);
            weapons[i].reloadMag();
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        int[] waList = new int[getWeaponNum()];
        for (int i = 0; i < waList.length; i++) {
            waList[i] = weapons[i].getRestAllAmmoNum() + weapons[i].getAmmo();
        }
        compound.setTag("AcWeaponsAmmo", com.norwood.mcheli.wrapper.W_NBTTag.newTagIntArray("AcWeaponsAmmo", waList));
    }

    public boolean useFlare(int type) {
        if (parent.getAcInfo() != null && parent.getAcInfo().haveFlare()) {
            for (int flareType : parent.getAcInfo().flare.types) {
                if (flareType == type) {
                    parent.setCommonStatus(0, true);
                    if (flareDv.use(type)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public int getCurrentFlareType() {
        if (!haveFlare()) {
            return 0;
        }

        assert parent.getAcInfo() != null;
        return parent.getAcInfo().flare.types[currentFlareIndex];
    }

    public void nextFlareType() {
        if (haveFlare()) {
            assert parent.getAcInfo() != null;
            currentFlareIndex = (currentFlareIndex + 1) % parent.getAcInfo().flare.types.length;
        }
    }

    public boolean canUseFlare() {
        return parent.getAcInfo() != null && parent.getAcInfo().haveFlare() && !parent.getCommonStatus(0) && flareDv.tick == 0;
    }

    public boolean isFlarePreparation() {
        return flareDv.isInPreparation();
    }

    public boolean isFlareUsing() {
        return flareDv.isUsing();
    }

    public int getFlareTick() {
        return flareDv.tick;
    }

    public boolean haveFlare() {
        return parent.getAcInfo() != null && parent.getAcInfo().haveFlare();
    }

    public boolean supportsDetachedTurretAim() {
        return hasAnyIndependentMountedWeapon();
    }

    public void setDetachedWeaponAim(float yaw, float pitch) {
        setDetachedWeaponAim(detachedWeaponAimYaw, yaw, detachedWeaponAimPitch, pitch);
    }

    public void setDetachedWeaponAim(float prevYaw, float yaw, float prevPitch, float pitch) {
        detachedWeaponAimActive = true;
        prevDetachedWeaponAimYaw = prevYaw;
        detachedWeaponAimYaw = yaw;
        prevDetachedWeaponAimPitch = prevPitch;
        detachedWeaponAimPitch = pitch;
        parent.prevLastRiderYaw = prevYaw;
        parent.lastRiderYaw = yaw;
        parent.prevLastRiderPitch = prevPitch;
        parent.lastRiderPitch = pitch;
    }

    public void clearDetachedWeaponAim() {
        detachedWeaponAimActive = false;
    }

    public float getCurrentWeaponShotYaw(@Nullable Entity entity) {
        MCH_WeaponSet weaponSet = entity != null ? getCurrentWeapon(entity) : null;
        if (weaponSet == null || weaponSet.getCurrentWeapon() == null) {
            return parent.getWeaponUserYaw(entity);
        }
        float yaw = parent.getYaw();
        float weaponYaw = weaponSet.getYaw();
        float fixedYaw = weaponSet.getCurrentWeapon().fixRotationYaw;
        return MathHelper.wrapDegrees(yaw + weaponYaw + fixedYaw);
    }

    public float getCurrentWeaponShotYaw(@Nullable Entity entity, float partialTicks) {
        MCH_WeaponSet weaponSet = entity != null ? getCurrentWeapon(entity) : null;
        if (weaponSet == null || weaponSet.getCurrentWeapon() == null) {
            return parent.getWeaponUserYaw(entity, partialTicks);
        }

        float yaw = parent.prevRotationYaw + (parent.rotationYaw - parent.prevRotationYaw) * partialTicks;
        float weaponYaw = weaponSet.getPrevYaw() + (weaponSet.getYaw() - weaponSet.getPrevYaw()) * partialTicks;
        float fixedYaw = weaponSet.getCurrentWeapon().fixRotationYaw;
        return MathHelper.wrapDegrees(yaw + weaponYaw + fixedYaw);
    }

    public float getCurrentWeaponShotPitch(@Nullable Entity entity) {
        MCH_WeaponSet weaponSet = entity != null ? getCurrentWeapon(entity) : null;
        if (weaponSet == null || weaponSet.getCurrentWeapon() == null) {
            return parent.getWeaponUserPitch(entity);
        }
        return MathHelper.wrapDegrees(parent.getPitch() + weaponSet.getPitch() + weaponSet.getCurrentWeapon().fixRotationPitch);
    }

    public float getCurrentWeaponShotPitch(@Nullable Entity entity, float partialTicks) {
        MCH_WeaponSet weaponSet = entity != null ? getCurrentWeapon(entity) : null;
        if (weaponSet == null || weaponSet.getCurrentWeapon() == null) {
            return parent.getWeaponUserPitch(entity, partialTicks);
        }

        float pitch = parent.prevRotationPitch + (parent.rotationPitch - parent.prevRotationPitch) * partialTicks;
        float weaponPitch = weaponSet.getPrevPitch() + (weaponSet.getPitch() - weaponSet.getPrevPitch()) * partialTicks;
        float fixedPitch = weaponSet.getCurrentWeapon().fixRotationPitch;
        return MathHelper.wrapDegrees(pitch + weaponPitch + fixedPitch);
    }

    @Nullable
    public MCH_AircraftInfo.Weapon getCurrentMountedWeapon(@Nullable Entity entity) {
        if (parent.getAcInfo() == null) {
            return null;
        }
        int weaponId = getCurrentWeaponID(entity);
        if (weaponId < 0) {
            return null;
        }
        return parent.getAcInfo().getWeaponById(weaponId);
    }

    public boolean hasIndependentMountedAim(@Nullable Entity entity) {
        MCH_AircraftInfo.Weapon weapon = getCurrentMountedWeapon(entity);
        return weapon != null && hasIndependentMountedAim(weapon);
    }

    public boolean hasAnyIndependentMountedWeapon() {
        if (parent.getAcInfo() == null) {
            return false;
        }
        for (MCH_AircraftInfo.WeaponSet weaponSet : parent.getAcInfo().weaponSetList) {
            for (MCH_AircraftInfo.Weapon weapon : weaponSet.weapons) {
                if (hasIndependentMountedAim(weapon)) {
                    return true;
                }
            }
        }
        return false;
    }

    public float getCurrentWeaponRotationSpeed(@Nullable Entity entity) {
        return getWeaponRotationSpeed(entity != null ? getCurrentWeapon(entity) : null);
    }

    public Vec3d getCurrentWeaponShotPos(Vec3d localPos, @Nullable Entity user) {
        MCH_AircraftInfo.Weapon weapon = getCurrentMountedWeapon(user);
        if (weapon != null && weapon.turret && parent.getAcInfo() != null) {
            Vec3d turretPos = parent.getAcInfo().turretPosition;
            Vec3d relative = localPos.subtract(turretPos);
            Vec3d rotatedRelative = MCH_Lib.RotVec3(relative, parent.getYaw() - getCurrentWeaponShotYaw(user), 0.0F, 0.0F);
            return MCH_Lib.RotVec3(rotatedRelative.add(turretPos), -parent.getYaw(), -parent.getPitch(), -parent.getRoll());
        }
        return MCH_Lib.RotVec3(localPos, -getCurrentWeaponShotYaw(user), -getCurrentWeaponShotPitch(user), -parent.getRoll());
    }

    public Vec3d getCurrentWeaponShotPos(Vec3d localPos, @Nullable Entity user, float partialTicks) {
        MCH_AircraftInfo.Weapon weapon = getCurrentMountedWeapon(user);
        if (weapon != null && weapon.turret && parent.getAcInfo() != null) {
            float yaw = parent.prevRotationYaw + (parent.rotationYaw - parent.prevRotationYaw) * partialTicks;
            float pitch = parent.prevRotationPitch + (parent.rotationPitch - parent.prevRotationPitch) * partialTicks;
            float roll = parent.getPrevRotationRoll() + (parent.getRoll() - parent.getPrevRotationRoll()) * partialTicks;
            Vec3d turretPos = parent.getAcInfo().turretPosition;
            Vec3d relative = localPos.subtract(turretPos);
            Vec3d rotatedRelative = MCH_Lib.RotVec3(relative, yaw - getCurrentWeaponShotYaw(user, partialTicks), 0.0F, 0.0F);
            return MCH_Lib.RotVec3(rotatedRelative.add(turretPos), -yaw, -pitch, -roll);
        }
        float roll = parent.getPrevRotationRoll() + (parent.getRoll() - parent.getPrevRotationRoll()) * partialTicks;
        return MCH_Lib.RotVec3(localPos, -getCurrentWeaponShotYaw(user, partialTicks), -getCurrentWeaponShotPitch(user, partialTicks), -roll);
    }

    private float getWeaponRotationSpeed(@Nullable MCH_WeaponSet weaponSet) {
        if (parent.getAcInfo() == null) {
            return 0.0F;
        }
        float speed = Math.max(0.0F, parent.getAcInfo().cameraRotationSpeed);
        if (weaponSet != null && weaponSet.getInfo() != null) {
            speed *= Math.max(0.0F, weaponSet.getInfo().cameraRotationSpeedPitch);
        }
        return speed;
    }

    private float getWeaponRotationStep(@Nullable MCH_WeaponSet weaponSet) {
        return getWeaponRotationSpeed(weaponSet) / 20.0F;
    }

    public boolean isCameraView(Entity entity) {
        return getIsGunnerMode(entity) || parent.isUAV();
    }

    public void updateCamera(double x, double y, double z) {
        if (parent.world.isRemote) {
            if (getTVMissile() != null) {
                parent.camera.setPosition(tvMissile.posX, tvMissile.posY, tvMissile.posZ);
                parent.camera.setCameraZoom(1.0F);
                tvMissile.isSpawnParticle = !isMissileCameraMode(tvMissile.shootingEntity);
            } else {
                setTVMissile(null);
                MCH_AircraftInfo.CameraPosition cpi = parent.getCameraPosInfo();
                Vec3d cp = cpi != null ? cpi.pos() : Vec3d.ZERO;
                Vec3d v = MCH_Lib.RotVec3(cp, -parent.getYaw(), -parent.getPitch(), -parent.getRoll());
                parent.camera.setPosition(x + v.x, y + v.y, z + v.z);
            }
        }
    }

    @Nullable
    public MCH_EntityTvMissile getTVMissile() {
        return tvMissile != null && !tvMissile.isDead ? tvMissile : null;
    }

    public MCH_WeaponSet[] getWeapons() {
        return weapons;
    }

    public void setWeaponBays(MCH_EntityAircraft.WeaponBay[] weaponBays) {
        this.weaponBays = weaponBays;
    }

    public boolean isDetachedWeaponAimActive() {
        return detachedWeaponAimActive;
    }

    public float getDetachedWeaponAimYaw() {
        return detachedWeaponAimYaw;
    }

    public float getPrevDetachedWeaponAimYaw() {
        return prevDetachedWeaponAimYaw;
    }

    public float getDetachedWeaponAimPitch() {
        return detachedWeaponAimPitch;
    }

    public float getPrevDetachedWeaponAimPitch() {
        return prevDetachedWeaponAimPitch;
    }

    public boolean isGunnerMode() {
        return gunnerMode;
    }

    public void setGunnerModeOtherSeat(boolean gunnerModeOtherSeat) {
        this.gunnerModeOtherSeat = gunnerModeOtherSeat;
    }

    public void setTVMissile(MCH_EntityTvMissile entity) {
        tvMissile = entity;
    }

    public MCH_WeaponSet[] createWeapon(int seatNum) {
        currentWeaponID = new int[seatNum];
        Arrays.fill(currentWeaponID, -1);

        if (parent.getAcInfo() != null && !parent.getAcInfo().weaponSetList.isEmpty() && seatNum > 0) {
            MCH_WeaponSet[] weaponSetArray = new MCH_WeaponSet[parent.getAcInfo().weaponSetList.size()];

            for (int i = 0; i < parent.getAcInfo().weaponSetList.size(); i++) {
                MCH_AircraftInfo.WeaponSet ws = parent.getAcInfo().weaponSetList.get(i);
                MCH_WeaponBase[] wb = new MCH_WeaponBase[ws.weapons.size()];

                for (int j = 0; j < ws.weapons.size(); j++) {
                    wb[j] = MCH_WeaponCreator.createWeapon(parent.world, ws.type, ws.weapons.get(j).pos, ws.weapons.get(j).yaw, ws.weapons.get(j).pitch, parent, ws.weapons.get(j).turret);
                    assert wb[j] != null;
                    wb[j].aircraft = parent;
                }

                if (wb.length > 0 && wb[0] != null) {
                    float defYaw = ws.weapons.getFirst().defaultYaw;
                    weaponSetArray[i] = new MCH_WeaponSet(wb);
                    weaponSetArray[i].setPrevYaw(defYaw);
                    weaponSetArray[i].setYaw(defYaw);
                    weaponSetArray[i].setDefYaw(defYaw);
                }
            }

            return weaponSetArray;
        }

        return new MCH_WeaponSet[]{dummyWeapon};
    }

    public void switchWeapon(Entity entity, int id) {
        int sid = parent.getSeatIdByEntity(entity);
        if (parent.isValidSeatID(sid) && getWeaponNum() > 0 && currentWeaponID.length > 0) {
            if (id < 0) {
                currentWeaponID[sid] = -1;
            }

            if (id >= getWeaponNum()) {
                id = getWeaponNum() - 1;
            }

            MCH_Logger.debugLog(parent.world, "switchWeapon:" + W_Entity.getEntityId(entity) + " -> " + id);
            getCurrentWeapon(entity).reload();
            currentWeaponID[sid] = id;
            MCH_WeaponSet ws = getCurrentWeapon(entity);
            ws.onSwitchWeapon(parent.world.isRemote, parent.isInfinityAmmo(entity));
            if (!parent.world.isRemote) {
                new PacketSyncWeapon(MCH_EntityAircraft.getEntityId(parent), sid, id, (short) ws.getAmmo(), (short) ws.getRestAllAmmoNum()).sendPacketToAllAround(parent.world, parent.posX, parent.posY, parent.posZ, 150);
            }
        }
    }

    public void updateWeaponID(int sid, int id) {
        if (sid >= 0 && sid < currentWeaponID.length && getWeaponNum() > 0 && currentWeaponID.length > 0) {
            if (id < 0) {
                currentWeaponID[sid] = -1;
            }

            if (id >= getWeaponNum()) {
                id = getWeaponNum() - 1;
            }

            MCH_Logger.debugLog(parent.world, "switchWeapon:seatID=" + sid + ", WeaponID=" + id);
            currentWeaponID[sid] = id;
        }
    }

    @Nullable
    public MCH_WeaponSet getWeaponByName(String name) {
        for (MCH_WeaponSet ws : weapons) {
            if (ws.isName(name)) {
                return ws;
            }
        }

        return null;
    }

    public void reloadAllWeapon() {
        for (MCH_WeaponSet ws : weapons) {
            ws.reloadMag();
        }
    }

    public MCH_WeaponSet getFirstSeatWeapon() {
        return currentWeaponID.length > 0 && currentWeaponID[0] >= 0 ? getWeapon(currentWeaponID[0]) : getWeapon(0);
    }

    public void initCurrentWeapon(Entity entity) {
        int sid = parent.getSeatIdByEntity(entity);
        MCH_Logger.debugLog(parent.world, "initCurrentWeapon:" + W_Entity.getEntityId(entity) + ":%d", sid);
        if (sid >= 0 && sid < currentWeaponID.length) {
            currentWeaponID[sid] = -1;
            if (entity instanceof EntityPlayer || entity instanceof MCH_EntityGunner) {
                currentWeaponID[sid] = getNextWeaponID(entity, 1);
                switchWeapon(entity, getCurrentWeaponID(entity));
                if (parent.world.isRemote) {
                    PacketIndNotifyAmmoNum.send(parent, -1);
                }
            }
        }
    }

    public void initPilotWeapon() {
        currentWeaponID[0] = -1;
    }

    public MCH_WeaponSet getCurrentWeapon(Entity entity) {
        return getWeapon(getCurrentWeaponID(entity));
    }

    public MCH_WeaponSet getWeapon(int id) {
        return id >= 0 && weapons.length > 0 && id < weapons.length ? weapons[id] : dummyWeapon;
    }

    public int getWeaponIDBySeatID(int sid) {
        return sid >= 0 && sid < currentWeaponID.length ? currentWeaponID[sid] : -1;
    }

    public boolean prepareCurrentWeapon(Entity user) {
        MCH_WeaponParam prm = new MCH_WeaponParam();
        prm.setPosition(parent.posX, parent.posY, parent.posZ);
        prm.entity = parent;
        prm.user = user;
        prm.isInfinity = parent.isInfinityAmmo(prm.user);
        if (prm.user == null) {
            return false;
        }

        MCH_WeaponSet currentWs = getCurrentWeapon(prm.user);
        if (currentWs == null) {
            return false;
        }

        int sid = parent.getSeatIdByEntity(prm.user);
        if (parent.getAcInfo() != null && parent.getAcInfo().getWeaponSetById(sid) != null) {
            prm.isTurret = parent.getAcInfo().getWeaponSetById(sid).weapons.getFirst().turret;
        }

        return currentWs.prepareUse(prm);
    }

    public boolean useCurrentWeapon(Entity user) {
        MCH_WeaponParam prm = new MCH_WeaponParam();
        prm.setPosition(parent.posX, parent.posY, parent.posZ);
        prm.entity = parent;
        prm.user = user;
        return useCurrentWeapon(prm);
    }

    public boolean useCurrentWeapon(MCH_WeaponParam prm) {
        prm.isInfinity = parent.isInfinityAmmo(prm.user);
        if (prm.user == null) {
            return false;
        }

        MCH_WeaponSet currentWs = getCurrentWeapon(prm.user);
        if (currentWs == null || !currentWs.canFire()) {
            return false;
        }

        MCH_AircraftInfo acInfo = parent.getAcInfo();
        if (acInfo != null) {
            MCH_AircraftInfo.WeaponSet seatWeaponSet = acInfo.getWeaponSetById(parent.getSeatIdByEntity(prm.user));
            if (seatWeaponSet != null && !seatWeaponSet.weapons.isEmpty()) {
                prm.isTurret = seatWeaponSet.weapons.getFirst().turret;
            }
        }

        int lastUsedIndex = currentWs.getCurrentWeaponIndex();
        if (!currentWs.use(prm)) {
            return false;
        }

        String currentGroup = currentWs.getInfo().group;
        boolean hasGroup = currentGroup != null && !currentGroup.isEmpty();
        for (MCH_WeaponSet ws : weapons) {
            if (ws != currentWs && hasGroup && currentGroup.equals(ws.getInfo().group)) {
                ws.waitAndReloadByOther(prm.reload);
            }
        }

        if (!parent.world.isRemote) {
            int shift = 0;
            for (MCH_WeaponSet ws : weapons) {
                if (ws == currentWs) {
                    break;
                }
                shift += ws.getWeaponsCount();
            }
            shift += lastUsedIndex;
            if (shift < 32) {
                useWeaponStat |= 1 << shift;
            }
        }

        return true;
    }

    public void switchCurrentWeaponMode(Entity entity) {
        getCurrentWeapon(entity).switchMode();
    }

    public int getWeaponNum() {
        return weapons.length;
    }

    public int[] getCurrentWeaponIds() {
        return currentWeaponID;
    }

    public int getCurrentWeaponID(Entity entity) {
        if (!(entity instanceof EntityPlayer) && !(entity instanceof MCH_EntityGunner)) {
            return -1;
        }
        int id = parent.getSeatIdByEntity(entity);
        return id >= 0 && id < currentWeaponID.length ? currentWeaponID[id] : -1;
    }

    public int getNextWeaponID(Entity entity, int step) {
        if (parent.getAcInfo() == null) {
            return -1;
        }

        int sid = parent.getSeatIdByEntity(entity);
        if (sid < 0) {
            return -1;
        }

        int id = getCurrentWeaponID(entity);
        int attempts;
        for (attempts = 0; attempts < getWeaponNum(); attempts++) {
            if (step >= 0) {
                id = (id + 1) % getWeaponNum();
            } else {
                id = id > 0 ? id - 1 : getWeaponNum() - 1;
            }

            MCH_AircraftInfo.Weapon weapon = parent.getAcInfo().getWeaponById(id);
            if (weapon != null) {
                MCH_WeaponInfo weaponInfo = parent.getWeaponInfoById(id);
                int weaponSeatId = getWeaponSeatID(weaponInfo, weapon);
                if (weaponSeatId < parent.getSeatNum() + 2
                        && (weaponSeatId == sid
                        || sid == 0 && weapon.canUsePilot
                        && !(parent.getEntityBySeatId(weaponSeatId) instanceof EntityPlayer)
                        && !(parent.getEntityBySeatId(weaponSeatId) instanceof MCH_EntityGunner))) {
                    break;
                }
            }
        }

        if (attempts >= getWeaponNum()) {
            return -1;
        }

        MCH_Logger.debugLog(parent.world, "getNextWeaponID:%d:->%d", W_Entity.getEntityId(entity), id);
        return id;
    }

    public int getWeaponSeatID(MCH_WeaponInfo weaponInfo, MCH_AircraftInfo.Weapon weapon) {
        return weaponInfo == null || (weaponInfo.target & 195) != 0 || !weaponInfo.type.isEmpty()
                || !MCH_MOD.proxy.isSinglePlayer() && !MCH_Config.TestMode.prmBool ? weapon.seatID : 1000;
    }

    public boolean isMissileCameraMode(Entity entity) {
        return getTVMissile() != null && isCameraView(entity);
    }

    public int getUsedWeaponStat() {
        if (parent.getAcInfo() == null || parent.getAcInfo().getWeaponCount() <= 0) {
            return 0;
        }

        int stat = 0;
        int index = 0;
        for (MCH_WeaponSet weaponSet : weapons) {
            if (index >= 32) {
                break;
            }
            for (int weaponIndex = 0; weaponIndex < weaponSet.getWeaponsCount() && index < 32; weaponIndex++) {
                stat |= weaponSet.isUsed(weaponIndex) ? 1 << index : 0;
                index++;
            }
        }
        return stat;
    }

    public boolean isWeaponOnCooldown(MCH_WeaponSet checkWs, int index) {
        if (parent.getAcInfo() == null || parent.getAcInfo().getWeaponCount() <= 0) {
            return true;
        }

        int shift = 0;
        for (MCH_WeaponSet ws : weapons) {
            if (ws == checkWs) {
                break;
            }
            shift += ws.getWeaponsCount();
        }
        shift += index;
        return (useWeaponStat & 1 << shift) == 0;
    }

    public void updateWeapons() {
        if (parent.getAcInfo() == null || parent.getAcInfo().getWeaponCount() <= 0) {
            return;
        }

        int prevUseWeaponStat = useWeaponStat;
        if (!parent.world.isRemote) {
            useWeaponStat |= getUsedWeaponStat();
            parent.networkSync.setUseWeaponStat(useWeaponStat);
            useWeaponStat = 0;
        } else {
            useWeaponStat = parent.networkSync.getUseWeaponStat();
        }

        float yaw = MathHelper.wrapDegrees(parent.getYaw());
        float pitch = MathHelper.wrapDegrees(parent.getPitch());
        int id = 0;

        for (int wid = 0; wid < weapons.length; wid++) {
            MCH_WeaponSet weaponSet = weapons[wid];
            boolean isLongDelay = weaponSet.getFirstWeapon() != null && weaponSet.hasLongDelay();
            boolean isSelected = false;

            for (int selectedId : currentWeaponID) {
                if (selectedId == wid) {
                    isSelected = true;
                    break;
                }
            }

            boolean isWeaponUsed = false;
            for (int index = 0; index < weaponSet.getWeaponsCount(); index++) {
                boolean wasUsed = id < 32 && (prevUseWeaponStat & 1 << id) != 0;
                boolean isUsed = id < 32 && (useWeaponStat & 1 << id) != 0;
                if (isLongDelay && wasUsed && isUsed) {
                    isUsed = false;
                }

                isWeaponUsed |= isUsed;
                if (!wasUsed && isUsed) {
                    float recoil = weaponSet.getInfo().recoil;
                    if (recoil > 0.0F) {
                        recoilCount = 30;
                        recoilValue = recoil;
                        recoilYaw = weaponSet.getYaw();
                    }
                }

                if (parent.world.isRemote && isUsed) {
                    Vec3d weaponVector = MCH_Lib.RotVec3(0.0, 0.0, -1.0, -weaponSet.getYaw() - yaw, -weaponSet.getPitch());
                    Vec3d shotPos = weaponSet.getCurrentWeapon().getShotPos(parent);
                    spawnParticleMuzzleFlash(parent.world, weaponSet.getInfo(), parent.posX + shotPos.x, parent.posY + shotPos.y, parent.posZ + shotPos.z, weaponVector);
                }

                weaponSet.updateWeapon(parent, isUsed, index);
                id++;
            }

            weaponSet.update(parent, isSelected, isWeaponUsed);
            MCH_AircraftInfo.Weapon weaponInfo = parent.getAcInfo().getWeaponById(wid);
            if (weaponInfo != null && !parent.isDestroyed()) {
                Entity entity = parent.getEntityBySeatId(getWeaponSeatID(parent.getWeaponInfoById(wid), weaponInfo));
                if (weaponInfo.canUsePilot && !(entity instanceof EntityPlayer) && !(entity instanceof MCH_EntityGunner)) {
                    entity = parent.getEntityBySeatId(0);
                }

                float weaponTurnStep = getWeaponRotationStep(weaponSet);
                float entityPitch = parent.getWeaponUserPitch(entity);
                if (!(entity instanceof EntityPlayer) && !(entity instanceof MCH_EntityGunner)) {
                    weaponSet.setTurretYaw(parent.seatManager.getLastRiderYaw() - parent.getYaw());
                    if (parent.getTowedChainEntity() != null || parent.getRidingEntity() != null) {
                        weaponSet.setYaw(0.0F);
                    }
                } else {
                    if ((int) weaponInfo.minYaw != 0 || (int) weaponInfo.maxYaw != 0) {
                        float entityYaw = parent.getWeaponUserYaw(entity);
                        float turretYaw = weaponInfo.turret ? MathHelper.wrapDegrees(parent.seatManager.getLastRiderYaw()) - yaw : 0.0F;
                        float entityWeaponYaw = MathHelper.wrapDegrees(entityYaw - yaw - weaponInfo.defaultYaw - turretYaw);
                        if (Math.abs((int) weaponInfo.minYaw) < 360 && Math.abs((int) weaponInfo.maxYaw) < 360) {
                            float targetYaw = MCH_Lib.RNG(entityWeaponYaw, weaponInfo.minYaw, weaponInfo.maxYaw);
                            float weaponYaw = weaponSet.getYaw() - weaponInfo.defaultYaw - turretYaw;
                            weaponYaw = stepWrappedAngle(weaponYaw, targetYaw, weaponTurnStep);
                            weaponSet.setYaw(weaponYaw + weaponInfo.defaultYaw + turretYaw);
                        } else {
                            weaponSet.setYaw(stepWrappedAngle(weaponSet.getYaw(), entityWeaponYaw + turretYaw, weaponTurnStep));
                        }
                    }

                    float entityPitchDelta = MathHelper.wrapDegrees(entityPitch - pitch);
                    float targetPitch = MCH_Lib.RNG(entityPitchDelta, weaponInfo.minPitch, weaponInfo.maxPitch);
                    weaponSet.setPitch(stepLinearAngle(weaponSet.getPitch(), targetPitch, weaponTurnStep));
                    weaponSet.setTurretYaw(0.0F);
                }
            }
        }

        updateWeaponBay();
        if (hitStatus > 0) {
            hitStatus--;
        }
    }

    public void updateWeaponsRotation() {
        if (parent.getAcInfo() == null || parent.getAcInfo().getWeaponCount() <= 0 || parent.isDestroyed()) {
            return;
        }

        float yaw = MathHelper.wrapDegrees(parent.getYaw());
        float pitch = MathHelper.wrapDegrees(parent.getPitch());

        for (int wid = 0; wid < weapons.length; wid++) {
            MCH_WeaponSet weaponSet = weapons[wid];
            MCH_AircraftInfo.Weapon weaponInfo = parent.getAcInfo().getWeaponById(wid);

            if (weaponInfo == null) {
                weaponSet.setPrevYaw(weaponSet.getYaw());
                continue;
            }

            Entity entity = parent.getEntityBySeatId(getWeaponSeatID(parent.getWeaponInfoById(wid), weaponInfo));
            boolean isHuman = entity instanceof EntityPlayer || entity instanceof MCH_EntityGunner;

            if (weaponInfo.canUsePilot && !isHuman) {
                entity = parent.getEntityBySeatId(0);
                isHuman = entity instanceof EntityPlayer || entity instanceof MCH_EntityGunner;
            }

            if (!isHuman) {
                weaponSet.setTurretYaw(parent.seatManager.getLastRiderYaw() - parent.getYaw());
            } else {
                float weaponTurnStep = getWeaponRotationStep(weaponSet);
                if ((int) weaponInfo.minYaw != 0 || (int) weaponInfo.maxYaw != 0) {
                    float entityYaw = parent.getWeaponUserYaw(entity);
                    float turretYaw = weaponInfo.turret ? MathHelper.wrapDegrees(parent.seatManager.getLastRiderYaw()) - yaw : 0.0F;
                    float entityWeaponYaw = MathHelper.wrapDegrees(entityYaw - yaw - weaponInfo.defaultYaw - turretYaw);

                    if (Math.abs((int) weaponInfo.minYaw) < 360 && Math.abs((int) weaponInfo.maxYaw) < 360) {
                        float targetYaw = Math.clamp(entityWeaponYaw, weaponInfo.minYaw, weaponInfo.maxYaw);
                        float weaponYaw = weaponSet.getYaw() - weaponInfo.defaultYaw - turretYaw;
                        weaponYaw = stepWrappedAngle(weaponYaw, targetYaw, weaponTurnStep);
                        weaponSet.setYaw(weaponYaw + weaponInfo.defaultYaw + turretYaw);
                    } else {
                        weaponSet.setYaw(stepWrappedAngle(weaponSet.getYaw(), entityWeaponYaw + turretYaw, weaponTurnStep));
                    }
                }

                float entityPitch = parent.getWeaponUserPitch(entity);
                float entityPitchDelta = MathHelper.wrapDegrees(entityPitch - pitch);
                float targetPitch = Math.clamp(entityPitchDelta, weaponInfo.minPitch, weaponInfo.maxPitch);
                weaponSet.setPitch(stepLinearAngle(weaponSet.getPitch(), targetPitch, weaponTurnStep));
                weaponSet.setTurretYaw(0.0F);
            }

            weaponSet.setPrevYaw(weaponSet.getYaw());
        }
    }

    private void spawnParticleMuzzleFlash(World world, MCH_WeaponInfo weaponInfo, double px, double py, double pz, Vec3d weaponVector) {
        if (weaponInfo.listMuzzleFlashSmoke != null) {
            for (MCH_WeaponInfo.MuzzleFlash muzzleFlash : weaponInfo.listMuzzleFlashSmoke) {
                double x = px - weaponVector.x * muzzleFlash.dist;
                double y = py - weaponVector.y * muzzleFlash.dist;
                double z = pz - weaponVector.z * muzzleFlash.dist;
                MCH_ParticleParam particle = new MCH_ParticleParam(world, "smoke", px, py, pz);
                particle.size = muzzleFlash.size;

                for (int i = 0; i < muzzleFlash.num; i++) {
                    particle.a = muzzleFlash.a * 0.9F + world.rand.nextFloat() * 0.1F;
                    float color = world.rand.nextFloat() * 0.1F;
                    particle.r = color + muzzleFlash.r * 0.9F;
                    particle.g = color + muzzleFlash.g * 0.9F;
                    particle.b = color + muzzleFlash.b * 0.9F;
                    particle.age = (int) (muzzleFlash.age + 0.1 * muzzleFlash.age * world.rand.nextFloat());
                    particle.posX = x + (world.rand.nextDouble() - 0.5) * muzzleFlash.range;
                    particle.posY = y + (world.rand.nextDouble() - 0.5) * muzzleFlash.range;
                    particle.posZ = z + (world.rand.nextDouble() - 0.5) * muzzleFlash.range;
                    particle.motionX = world.rand.nextDouble() * (particle.posX < x ? -0.2 : 0.2);
                    particle.motionY = world.rand.nextDouble() * (particle.posY < y ? -0.03 : 0.03);
                    particle.motionZ = world.rand.nextDouble() * (particle.posZ < z ? -0.2 : 0.2);
                    MCH_ParticlesUtil.spawnParticle(particle);
                }
            }
        }

        if (weaponInfo.listMuzzleFlash != null) {
            for (MCH_WeaponInfo.MuzzleFlash muzzleFlash : weaponInfo.listMuzzleFlash) {
                float color = world.rand.nextFloat() * 0.1F + 0.9F;
                MCH_ParticlesUtil.spawnParticleMuzzleFlash(parent.world, px - weaponVector.x * muzzleFlash.dist, py - weaponVector.y * muzzleFlash.dist, pz - weaponVector.z * muzzleFlash.dist, muzzleFlash.size, color * muzzleFlash.r, color * muzzleFlash.g, color * muzzleFlash.b, muzzleFlash.a, muzzleFlash.age + world.rand.nextInt(3));
            }
        }
    }

    private void updateWeaponBay() {
        for (int i = 0; i < weaponBays.length; i++) {
            MCH_EntityAircraft.WeaponBay weaponBay = weaponBays[i];
            assert parent.getAcInfo() != null;
            MCH_AircraftInfo.WeaponBay info = parent.getAcInfo().partWeaponBay.get(i);
            boolean isSelected = false;

            for (int weaponId : info.weaponIds) {
                for (int sid = 0; sid < currentWeaponID.length; sid++) {
                    if (weaponId == currentWeaponID[sid] && parent.getEntityBySeatId(sid) != null) {
                        isSelected = true;
                    }
                }
            }

            weaponBay.prevRot = weaponBay.rot;
            if (isSelected) {
                if (weaponBay.rot < 90.0F) {
                    weaponBay.rot += 3.0F;
                }
                if (weaponBay.rot >= 90.0F) {
                    weaponBay.rot = 90.0F;
                }
            } else {
                if (weaponBay.rot > 0.0F) {
                    weaponBay.rot -= 3.0F;
                }
                if (weaponBay.rot <= 0.0F) {
                    weaponBay.rot = 0.0F;
                }
            }
        }
    }

    public void updateRecoil(float partialTicks) {
        if (recoilCount > 0 && recoilCount >= 12) {
            float pitch = MathHelper.cos((float) ((recoilYaw - parent.getRoll()) * Math.PI / 180.0));
            float roll = MathHelper.sin((float) ((recoilYaw - parent.getRoll()) * Math.PI / 180.0));
            float recoil = MathHelper.cos((float) (recoilCount * 6 * Math.PI / 180.0)) * recoilValue;
            parent.setRotPitch(parent.getPitch() + recoil * pitch * partialTicks);
            parent.setRotRoll(parent.getRoll() + recoil * roll * partialTicks);
        }
    }

    public int getMaxHitStatus() {
        return 15;
    }

    public void hitBullet() {
        hitStatus = getMaxHitStatus();
    }

    public MCH_EntityAircraft.WeaponBay[] createWeaponBays() {
        assert parent.getAcInfo() != null;
        MCH_EntityAircraft.WeaponBay[] bays = new MCH_EntityAircraft.WeaponBay[parent.getAcInfo().partWeaponBay.size()];
        for (int i = 0; i < bays.length; i++) {
            bays[i] = new MCH_EntityAircraft.WeaponBay();
        }
        return bays;
    }

    public void switchGunnerMode(boolean mode) {
        boolean previousMode = gunnerMode;
        Entity pilot = parent.getEntityBySeatId(0);
        if (!mode || canSwitchGunnerMode()) {
            if (gunnerMode && !mode) {
                parent.setCurrentThrottle(parent.getBeforeHoverThrottle());
                gunnerMode = false;
                parent.camera.setCameraZoom(1.0F);
                getCurrentWeapon(pilot).onSwitchWeapon(parent.world.isRemote, parent.isInfinityAmmo(pilot));
            } else if (!gunnerMode && mode) {
                parent.setBeforeHoverThrottle(parent.getCurrentThrottle());
                gunnerMode = true;
                parent.camera.setCameraZoom(1.0F);
                getCurrentWeapon(pilot).onSwitchWeapon(parent.world.isRemote, parent.isInfinityAmmo(pilot));
            }
        }

        MCH_Logger.debugLog(parent.world, "switchGunnerMode %s->%s", previousMode ? "ON" : "OFF", mode ? "ON" : "OFF");
    }

    public boolean canSwitchGunnerMode() {
        if (parent.getAcInfo() == null || !parent.getAcInfo().isEnableGunnerMode) {
            return false;
        }
        if (!parent.isCanopyClose()) {
            return false;
        }

        boolean seatOccupied = parent.getEntityBySeatId(1) instanceof EntityPlayer;
        boolean concurrentAllowed = parent.getAcInfo().isEnableConcurrentGunnerMode;
        return (concurrentAllowed || !seatOccupied) && !parent.isHoveringMode();
    }

    public boolean canSwitchGunnerModeOtherSeat(EntityPlayer player) {
        int sid = parent.getSeatIdByEntity(player);
        if (sid > 0) {
            MCH_SeatInfo info = parent.getSeatInfo(sid);
            return info != null && info.gunner && info.switchgunner;
        }
        return false;
    }

    public void switchGunnerModeOtherSeat(EntityPlayer player) {
        gunnerModeOtherSeat = !gunnerModeOtherSeat;
    }

    public boolean getIsGunnerMode(Entity entity) {
        if (parent.getAcInfo() == null) {
            return false;
        }

        int id = parent.getSeatIdByEntity(entity);
        if (id < 0) {
            return false;
        }
        if (id == 0 && parent.getAcInfo().isEnableGunnerMode) {
            return gunnerMode;
        }

        MCH_SeatInfo seatInfo = parent.getSeatInfo(id);
        if (seatInfo == null || !seatInfo.gunner) {
            return false;
        }
        return !parent.world.isRemote || !seatInfo.switchgunner || gunnerModeOtherSeat;
    }

    public boolean isPilot(Entity player) {
        return W_Entity.isEqual(parent.getRiddenByEntity(), player);
    }

    public boolean canSwitchGunnerFreeLook(EntityPlayer player) {
        MCH_SeatInfo seatInfo = parent.getSeatInfo(player);
        return seatInfo != null && seatInfo.fixRot && getIsGunnerMode(player);
    }

    public boolean isGunnerLookMode(EntityPlayer player) {
        return isPilot(player) || !gunnerFreeLookMode;
    }

    public void switchGunnerFreeLookMode(boolean enabled) {
        gunnerFreeLookMode = enabled;
    }

    public void switchGunnerFreeLookMode() {
        switchGunnerFreeLookMode(!gunnerFreeLookMode);
    }

    public boolean canUseChaff() {
        return parent.getAcInfo() != null && parent.getAcInfo().haveChaff() && chaff.tick == 0;
    }

    public boolean useChaff() {
        return parent.getAcInfo() != null && parent.getAcInfo().haveChaff() && chaff.onUse();
    }

    public boolean isChaffUsing() {
        return chaff != null && chaff.isUsing();
    }

    public boolean useAPS() {
        return parent.getAcInfo() != null && parent.getAcInfo().haveAPS() && apsDv.onUse();
    }

    public boolean canUseAPS() {
        return parent.getAcInfo() != null && parent.getAcInfo().haveAPS() && apsDv.tick == 0;
    }

    public boolean haveAPS() {
        return parent.getAcInfo() != null && parent.getAcInfo().haveAPS();
    }

    public boolean isECMJammerUsing() {
        return parent.getCommonStatus(NetworkSyncComponent.CMN_ID_ECM_JAMMER_ENABLED);
    }

    public void setECMJammerUsing(boolean enabled) {
        parent.setCommonStatus(NetworkSyncComponent.CMN_ID_ECM_JAMMER_ENABLED, enabled, true);
    }

    public boolean useECMJammer() {
        if (parent.getAcInfo() != null && parent.getAcInfo().enableECMJammer && ecmJammerWaitTick <= 0) {
            ecmJammerRunningTick = parent.getAcInfo().ecmJammerUseTime;
            ecmJammerWaitTick = parent.getAcInfo().ecmJammerWaitTime;
            if (!parent.world.isRemote) {
                setECMJammerUsing(true);
            }
            return true;
        }
        return false;
    }

    public boolean isIronCurtainActive() {
        return ironCurtainRunningTick > 0;
    }

    public void startIronCurtain(int tickCount) {
        ironCurtainRunningTick = tickCount;
    }

    public void resetIronCurtainVisuals() {
        ironCurtainRunningTick = 0;
        ironCurtainWaveTimer = 0;
        ironCurtainCurrentFactor = 0.5f;
        ironCurtainLastFactor = 0.5f;
    }

    private void updateDefensiveSystems() {
        if (ironCurtainRunningTick > 0) {
            ironCurtainRunningTick--;
            ironCurtainWaveTimer++;
            ironCurtainLastFactor = ironCurtainCurrentFactor;
            double waveSpeed = 0.15;
            ironCurtainCurrentFactor = 0.75f + 0.25f * (float) Math.sin(ironCurtainWaveTimer * waveSpeed);
        } else {
            ironCurtainWaveTimer = 0;
            ironCurtainCurrentFactor = 0.5f;
            ironCurtainLastFactor = 0.5f;
        }

        if (ecmJammerRunningTick > 0) {
            ecmJammerRunningTick--;
            if (ecmJammerRunningTick <= 0 && !parent.world.isRemote) {
                setECMJammerUsing(false);
            }
        }
        if (ecmJammerWaitTick > 0) {
            ecmJammerWaitTick--;
        }
        if (jammingTick > 0) {
            jammingTick--;
        }
    }
}
