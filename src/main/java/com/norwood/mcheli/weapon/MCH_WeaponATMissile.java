package com.norwood.mcheli.weapon;

import com.norwood.mcheli.wrapper.W_Entity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class MCH_WeaponATMissile extends MCH_WeaponEntitySeeker {

    public MCH_WeaponATMissile(World w, Vec3d v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
        super(w, v, yaw, pitch, nm, wi);
        this.power = 32;
        this.acceleration = 2.0F;
        this.explosionPower = 4;
        this.interval = -100;
        if (w.isRemote) {
            this.interval -= 10;
        }

        this.numMode = 2;
        this.guidanceSystem.canLockOnGround = true;
        this.guidanceSystem.ridableOnly = wi.ridableOnly;
    }

    @Override
    public boolean isCooldownCountReloadTime() {
        return true;
    }

    @Override
    public String getName() {
        String opt = "";
        if (this.getCurrentMode() == 1) {
            opt = " [TA]";
        }

        return super.getName() + opt;
    }

    @Override
    public void update(int countWait) {
        super.update(countWait);
    }

    @Override
    public boolean shot(MCH_WeaponParam prm) {
        return this.worldObj.isRemote ? this.shotClient(prm.entity, prm.user) : this.shotServer(prm);
    }

    protected boolean shotClient(Entity entity, Entity user) {
        boolean result = false;
        if (this.guidanceSystem.lock(user) && this.guidanceSystem.lastLockEntity != null) {
            result = true;
            this.optionParameter1 = W_Entity.getEntityId(this.guidanceSystem.lastLockEntity);
        }

        this.optionParameter2 = this.getCurrentMode();
        return result;
    }

    protected boolean shotServer(MCH_WeaponParam prm) {
        Entity tgtEnt;
        tgtEnt = prm.user.world.getEntityByID(prm.option1);
        if (tgtEnt != null && !tgtEnt.isDead) {
            float yaw = prm.user.rotationYaw + this.fixRotationYaw;
            float pitch = prm.entity.rotationPitch + this.fixRotationPitch;
            double tX = -MathHelper.sin(yaw / 180.0F * (float) Math.PI) *
                    MathHelper.cos(pitch / 180.0F * (float) Math.PI);
            double tZ = MathHelper.cos(yaw / 180.0F * (float) Math.PI) *
                    MathHelper.cos(pitch / 180.0F * (float) Math.PI);
            double tY = -MathHelper.sin(pitch / 180.0F * (float) Math.PI);
            MCH_EntityATMissile e = new MCH_EntityATMissile(this.worldObj, prm.posX, prm.posY, prm.posZ, tX, tY, tZ,
                    yaw, pitch, this.acceleration);
            e.setName(this.name);
            e.setParameterFromWeapon(this, prm.entity, prm.user);
            e.setTargetEntity(tgtEnt);
            e.guidanceType = prm.option2;
            this.worldObj.spawnEntity(e);
            this.playSound(prm.entity);
            return true;
        } else {
            return false;
        }
    }
}
