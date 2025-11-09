package com.norwood.mcheli.weapon;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.networking.packet.PacketNotifyLock;
import com.norwood.mcheli.uav.MCH_EntityUavStation;
import com.norwood.mcheli.wrapper.W_Entity;
import com.norwood.mcheli.wrapper.W_Lib;
import com.norwood.mcheli.wrapper.W_MovingObjectPosition;
import com.norwood.mcheli.wrapper.W_WorldFunc;

public class MCH_WeaponGuidanceSystem {

    public Entity lastLockEntity;
    public boolean canLockInWater;
    public boolean canLockOnGround;
    public boolean canLockInAir;
    public boolean ridableOnly;
    public double lockRange;
    public int lockAngle;
    public MCH_IEntityLockChecker checker;
    protected World worldObj;
    private Entity targetEntity;
    private int lockCount;
    private int lockSoundCount;
    private int continueLockCount;
    private int lockCountMax;
    private int prevLockCount;

    public MCH_WeaponGuidanceSystem() {
        this(null);
    }

    public MCH_WeaponGuidanceSystem(World w) {
        this.worldObj = w;
        this.targetEntity = null;
        this.lastLockEntity = null;
        this.lockCount = 0;
        this.continueLockCount = 0;
        this.lockCountMax = 1;
        this.prevLockCount = 0;
        this.canLockInWater = false;
        this.canLockOnGround = false;
        this.canLockInAir = false;
        this.ridableOnly = false;
        this.lockRange = 50.0;
        this.lockAngle = 10;
        this.checker = null;
    }

