package com.norwood.mcheli.weapon;

import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.wrapper.W_MovingObjectPosition;
import com.norwood.mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class MCH_WeaponCAS extends MCH_WeaponBase {

    public int direction;
    public Entity user;
    private double targetPosX;
    private double targetPosY;
    private double targetPosZ;
    private int startTick;
    private int cntAtk;
    private Entity shooter;

    public MCH_WeaponCAS(World w, Vec3d v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
        super(w, v, yaw, pitch, nm, wi);
        this.acceleration = 4.0F;
        this.explosionPower = 2;
        this.power = 32;
        this.interval = 65236;
        if (w.isRemote) {
            this.interval -= 10;
        }

        this.targetPosX = 0.0;
        this.targetPosY = 0.0;
        this.targetPosZ = 0.0;
        this.direction = 0;
        this.startTick = 0;
        this.cntAtk = 3;
        this.shooter = null;
        this.user = null;
    }

    @Override
    public void update(int countWait) {
        super.update(countWait);
        if (!this.worldObj.isRemote && this.cntAtk < 3 && countWait != 0 && this.tick == this.startTick) {
            double x = 0.0;
            double z = 0.0;
            if (this.cntAtk >= 1) {
                double sign = this.cntAtk == 1 ? 1.0 : -1.0;
                if (this.direction == 0 || this.direction == 2) {
                    x = rand.nextDouble() * 10.0 * sign;
                    z = (rand.nextDouble() - 0.5) * 10.0;
                }

                if (this.direction == 1 || this.direction == 3) {
                    z = rand.nextDouble() * 10.0 * sign;
                    x = (rand.nextDouble() - 0.5) * 10.0;
                }
            }

            this.spawnA10(this.targetPosX + x, this.targetPosY + 20.0, this.targetPosZ + z);
            this.startTick = this.tick + 45;
            this.cntAtk++;
        }
    }

    @Override
    public void modifyParameters() {
        if (this.interval > 65286) {
            this.interval = 65286;
        }
    }

    public void setTargetPosition(double x, double y, double z) {
        this.targetPosX = x;
        this.targetPosY = y;
        this.targetPosZ = z;
    }

    public void spawnA10(double x, double y, double z) {
        double mX = 0.0;
        double mY = 0.0;
        double mZ = 0.0;
        if (this.direction == 0) {
            mZ += 3.0;
            z -= 90.0;
        }

        if (this.direction == 1) {
            mX -= 3.0;
            x += 90.0;
        }

        if (this.direction == 2) {
            mZ -= 3.0;
            z += 90.0;
        }

        if (this.direction == 3) {
            mX += 3.0;
            x -= 90.0;
        }

        MCH_EntityA10 a10 = new MCH_EntityA10(this.worldObj, x, y, z);
        a10.setWeaponName(this.name);
        a10.prevRotationYaw = a10.rotationYaw = 90 * this.direction;
        a10.motionX = mX;
        a10.motionY = mY;
        a10.motionZ = mZ;
        a10.direction = this.direction;
        a10.shootingEntity = this.user;
        a10.shootingAircraft = this.shooter;
        a10.explosionPower = this.explosionPower;
        a10.power = this.power;
        a10.acceleration = this.acceleration;
        this.worldObj.spawnEntity(a10);
        W_WorldFunc.MOD_playSoundEffect(this.worldObj, x, y, z, "a-10_snd", 150.0F, 1.0F);
    }

    @Override
    public boolean shot(MCH_WeaponParam prm) {
        float yaw = prm.user.rotationYaw;
        float pitch = prm.user.rotationPitch;
        double tX = -MathHelper.sin(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI);
        double tZ = MathHelper.cos(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI);
        double tY = -MathHelper.sin(pitch / 180.0F * (float) Math.PI);
        double dist = MathHelper.sqrt(tX * tX + tY * tY + tZ * tZ);
        if (this.worldObj.isRemote) {
            tX = tX * 80.0 / dist;
            tY = tY * 80.0 / dist;
            tZ = tZ * 80.0 / dist;
        } else {
            tX = tX * 150.0 / dist;
            tY = tY * 150.0 / dist;
            tZ = tZ * 150.0 / dist;
        }

        Vec3d src = new Vec3d(prm.entity.posX, prm.entity.posY + 2.0, prm.entity.posZ);
        Vec3d dst = new Vec3d(prm.entity.posX + tX, prm.entity.posY + tY + 2.0, prm.entity.posZ + tZ);
        RayTraceResult m = W_WorldFunc.clip(this.worldObj, src, dst);
        if (W_MovingObjectPosition.isHitTypeTile(m)) {
            this.targetPosX = m.hitVec.x;
            this.targetPosY = m.hitVec.y;
            this.targetPosZ = m.hitVec.z;
            this.direction = (int) MCH_Lib.getRotate360(yaw + 45.0F) / 90;
            this.direction = this.direction + (rand.nextBoolean() ? -1 : 1);
            this.direction %= 4;
            if (this.direction < 0) {
                this.direction += 4;
            }

            this.user = prm.user;
            this.shooter = prm.entity;
            if (prm.entity != null) {
                this.playSoundClient(prm.entity, 1.0F, 1.0F);
            }

            this.startTick = 50;
            this.cntAtk = 0;
            return true;
        } else {
            return false;
        }
    }

    public boolean shot(Entity user, double px, double py, double pz, int option1, int option2) {
        float yaw = user.rotationYaw;
        float pitch = user.rotationPitch;
        double tX = -MathHelper.sin(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI);
        double tZ = MathHelper.cos(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI);
        double tY = -MathHelper.sin(pitch / 180.0F * (float) Math.PI);
        double dist = MathHelper.sqrt(tX * tX + tY * tY + tZ * tZ);
        if (this.worldObj.isRemote) {
            tX = tX * 80.0 / dist;
            tY = tY * 80.0 / dist;
            tZ = tZ * 80.0 / dist;
        } else {
            tX = tX * 120.0 / dist;
            tY = tY * 120.0 / dist;
            tZ = tZ * 120.0 / dist;
        }

        Vec3d src = new Vec3d(px, py, pz);
        Vec3d dst = new Vec3d(px + tX, py + tY, pz + tZ);
        RayTraceResult m = W_WorldFunc.clip(this.worldObj, src, dst);
        if (W_MovingObjectPosition.isHitTypeTile(m)) {
            if (this.worldObj.isRemote) {
                double dx = m.hitVec.x - px;
                double dz = m.hitVec.z - pz;
                if (Math.sqrt(dx * dx + dz * dz) < 20.0) {
                    return false;
                }
            }

            this.targetPosX = m.hitVec.x;
            this.targetPosY = m.hitVec.y;
            this.targetPosZ = m.hitVec.z;
            this.direction = (int) MCH_Lib.getRotate360(yaw + 45.0F) / 90;
            this.direction = this.direction + (rand.nextBoolean() ? -1 : 1);
            this.direction %= 4;
            if (this.direction < 0) {
                this.direction += 4;
            }

            this.user = user;
            this.shooter = null;
            this.tick = 0;
            this.startTick = 50;
            this.cntAtk = 0;
            return true;
        } else {
            return false;
        }
    }
}
