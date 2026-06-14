package com.norwood.mcheli.weapon;

import com.norwood.mcheli.tank.MCH_EntityTank;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.util.math.Vec2f;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class MCH_WeaponEntitySeeker extends MCH_WeaponBase {

    public final MCH_WeaponGuidanceSystem guidanceSystem;

    public MCH_WeaponEntitySeeker(World w, Vec3d v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
        super(w, v, yaw, pitch, nm, wi);
        this.guidanceSystem = new MCH_WeaponGuidanceSystem(w);
        this.guidanceSystem.lockRange = wi.maxLockOnRange;
        this.guidanceSystem.lockAngle = wi.maxLockOnAngle;
        this.guidanceSystem.antiFlareCount = wi.antiFlareCount;
        this.guidanceSystem.pdHDNMaxDegree = wi.pdHDNMaxDegree;
        this.guidanceSystem.pdHDNMaxDegreeLockOutCount = wi.pdHDNMaxDegreeLockOutCount;
        this.guidanceSystem.lockMinHeight = wi.lockMinHeight;
        this.guidanceSystem.setLockCountMax(25);
        this.guidanceSystem.isHeatSeekerMissile = wi.isHeatSeekerMissile;
        this.guidanceSystem.isRadarMissile = wi.isRadarMissile;
        this.guidanceSystem.passiveRadar = wi.passiveRadar;
        this.guidanceSystem.passiveRadarLockOutCount = wi.passiveRadarLockOutCount;
        this.guidanceSystem.canLockMissile = wi.canLockMissile;
        this.guidanceSystem.canLockInWater = wi.canLockInWater; // Reforged: ship/water target lock-on
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



    public static List<MCH_EntityBaseBullet> getShootBullets(World world, Entity user, double range) {
        List<MCH_EntityBaseBullet> bullets = world.getEntitiesWithinAABB(MCH_EntityBaseBullet.class,
                user.getEntityBoundingBox().grow(range));

        if (bullets.isEmpty()) {
            return Collections.emptyList();
        }

        List<MCH_EntityBaseBullet> result = new ArrayList<>();

        for (MCH_EntityBaseBullet bullet : bullets) {
            if (bullet.isDead) continue;

            final var info = bullet.getInfo();
            if (info != null && info.passiveRadar) {
                result.add(bullet);
            }
        }

        return result;
    }
}