    public static boolean isEntityOnGround(@Nullable Entity entity) {
        if (entity != null && !entity.isDead) {
            if (entity.onGround) {
                return true;
            }

            for (int i = 0; i < 12; i++) {
                int x = (int) (entity.posX + 0.5);
                int y = (int) (entity.posY + 0.5) - i;
                int z = (int) (entity.posZ + 0.5);
                int blockId = W_WorldFunc.getBlockId(entity.world, x, y, z);
                if (blockId != 0 && !W_WorldFunc.isBlockWater(entity.world, x, y, z)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static float getEntityStealth(@Nullable Entity entity) {
        if (entity instanceof MCH_EntityAircraft) {
            return ((MCH_EntityAircraft) entity).getStealth();
        } else {
            return entity != null && entity.getRidingEntity() instanceof MCH_EntityAircraft ?
                    ((MCH_EntityAircraft) entity.getRidingEntity()).getStealth() : 0.0F;
        }
    }

    public static boolean inLockRange(Entity entity, float rotationYaw, float rotationPitch, Entity target,
                                      float lockAng) {
        double dx = target.posX - entity.posX;
        double dy = target.posY + target.height / 2.0F - entity.posY;
        double dz = target.posZ - entity.posZ;
        float entityYaw = (float) MCH_Lib.getRotate360(rotationYaw);
        float targetYaw = (float) MCH_Lib.getRotate360(Math.atan2(dz, dx) * 180.0 / Math.PI);
        float diffYaw = (float) MCH_Lib.getRotate360(targetYaw - entityYaw - 90.0F);
        double dxz = Math.sqrt(dx * dx + dz * dz);
        float targetPitch = -((float) (Math.atan2(dy, dxz) * 180.0 / Math.PI));
        float diffPitch = targetPitch - rotationPitch;
        return (diffYaw < lockAng || diffYaw > 360.0F - lockAng) && Math.abs(diffPitch) < lockAng;
    }

    public void setWorld(World w) {
        this.worldObj = w;
    }

    public int getLockCountMax() {
        float stealth = getEntityStealth(this.targetEntity);
        return (int) (this.lockCountMax + this.lockCountMax * stealth);
    }

    public void setLockCountMax(int i) {
        this.lockCountMax = i > 0 ? i : 1;
    }

    public int getLockCount() {
        return this.lockCount;
    }

    public boolean isLockingEntity(Entity entity) {
        return this.getLockCount() > 0 && this.targetEntity != null && !this.targetEntity.isDead &&
                W_Entity.isEqual(entity, this.targetEntity);
    }

    @Nullable
    public Entity getLockingEntity() {
        return this.getLockCount() > 0 && this.targetEntity != null && !this.targetEntity.isDead ? this.targetEntity :
                null;
    }

    @Nullable
    public Entity getTargetEntity() {
        return this.targetEntity;
    }

    public boolean isLockComplete() {
        return this.getLockCount() == this.getLockCountMax() && this.lastLockEntity != null;
    }

    public void update() {
        if (this.worldObj != null && this.worldObj.isRemote) {
            if (this.lockCount != this.prevLockCount) {
                this.prevLockCount = this.lockCount;
            } else {
                this.lockCount = this.prevLockCount = 0;
            }
        }
    }

    public boolean lock(Entity user) {
        return this.lock(user, true);
    }

    public boolean lock(Entity user, boolean isLockContinue) {
        if (!this.worldObj.isRemote) {
            return false;
        } else {
            boolean result = false;
            if (this.lockCount == 0) {
                List<Entity> list = this.worldObj
                        .getEntitiesWithinAABBExcludingEntity(user,
                                user.getEntityBoundingBox().grow(this.lockRange, this.lockRange, this.lockRange));
                Entity tgtEnt = null;
                double dist = this.lockRange * this.lockRange * 2.0;

                for (Entity entity : list) {
                    if (this.canLockEntity(entity)) {
                        double dx = entity.posX - user.posX;
                        double dy = entity.posY - user.posY;
                        double dz = entity.posZ - user.posZ;
                        double d = dx * dx + dy * dy + dz * dz;
                        Entity entityLocker = this.getLockEntity(user);
                        float stealth = 1.0F - getEntityStealth(entity);
                        double range = this.lockRange * stealth;
                        float angle = this.lockAngle * (stealth / 2.0F + 0.5F);
                        if (d < range * range && d < dist &&
                                inLockRange(entityLocker, user.rotationYaw, user.rotationPitch, entity, angle)) {
                            Vec3d v1 = new Vec3d(entityLocker.posX, entityLocker.posY, entityLocker.posZ);
                            Vec3d v2 = new Vec3d(entity.posX, entity.posY + entity.height / 2.0F, entity.posZ);
                            RayTraceResult m = W_WorldFunc.clip(this.worldObj, v1, v2, false, true, false);
                            if (m == null || W_MovingObjectPosition.isHitTypeEntity(m)) {
                                tgtEnt = entity;
                            }
                        }
                    }
                }

                this.targetEntity = tgtEnt;
                if (tgtEnt != null) {
                    this.lockCount++;
                }
            } else if (this.targetEntity != null && !this.targetEntity.isDead) {
                boolean canLock = this.canLockInWater || !this.targetEntity.isInWater();

                boolean ong = isEntityOnGround(this.targetEntity);
                if (!this.canLockOnGround && ong) {
                    canLock = false;
                }

                if (!this.canLockInAir && !ong) {
                    canLock = false;
                }

                if (canLock) {
                    double dx = this.targetEntity.posX - user.posX;
                    double dy = this.targetEntity.posY - user.posY;
                    double dz = this.targetEntity.posZ - user.posZ;
                    float stealth = 1.0F - getEntityStealth(this.targetEntity);
                    double range = this.lockRange * stealth;
                    if (dx * dx + dy * dy + dz * dz < range * range) {
                        if (this.worldObj.isRemote && this.lockSoundCount == 1) {
                            PacketNotifyLock.send(this.getTargetEntity());
                        }

                        this.lockSoundCount = (this.lockSoundCount + 1) % 15;
                        Entity entityLocker = this.getLockEntity(user);
                        if (inLockRange(entityLocker, user.rotationYaw, user.rotationPitch, this.targetEntity,
                                this.lockAngle)) {
                            if (this.lockCount < this.getLockCountMax()) {
                                this.lockCount++;
                            }
                        } else if (this.continueLockCount > 0) {
                            this.continueLockCount--;
                            if (this.continueLockCount <= 0 && this.lockCount > 0) {
                                this.lockCount--;
                            }
                        } else {
                            this.continueLockCount = 0;
                            this.lockCount--;
                        }

                        if (this.lockCount >= this.getLockCountMax()) {
                            if (this.continueLockCount <= 0) {
                                this.continueLockCount = this.getLockCountMax() / 3;
                                if (this.continueLockCount > 20) {
                                    this.continueLockCount = 20;
                                }
                            }

                            result = true;
                            this.lastLockEntity = this.targetEntity;
                            if (isLockContinue) {
                                this.prevLockCount = this.lockCount - 1;
                            } else {
                                this.clearLock();
                            }
                        }
                    } else {
                        this.clearLock();
                    }
                } else {
                    this.clearLock();
                }
            } else {
                this.clearLock();
            }

            return result;
        }
    }

    public void clearLock() {
        this.targetEntity = null;
        this.lockCount = 0;
        this.continueLockCount = 0;
        this.lockSoundCount = 0;
    }

    public Entity getLockEntity(Entity entity) {
        if (entity.getRidingEntity() instanceof MCH_EntityUavStation us) {
            if (us.getControlAircract() != null) {
                return us.getControlAircract();
            }
        }

        return entity;
    }

    public boolean canLockEntity(Entity entity) {
        if (this.ridableOnly && entity instanceof EntityPlayer && entity.getRidingEntity() == null) {
            return false;
        } else {
            String className = entity.getClass().getName();
            if (className.contains("EntityCamera")) {
                return false;
            } else if (!W_Lib.isEntityLivingBase(entity) && !(entity instanceof MCH_EntityAircraft)) {
                return false;
            } else if (!this.canLockInWater && entity.isInWater()) {
                return false;
            } else if (this.checker != null && !this.checker.canLockEntity(entity)) {
                return false;
            } else {
                boolean ong = isEntityOnGround(entity);
                return (this.canLockOnGround || !ong) && (this.canLockInAir || ong);
            }
        }
    }
}
