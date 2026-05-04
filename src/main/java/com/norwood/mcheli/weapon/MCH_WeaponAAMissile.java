package com.norwood.mcheli.weapon;

import com.norwood.mcheli.tank.MCH_EntityTank;
import com.norwood.mcheli.wrapper.W_Entity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

public class MCH_WeaponAAMissile extends MCH_WeaponEntitySeeker {

    public MCH_WeaponAAMissile(World world, Vec3d v, float yaw, float pitch, String name, MCH_WeaponInfo info) {
        super(world, v, yaw, pitch, name, info);
        this.power = 12;
        this.acceleration = 2.5F;
        this.explosionPower = 4;
        this.interval = world.isRemote ? 10 : 5;

        this.guidanceSystem.canLockInAir = true;
        this.guidanceSystem.ridableOnly = info.ridableOnly;
    }

    @Override
    public boolean isCooldownCountReloadTime() {
        return true;
    }

    @Override
    public void update(int countWait) {
        super.update(countWait);
    }

    @Override
    public boolean shot(MCH_WeaponParam prm) {
        if (this.world.isRemote) {
            return handleClientShot(prm);
        } else {
            return handleServerShot(prm);
        }
    }

    private boolean handleServerShot(MCH_WeaponParam prm) {
        final var info = getInfo();
        boolean isRadar = info.passiveRadar || info.activeRadar;
        Entity target = isRadar ? null : this.world.getEntityByID(prm.option1);

        if (!isRadar && (target == null || target.isDead)) return false;

        this.playSound(prm.entity);

        Vec2f rot = calculateShotRotation(prm, isRadar, !isRadar);
        Vec3d motion = Vec3d.fromPitchYaw(rot.y, rot.x);

        MCH_EntityAAMissile missile = new MCH_EntityAAMissile(this.world, prm.posX, prm.posY, prm.posZ,
                motion.x, motion.y, motion.z, rot.x, rot.y, this.acceleration);

        missile.setName(this.name);
        missile.setParameterFromWeapon(this, prm.entity, prm.user);
        if (target != null) missile.setTargetEntity(target);

        this.world.spawnEntity(missile);
        return true;
    }

    private boolean handleClientShot(MCH_WeaponParam prm) {
        final var info = getInfo();
        if (info.passiveRadar || info.activeRadar) {
            return true;
        } else if (this.guidanceSystem.lock(prm.user) && this.guidanceSystem.lastLockEntity != null) {
            this.optionParameter1 = W_Entity.getEntityId(this.guidanceSystem.lastLockEntity);
            return true;
        }
        return false;
    }

    @Override
    public boolean lock(MCH_WeaponParam prm) {
        if (!this.world.isRemote) return false;

        final var info = getInfo();
        if (info != null && info.passiveRadar) {
            this.guidanceSystem.lock(prm.user);

            if (this.guidanceSystem.isLockComplete()) {
                updateActiveBullets(prm.user, this.guidanceSystem.lastLockEntity);
            } else {
                updateActiveBullets(prm.user, null);
            }
        }
        return false;
    }

    @Override
    public void onUnlock(MCH_WeaponParam prm) {
        if (!this.world.isRemote || this.guidanceSystem == null || prm.user == null) return;

        if (!this.guidanceSystem.isLockComplete()) {
            updateActiveBullets(prm.user, null);
        }
    }


    private void updateActiveBullets(Entity user, @Nullable Entity target) {
        double range = getInfo().maxLockOnRange;
        int targetId = (target != null) ? W_Entity.getEntityId(target) : 0;

        for (MCH_EntityBaseBullet bullet : getShootBullets(this.world, user, range)) {
            bullet.clientSetTargetEntity(target);
            this.optionParameter1 = targetId;
        }
    }


}

