package com.norwood.mcheli.weapon;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import com.norwood.mcheli.wrapper.W_Entity;

public class MCH_WeaponAAMissile extends MCH_WeaponEntitySeeker {

    public MCH_WeaponAAMissile(World w, Vec3d v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
        super(w, v, yaw, pitch, nm, wi);
        this.power = 12;
        this.acceleration = 2.5F;
        this.explosionPower = 4;
        this.interval = 5;
        if (w.isRemote) {
            this.interval += 5;
        }

        this.guidanceSystem.canLockInAir = true;
        this.guidanceSystem.ridableOnly = wi.ridableOnly;
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
        boolean result = false;
        if (!this.worldObj.isRemote) {
            Entity tgtEnt = prm.user.world.getEntityByID(prm.option1);
            if (tgtEnt != null && !tgtEnt.isDead) {
                this.playSound(prm.entity);
                float yaw = prm.entity.rotationYaw + this.fixRotationYaw;
                float pitch = prm.entity.rotationPitch + this.fixRotationPitch;
                double tX = -MathHelper.sin(yaw / 180.0F * (float) Math.PI) *
                        MathHelper.cos(pitch / 180.0F * (float) Math.PI);
                double tZ = MathHelper.cos(yaw / 180.0F * (float) Math.PI) *
                        MathHelper.cos(pitch / 180.0F * (float) Math.PI);
                double tY = -MathHelper.sin(pitch / 180.0F * (float) Math.PI);
                MCH_EntityAAMissile e = new MCH_EntityAAMissile(this.worldObj, prm.posX, prm.posY, prm.posZ, tX, tY, tZ,
                        yaw, pitch, this.acceleration);
                e.setName(this.name);
                e.setParameterFromWeapon(this, prm.entity, prm.user);
                e.setTargetEntity(tgtEnt);
                this.worldObj.spawnEntity(e);
                result = true;
            }
        } else if (this.guidanceSystem.lock(prm.user) && this.guidanceSystem.lastLockEntity != null) {
            result = true;
            this.optionParameter1 = W_Entity.getEntityId(this.guidanceSystem.lastLockEntity);
        }

        return result;
    }
}
