package com.norwood.mcheli.weapon;

import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import com.norwood.mcheli.MCH_Lib;

public class MCH_WeaponMachineGun2 extends MCH_WeaponBase {

    public MCH_WeaponMachineGun2(World w, Vec3d v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
        super(w, v, yaw, pitch, nm, wi);
        this.power = 16;
        this.acceleration = 4.0F;
        this.explosionPower = 1;
        this.interval = 2;
        this.numMode = 2;
    }

    @Override
    public void modifyParameters() {
        if (this.explosionPower == 0) {
            this.numMode = 0;
        }
    }

    @Override
    public String getName() {
        return super.getName() + (this.getCurrentMode() == 0 ? "" : " [HE]");
    }

    @Override
    public boolean shot(MCH_WeaponParam prm) {
        if (!this.worldObj.isRemote) {
            Vec3d v = MCH_Lib.RotVec3(0.0, 0.0, 1.0, -prm.rotYaw, -prm.rotPitch, -prm.rotRoll);
            MCH_EntityBullet e = new MCH_EntityBullet(this.worldObj, prm.posX, prm.posY, prm.posZ, v.x, v.y, v.z,
                    prm.rotYaw, prm.rotPitch, this.acceleration);
            e.setName(this.name);
            e.setParameterFromWeapon(this, prm.entity, prm.user);
            if (this.getInfo().modeNum < 2) {
                e.explosionPower = this.explosionPower;
            } else {
                e.explosionPower = prm.option1 == 0 ? -this.explosionPower : this.explosionPower;
            }

            e.posX = e.posX + e.motionX * 0.5;
            e.posY = e.posY + e.motionY * 0.5;
            e.posZ = e.posZ + e.motionZ * 0.5;
            this.worldObj.spawnEntity(e);
            this.playSound(prm.entity);
        } else {
            this.optionParameter1 = this.getCurrentMode();
        }

        return true;
    }
}
