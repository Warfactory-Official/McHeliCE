package com.norwood.mcheli.weapon;

import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import com.norwood.mcheli.MCH_Lib;

public class MCH_WeaponMarkerRocket extends MCH_WeaponBase {

    public MCH_WeaponMarkerRocket(World w, Vec3d v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
        super(w, v, yaw, pitch, nm, wi);
        this.acceleration = 3.0F;
        this.explosionPower = 0;
        this.power = 0;
        this.interval = 60;
        if (w.isRemote) {
            this.interval += 10;
        }
    }

    @Override
    public boolean shot(MCH_WeaponParam prm) {
        if (!this.worldObj.isRemote) {
            this.playSound(prm.entity);
            Vec3d v = MCH_Lib.RotVec3(0.0, 0.0, 1.0, -prm.rotYaw, -prm.rotPitch, -prm.rotRoll);
            MCH_EntityMarkerRocket e = new MCH_EntityMarkerRocket(
                    this.worldObj, prm.posX, prm.posY, prm.posZ, v.x, v.y, v.z, prm.rotYaw, prm.rotPitch,
                    this.acceleration);
            e.setName(this.name);
            e.setParameterFromWeapon(this, prm.entity, prm.user);
            e.setMarkerStatus(1);
            this.worldObj.spawnEntity(e);
        } else {
            this.optionParameter1 = this.getCurrentMode();
        }

        return true;
    }
}
