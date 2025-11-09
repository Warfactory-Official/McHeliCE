package com.norwood.mcheli.weapon;

import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import com.norwood.mcheli.MCH_Lib;

public class MCH_WeaponDispenser extends MCH_WeaponBase {

    public MCH_WeaponDispenser(World w, Vec3d v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
        super(w, v, yaw, pitch, nm, wi);
        this.acceleration = 0.5F;
        this.explosionPower = 0;
        this.power = 0;
        this.interval = -90;
        if (w.isRemote) {
            this.interval -= 10;
        }
    }

    @Override
    public boolean shot(MCH_WeaponParam prm) {
        if (!this.worldObj.isRemote) {
            this.playSound(prm.entity);
            Vec3d v = MCH_Lib.RotVec3(0.0, 0.0, 1.0, -prm.rotYaw, -prm.rotPitch, -prm.rotRoll);
            MCH_EntityDispensedItem e = new MCH_EntityDispensedItem(
                    this.worldObj, prm.posX, prm.posY, prm.posZ, v.x, v.y, v.z, prm.rotYaw, prm.rotPitch,
                    this.acceleration);
            e.setName(this.name);
            e.setParameterFromWeapon(this, prm.entity, prm.user);
            e.motionX = prm.entity.motionX + e.motionX * 0.5;
            e.motionY = prm.entity.motionY + e.motionY * 0.5;
            e.motionZ = prm.entity.motionZ + e.motionZ * 0.5;
            e.posX = e.posX + e.motionX * 0.5;
            e.posY = e.posY + e.motionY * 0.5;
            e.posZ = e.posZ + e.motionZ * 0.5;
            this.worldObj.spawnEntity(e);
        }

        return true;
    }
}
