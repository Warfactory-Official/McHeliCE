package com.norwood.mcheli.weapon;

import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class MCH_WeaponASMissile extends MCH_WeaponBase {

    public MCH_WeaponASMissile(World world, Vec3d position, float yaw, float pitch, String name, MCH_WeaponInfo weaponInfo) {
        super(world, position, yaw, pitch, name, weaponInfo);
        this.acceleration = 3.0F;
        this.explosionPower = 9;
        this.power = 40;
        this.interval = -350;

        if (world.isRemote) {
            this.interval -= 10;
        }
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
        if (!this.world.isRemote) {
            float yaw = prm.user.rotationYaw;
            float pitch = prm.user.rotationPitch;

            Vec3d lookDir = Vec3d.fromPitchYaw(pitch, yaw);

            double maxDist = 1500.0;
            Vec3d src = new Vec3d(prm.entity.posX, prm.entity.posY + prm.entity.getEyeHeight(), prm.entity.posZ);
            Vec3d dst = src.add(lookDir.scale(maxDist));

            RayTraceResult hitResult = this.world.rayTraceBlocks(src, dst, false, true, true);

            Vec3d hitVec = (hitResult != null && hitResult.typeOfHit != RayTraceResult.Type.MISS)
                    ? hitResult.hitVec
                    : dst;

            MCH_EntityASMissile missile = new MCH_EntityASMissile(
                    this.world,
                    prm.posX, prm.posY, prm.posZ,
                    lookDir.x, lookDir.y, lookDir.z,
                    yaw, pitch, this.acceleration
            );

            missile.setName(this.name);
            missile.setParameterFromWeapon(this, prm.entity, prm.user);

            missile.targetPosX = hitVec.x;
            missile.targetPosY = hitVec.y;
            missile.targetPosZ = hitVec.z;

            this.world.spawnEntity(missile);
            this.playSound(prm.entity);
        }

        return true;
    }
}
