package com.norwood.mcheli.weapon;

import net.minecraft.entity.Entity;

public abstract class MCH_EntityGuidanceSystem implements MCH_IGuidanceSystem {
    public int lockCount;
    public int lockSoundCount;
    public int continueLockCount;
    public int lockCountMax;
    public int prevLockCount;
    public boolean canLockInWater;
    public boolean canLockOnGround;
    public boolean canLockInAir;
    public boolean ridableOnly;
    public double lockRange;
    public int lockAngle;
    public MCH_IEntityLockChecker checker;


    /** Infrared missile; affected by flares */
    public boolean isHeatSeekerMissile = true;
    /** Radar missile; affected by chaff */
    public boolean isRadarMissile = false;
    /** Semi-active radar; requires continuous guidance */
    public boolean passiveRadar = false;
    /** Lock-out timer when semi-active guidance is lost */
    public int passiveRadarLockOutCount = 20;
    /** Max angle for Pulse Doppler/Rear-aspect lock */
    public float pdHDNMaxDegree = 1000f;
    /** Lock-out delay after exceeding max PD angle */
    public int pdHDNMaxDegreeLockOutCount = 10;
    /** Jamming resistance duration; -1 to disable */
    public int antiFlareCount = -1;
    /** Multipath height; radar lock breaks below this value */
    public int lockMinHeight = 12;
    /** Allows locking onto missile entities */
    public boolean canLockMissile = false;


    public boolean canLockEntity(Entity entity) {
        return false;
    }

    public boolean isLockingEntity(Entity entity) {
        return false;
    }

    protected abstract void setLockCountMax(int i);

    protected abstract boolean lock(Entity user);

    protected abstract int getLockCount();

    protected abstract int getLockCountMax();

    protected abstract Entity getLastLockEntity();
}
