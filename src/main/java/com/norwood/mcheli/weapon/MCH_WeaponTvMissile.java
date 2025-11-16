package com.norwood.mcheli.weapon;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.networking.packet.PacketNotifyTVMissileEntity;
import com.norwood.mcheli.wrapper.W_Entity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class MCH_WeaponTvMissile extends MCH_WeaponBase {

    protected MCH_EntityTvMissile lastShotTvMissile;
    protected Entity lastShotEntity;
    protected boolean isTVGuided;

    public MCH_WeaponTvMissile(World w, Vec3d v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
        super(w, v, yaw, pitch, nm, wi);
        this.power = 32;
        this.acceleration = 2.0F;
        this.explosionPower = 4;
        this.interval = -100;
        if (w.isRemote) {
            this.interval -= 10;
        }

        this.numMode = 2;
        this.lastShotEntity = null;
        this.lastShotTvMissile = null;
        this.isTVGuided = false;
    }

    @Override
    public String getName() {
        String opt = "";
        if (this.getCurrentMode() == 0) {
            opt = " [TV]";
        }

        if (this.getCurrentMode() == 2) {
            opt = " [TA]";
        }

        return super.getName() + opt;
    }

    @Override
    public void update(int countWait) {
        super.update(countWait);
        if (!this.worldObj.isRemote) {
            if (this.isTVGuided && this.tick <= 9) {
                if (this.tick % 3 == 0 && this.lastShotTvMissile != null && !this.lastShotTvMissile.isDead &&
                        this.lastShotEntity != null && !this.lastShotEntity.isDead) {
                    int heliEntityID = W_Entity.getEntityId(this.lastShotEntity);
                    new PacketNotifyTVMissileEntity(
                            heliEntityID,
                            W_Entity.getEntityId(this.lastShotTvMissile))
                                    .sendToClients();
                }

                if (this.tick == 9) {
                    this.lastShotEntity = null;
                    this.lastShotTvMissile = null;
                }
            }

            if (this.tick <= 2 && this.lastShotEntity instanceof MCH_EntityAircraft) {
                ((MCH_EntityAircraft) this.lastShotEntity).setTVMissile(this.lastShotTvMissile);
            }
        }
    }

    @Override
    public boolean shot(MCH_WeaponParam prm) {
        return this.worldObj.isRemote ? this.shotClient(prm.entity, prm.user) : this.shotServer(prm);
    }

    protected boolean shotClient(Entity entity, Entity user) {
        this.optionParameter2 = 0;
        this.optionParameter1 = this.getCurrentMode();
        return true;
    }

    protected boolean shotServer(MCH_WeaponParam prm) {
        float yaw = prm.user.rotationYaw + this.fixRotationYaw;
        float pitch = prm.user.rotationPitch + this.fixRotationPitch;
        double tX = -MathHelper.sin(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI);
        double tZ = MathHelper.cos(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI);
        double tY = -MathHelper.sin(pitch / 180.0F * (float) Math.PI);
        this.isTVGuided = prm.option1 == 0;
        float acr = this.acceleration;
        if (!this.isTVGuided) {
            acr = (float) (acr * 1.5);
        }

        MCH_EntityTvMissile e = new MCH_EntityTvMissile(this.worldObj, prm.posX, prm.posY, prm.posZ, tX, tY, tZ, yaw,
                pitch, acr);
        e.setName(this.name);
        e.setParameterFromWeapon(this, prm.entity, prm.user);
        this.lastShotEntity = prm.entity;
        this.lastShotTvMissile = e;
        this.worldObj.spawnEntity(e);
        this.playSound(prm.entity);
        return true;
    }
}
