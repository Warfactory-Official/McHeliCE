package com.norwood.mcheli.weapon;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.multiplay.MCH_Multiplay;

public class MCH_WeaponTargetingPod extends MCH_WeaponBase {

    public MCH_WeaponTargetingPod(World w, Vec3d v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
        super(w, v, yaw, pitch, nm, wi);
        this.interval = -90;
        if (w.isRemote) {
            this.interval -= 10;
        }
    }

    @Override
    public boolean shot(MCH_WeaponParam prm) {
        if (!this.worldObj.isRemote) {
            MCH_WeaponInfo info = this.getInfo();
            if ((info.target & 64) != 0) {
                if (MCH_Multiplay.markPoint((EntityPlayer) prm.user, prm.posX, prm.posY, prm.posZ)) {
                    this.playSound(prm.user);
                } else {
                    this.playSound(prm.user, "ng");
                }
            } else if (MCH_Multiplay.spotEntity(
                    (EntityLivingBase) prm.user, (MCH_EntityAircraft) prm.entity, prm.posX, prm.posY, prm.posZ,
                    info.target, info.length, info.markTime, info.angle)) {
                        this.playSound(prm.entity);
                    } else {
                        this.playSound(prm.entity, "ng");
                    }
        }

        return true;
    }
}
