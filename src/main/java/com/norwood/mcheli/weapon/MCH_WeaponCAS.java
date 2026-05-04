package com.norwood.mcheli.weapon;

import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.Tags;
import com.norwood.mcheli.sound.MCH_SoundEvents;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class MCH_WeaponCAS extends MCH_WeaponBase {

    private double targetPosX, targetPosY, targetPosZ;
    private int direction;
    private int startTick;
    private int attackCount;
    private Entity user;
    private Entity shooter;

    public MCH_WeaponCAS(World world, Vec3d pos, float yaw, float pitch, String name, MCH_WeaponInfo info) {
        super(world, pos, yaw, pitch, name, info);
        this.acceleration = 4.0F;
        this.explosionPower = 2;
        this.power = 32;
        this.interval = world.isRemote ? 65226 : 65236;

        this.direction = 0;
        this.startTick = 0;
        this.attackCount = 3;
    }

    @Override
    public void update(int countWait) {
        super.update(countWait);

        if (!world.isRemote && attackCount < 3 && countWait != 0 && tick == startTick) {
            double offsetX = 0;
            double offsetZ = 0;

            if (attackCount >= 1) {
                double sign = (attackCount == 1) ? 1.0 : -1.0;
                if (direction == 0 || direction == 2) {
                    offsetX = rand.nextDouble() * 10.0 * sign;
                    offsetZ = (rand.nextDouble() - 0.5) * 10.0;
                } else {
                    offsetZ = rand.nextDouble() * 10.0 * sign;
                    offsetX = (rand.nextDouble() - 0.5) * 10.0;
                }
            }

            spawnA10(targetPosX + offsetX, targetPosY + 20.0, targetPosZ + offsetZ);
            this.startTick = this.tick + 45;
            this.attackCount++;
        }
    }

    @Override
    public void modifyParameters() {
        if (this.interval > 65286) {
            this.interval = 65286;
        }
    }

    public void spawnA10(double x, double y, double z) {
        double motionX = 0, motionZ = 0;
        double spawnX = x, spawnZ = z;

        switch (this.direction) {
            case 0 -> {
                motionZ = 3.0;
                spawnZ -= 90.0;
            }
            case 1 -> {
                motionX = -3.0;
                spawnX += 90.0;
            }
            case 2 -> {
                motionZ = -3.0;
                spawnZ += 90.0;
            }
            case 3 -> {
                motionX = 3.0;
                spawnX -= 90.0;
            }
        }

        MCH_EntityA10 a10 = new MCH_EntityA10(world, spawnX, y, spawnZ);
        a10.setWeaponName(this.name);
        a10.prevRotationYaw = a10.rotationYaw = 90.0F * this.direction;
        a10.motionX = motionX;
        a10.motionY = 0;
        a10.motionZ = motionZ;
        a10.direction = this.direction;
        a10.shootingEntity = this.user;
        a10.shootingAircraft = this.shooter;
        a10.explosionPower = this.explosionPower;
        a10.power = this.power;
        a10.acceleration = this.acceleration;

        world.spawnEntity(a10);
        MCH_SoundEvents.playSound(world, spawnX, y, spawnZ, Tags.MODID + ":a-10_snd", 150.0F, 1.0F);
    }

    @Override
    public boolean shot(MCH_WeaponParam prm) {
        return performStrikeInquiry(prm.user, prm.entity, prm.entity.posX, prm.entity.posY + 2.0, prm.entity.posZ);
    }

    private boolean performStrikeInquiry(Entity user, Entity shooter, double px, double py, double pz) {
        float yaw = user.rotationYaw;
        float pitch = user.rotationPitch;

        Vec3d lookVec = Vec3d.fromPitchYaw(pitch, yaw);
        double range = world.isRemote ? 80.0 : 150.0;

        Vec3d src = new Vec3d(px, py, pz);
        Vec3d dst = src.add(lookVec.scale(range));

        RayTraceResult hit = world.rayTraceBlocks(src, dst, false, true, true);

        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) {
            return false;
        }

        if (world.isRemote) {
            if (hit.hitVec.squareDistanceTo(px, py, pz) < 400.0) {
                return false;
            }
        }

        this.targetPosX = hit.hitVec.x;
        this.targetPosY = hit.hitVec.y;
        this.targetPosZ = hit.hitVec.z;

        int baseDir = (int) MCH_Lib.getRotate360(yaw + 45.0F) / 90;
        this.direction = (baseDir + (rand.nextBoolean() ? -1 : 1)) % 4;
        if (this.direction < 0) this.direction += 4;

        this.user = user;
        this.shooter = shooter;

        if (shooter != null) {
            this.playSoundClient(shooter, 1.0F, 1.0F);
        }

        this.tick = 0;
        this.startTick = 50;
        this.attackCount = 0;

        return true;
    }
}
