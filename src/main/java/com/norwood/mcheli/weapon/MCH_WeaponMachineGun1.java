package com.norwood.mcheli.weapon;

import com.norwood.mcheli.MCH_PlayerViewHandler;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class MCH_WeaponMachineGun1 extends MCH_WeaponBase {

    public MCH_WeaponMachineGun1(World world, Vec3d pos, float yaw, float pitch, String name, MCH_WeaponInfo info) {
        super(world, pos, yaw, pitch, name, info);
        this.power = 8;
        this.acceleration = 4.0F;
        this.explosionPower = 0;
        this.interval = 0;
    }

    @Override
    public boolean shot(MCH_WeaponParam prm) {
        if (!world.isRemote) {
            Vec2f rot = calculateShotRotation(prm, false, false);
            Vec3d motion = Vec3d.fromPitchYaw(rot.y, rot.x);

            MCH_EntityBullet bullet = new MCH_EntityBullet(world, prm.posX, prm.posY, prm.posZ,
                    motion.x, motion.y, motion.z, rot.x, rot.y, this.acceleration);

            bullet.setName(this.name);
            bullet.setParameterFromWeapon(this, prm.entity, prm.user);
            bullet.setAirburstDist(this.airburstDist);

            bullet.posX += bullet.motionX * 0.5D;
            bullet.posY += bullet.motionY * 0.5D;
            bullet.posZ += bullet.motionZ * 0.5D;

            world.spawnEntity(bullet);
            this.playSound(prm.entity);
        } else {
            MCH_PlayerViewHandler.applyRecoil(getInfo().getRecoilPitch(), getInfo().getRecoilYaw(), getInfo().recoilRecoverFactor);
        }

        return true;
    }
}
