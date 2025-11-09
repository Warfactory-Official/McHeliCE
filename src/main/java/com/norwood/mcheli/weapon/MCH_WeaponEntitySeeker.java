package com.norwood.mcheli.weapon;

import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public abstract class MCH_WeaponEntitySeeker extends MCH_WeaponBase {

    public MCH_IEntityLockChecker entityLockChecker;
    public final MCH_WeaponGuidanceSystem guidanceSystem;

    public MCH_WeaponEntitySeeker(World w, Vec3d v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
        super(w, v, yaw, pitch, nm, wi);
        this.guidanceSystem = new MCH_WeaponGuidanceSystem(w);
        this.guidanceSystem.lockRange = 200.0;
        this.guidanceSystem.lockAngle = 5;
        this.guidanceSystem.setLockCountMax(25);
    }

    @Override
    public MCH_WeaponGuidanceSystem getGuidanceSystem() {
        return this.guidanceSystem;
    }

    @Override
    public int getLockCount() {
        return this.guidanceSystem.getLockCount();
    }

    @Override
    public int getLockCountMax() {
        return this.guidanceSystem.getLockCountMax();
    }

    @Override
    public void setLockCountMax(int n) {
        this.guidanceSystem.setLockCountMax(n);
    }

    @Override
    public void setLockChecker(MCH_IEntityLockChecker checker) {
        this.guidanceSystem.checker = checker;
    }

    @Override
    public void update(int countWait) {
        super.update(countWait);
        this.guidanceSystem.update();
    }
}
