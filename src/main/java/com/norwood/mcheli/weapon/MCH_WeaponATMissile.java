package com.norwood.mcheli.weapon;

import com.norwood.mcheli.wrapper.W_Entity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class MCH_WeaponATMissile extends MCH_WeaponEntitySeeker {

    public MCH_WeaponATMissile(World world, Vec3d v, float yaw, float pitch, String name, MCH_WeaponInfo info) {
        super(world, v, yaw, pitch, name, info);
        this.power = 32;
        this.acceleration = 2.0F;
        this.explosionPower = 4;
        this.interval = world.isRemote ? 10 : 5;
        this.numMode = 2;

        this.guidanceSystem.canLockOnGround = true;
        // Reforged: anti-surface missiles engage watercraft (ships sit in water) by default.
        this.guidanceSystem.canLockInWater = true;
        this.guidanceSystem.ridableOnly = info.ridableOnly;
    }

    @Override
    public boolean isCooldownCountReloadTime() {
        return true;
    }

    @Override
    public String getName() {
        return super.getName() + (this.getCurrentMode() == 1 ? " [TA]" : "");
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

        MCH_EntityATMissile missile = new MCH_EntityATMissile(this.world, prm.posX, prm.posY, prm.posZ, motion.x, motion.y, motion.z, rot.x, rot.y, this.acceleration);

        missile.setName(this.name);
        missile.setParameterFromWeapon(this, prm.entity, prm.user);
        missile.guidanceType = prm.option2;

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
            this.optionParameter2 = this.getCurrentMode();
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

