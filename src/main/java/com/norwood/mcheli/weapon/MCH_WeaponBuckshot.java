package com.norwood.mcheli.weapon;

import com.norwood.mcheli.MCH_PlayerViewHandler;
import com.norwood.mcheli.weapon.MCH_WeaponInfo.BuckshotPayload;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;


public class MCH_WeaponBuckshot extends MCH_WeaponBase {

    public MCH_WeaponBuckshot(World world, Vec3d pos, float yaw, float pitch, String name, MCH_WeaponInfo info) {
        super(world, pos, yaw, pitch, name, info);
        this.power = 8;
        this.acceleration = 4.0F;
        this.explosionPower = 0;
        this.interval = 0;
    }

    @Override
    public boolean shot(MCH_WeaponParam prm) {
        if (!world.isRemote) {
            Vec2f baseRot = calculateShotRotation(prm, false, false);
            int count = Math.max(1, getInfo().buckshotCount);
            float accuracy = getInfo().accuracy;
            BuckshotPayload payload = getInfo().buckshotPayload;

            for (int i = 0; i < count; i++) {
                float yaw = baseRot.x;
                float pitch = baseRot.y;

                // Per-pellet scatter inside a circular cone sized by accuracy.
                if (accuracy > 0.0F) {
                    double r = Math.sqrt(rand.nextDouble()) * (accuracy * 0.5);
                    double theta = rand.nextDouble() * 2 * Math.PI;
                    yaw += (float) (r * Math.cos(theta));
                    pitch += (float) (r * Math.sin(theta));
                }

                yaw = MathHelper.wrapDegrees(yaw);
                pitch = MathHelper.wrapDegrees(pitch);

                Vec3d motion = Vec3d.fromPitchYaw(pitch, yaw);
                MCH_EntityBaseBullet pellet = createPellet(payload, prm, motion, yaw, pitch);

                pellet.setName(this.name);
                pellet.setParameterFromWeapon(this, prm.entity, prm.user);
                pellet.setAirburstDist(this.airburstDist);

                pellet.posX += pellet.motionX * 0.5D;
                pellet.posY += pellet.motionY * 0.5D;
                pellet.posZ += pellet.motionZ * 0.5D;

                world.spawnEntity(pellet);
            }

            this.playSound(prm.entity);
        } else {
            MCH_PlayerViewHandler.applyRecoil(getInfo().getRecoilPitch(), getInfo().getRecoilYaw(),
                    getInfo().recoilRecoverFactor);
        }

        return true;
    }

    private MCH_EntityBaseBullet createPellet(BuckshotPayload payload, MCH_WeaponParam prm, Vec3d motion,
                                              float yaw, float pitch) {
        if (payload == BuckshotPayload.ROCKET) {
            return new MCH_EntityRocket(world, prm.posX, prm.posY, prm.posZ,
                    motion.x, motion.y, motion.z, yaw, pitch, this.acceleration);
        }
        return new MCH_EntityBullet(world, prm.posX, prm.posY, prm.posZ,
                motion.x, motion.y, motion.z, yaw, pitch, this.acceleration);
    }
}
