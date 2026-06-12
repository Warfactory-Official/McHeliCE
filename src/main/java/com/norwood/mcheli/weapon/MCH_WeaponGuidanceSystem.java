package com.norwood.mcheli.weapon;

import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_EntitySeat;
import com.norwood.mcheli.flare.MCH_EntityChaff;
import com.norwood.mcheli.flare.MCH_EntityFlare;
import com.norwood.mcheli.helper.MCH_Logger;
import com.norwood.mcheli.plane.MCH_EntityPlane;
import com.norwood.mcheli.uav.IUavStation;
import com.norwood.mcheli.uav.MCH_EntityUavStation;
import com.norwood.mcheli.wrapper.W_Entity;
import com.norwood.mcheli.wrapper.W_WorldFunc;
import lombok.Setter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

public class MCH_WeaponGuidanceSystem extends MCH_EntityGuidanceSystem {


    @Setter
    public World world;
    public Entity lastLockEntity;
    protected Entity user;
    private Entity targetEntity;

    public void setTarget(Entity target) {
        this.targetEntity = target;
    }

    public Entity getTarget() {
        return this.targetEntity;
    }

    public MCH_WeaponGuidanceSystem() {
        this(null);
    }

    public MCH_WeaponGuidanceSystem(World w) {
        this.world = w;
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

    public static boolean isEntityOnGround(@Nullable Entity entity, int height) {
        if (entity != null && !entity.isDead) {
            if (entity.onGround) {
                return true;
            }

            for (int i = 0; i < height; i++) {
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
            return entity != null && entity.getRidingEntity() instanceof MCH_EntityAircraft ? ((MCH_EntityAircraft) entity.getRidingEntity()).getStealth() : 0.0F;
        }
    }

    public static boolean inLockRange(Entity entity, float rotationYaw, float rotationPitch, Entity target, float lockAng) {
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

    public static boolean inLockAngle(Entity entity, float rotationYaw, float rotationPitch, Entity target, float lockAng) {
        double dx = target.posX - entity.posX;
        double dy = target.posY + (double) (target.height / 2.0F) - entity.posY;
        double dz = target.posZ - entity.posZ;
        float entityYaw = (float) MCH_Lib.getRotate360(rotationYaw);
        float targetYaw = (float) MCH_Lib.getRotate360(Math.atan2(dz, dx) * 180.0D / 3.141592653589793D);
        float diffYaw = (float) MCH_Lib.getRotate360(targetYaw - entityYaw - 90.0F);
        double dxz = Math.sqrt(dx * dx + dz * dz);
        float targetPitch = -((float) (Math.atan2(dy, dxz) * 180.0D / 3.141592653589793D));
        float diffPitch = targetPitch - rotationPitch;
        return (diffYaw < lockAng || diffYaw > 360.0F - lockAng) && Math.abs(diffPitch) < lockAng;
    }

    public void setWorld(World w) {
        this.world = w;
    }

    public int getLockCountMax() {
        float stealth = getEntityStealth(this.targetEntity);
        return (int) (this.lockCountMax + this.lockCountMax * stealth);
    }

    public void setLockCountMax(int i) {
        this.lockCountMax = i > 0 ? i : 1;
    }

    @Override
    protected Entity getLastLockEntity() {
        return null;
    }

    public int getLockCount() {
        return this.lockCount;
    }

    public boolean isLockingEntity(Entity entity) {
        return this.getLockCount() > 0 && this.targetEntity != null && !this.targetEntity.isDead && W_Entity.isEqual(entity, this.targetEntity);
    }

    @Nullable
    public Entity getLockingEntity() {
        return this.getLockCount() > 0 && this.targetEntity != null && !this.targetEntity.isDead ? this.targetEntity : null;
    }

    @Nullable
    public Entity getTargetEntity() {
        return this.targetEntity;
    }

    public boolean isLockComplete() {
        return this.getLockCount() == this.getLockCountMax() && this.lastLockEntity != null;
    }

    public void update() {
        if (this.world != null && this.world.isRemote) {
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
        if (!this.world.isRemote) return false;

        if (this.lockCount == 0) {
            return searchForTarget(user);
        }

        if (this.targetEntity == null || this.targetEntity.isDead) {
            this.clearLock();
            return playLockSound(user, false);
        }

        if (!canContinueLocking(user)) {
            this.clearLock();
            return playLockSound(user, false);
        }

        processLockLogic(user, isLockContinue);

        boolean locked = this.lockCount >= this.getLockCountMax();
        return playLockSound(user, locked);
    }

    private boolean searchForTarget(Entity user) {
        List<Entity> canLock = this.world.getEntitiesWithinAABBExcludingEntity(user, user.getEntityBoundingBox().grow(this.lockRange));
        Entity potentialTarget = null;
        double minDistSq = this.lockRange * this.lockRange * 2.0D;

        for (Entity current : canLock) {
            if (!this.canLockEntity(current)) continue;

            double dx = current.posX - user.posX;
            double dy = current.posY - user.posY;
            double dz = current.posZ - user.posZ;
            double distSq = dx * dx + dy * dy + dz * dz;

            Entity locker = this.getLockEntity(user);
            float stealth = 1.0F - getEntityStealth(current);
            float angle = (float) this.lockAngle * (stealth / 2.0F + 0.5F);

            if (distSq < this.lockRange * this.lockRange && distSq < minDistSq && inLockAngle(locker, user.rotationYaw, user.rotationPitch, current, angle)) {
                Vec3d v1 = new Vec3d(locker.posX, locker.posY, locker.posZ);
                Vec3d v2 = new Vec3d(current.posX, current.posY + (current.height / 2.0F), current.posZ);
                RayTraceResult m = this.world.rayTraceBlocks(v1, v2, false, true, false);

                if (m == null || m.typeOfHit == RayTraceResult.Type.ENTITY) {
                    potentialTarget = current;
                    minDistSq = distSq;
                }
            }
        }

        this.targetEntity = potentialTarget;
        if (potentialTarget != null) this.lockCount++;
        return playLockSound(user, false);
    }

    private boolean canContinueLocking(Entity user) {
        if (targetEntity instanceof MCH_EntityAircraft ac && isRadarMissile && ac.chaffUseTime > 0) return false;
        if (!this.canLockInWater && this.targetEntity.isInWater()) return false;

        boolean onGround = isEntityOnGround(this.targetEntity, lockMinHeight);
        if (!this.canLockOnGround && onGround) return false;
        if (!this.canLockInAir && !onGround) return false;

        if (getAircraft(user) instanceof MCH_EntityPlane p1 && targetEntity instanceof MCH_EntityPlane p2) {
            org.joml.Vector3f v1 = new org.joml.Vector3f((float) p1.motionX, (float) p1.motionY, (float) p1.motionZ);
            org.joml.Vector3f v2 = new org.joml.Vector3f((float) p2.motionX, (float) p2.motionY, (float) p2.motionZ);

            if (v1.length() > 0.001f && v2.length() > 0.001f) {
                float angleRad = (float) Math.acos(Math.clamp(v1.dot(v2) / (v1.length() * v2.length()), -1f, 1f));
                if (angleRad > Math.PI / 2) angleRad = (float) (Math.PI - angleRad);
                return !(Math.toDegrees(angleRad) > p1.getCurrentWeapon(user).getCurrentWeapon().getInfo().pdHDNMaxDegree);
            }
        }
        return true;
    }

    private void processLockLogic(Entity user, boolean isLockContinue) {
        double dx = this.targetEntity.posX - user.posX;
        double dy = this.targetEntity.posY - user.posY;
        double dz = this.targetEntity.posZ - user.posZ;
        double distSq = dx * dx + dy * dy + dz * dz;

        float stealth = 1.0F - getEntityStealth(this.targetEntity);
        double currentRange = this.lockRange * stealth;

        if (distSq >= currentRange * currentRange) {
            this.clearLock();
            return;
        }

        this.lockSoundCount = (this.lockSoundCount + 1) % 15;
        if (inLockAngle(getLockEntity(user), user.rotationYaw, user.rotationPitch, this.targetEntity, (float) this.lockAngle)) {
            if (this.lockCount < this.getLockCountMax()) this.lockCount++;
        } else if (this.continueLockCount > 0) {
            if (--this.continueLockCount <= 0 && this.lockCount > 0) this.lockCount--;
        } else {
            this.continueLockCount = 0;
            this.lockCount--;
        }

        if (this.lockCount >= this.getLockCountMax()) {
            if (this.continueLockCount <= 0) this.continueLockCount = Math.min(this.getLockCountMax() / 3, 20);
            this.lastLockEntity = this.targetEntity;
            if (!isLockContinue) this.clearLock();
        }
    }

    private MCH_EntityAircraft getAircraft(Entity user) {
        if (user.getRidingEntity() instanceof MCH_EntityAircraft ac) return ac;
        if (user.getRidingEntity() instanceof MCH_EntitySeat seat) return seat.getParent();
        if (user.getRidingEntity() instanceof MCH_EntityUavStation uav) return uav.getControlled();
        return null;
    }

    private boolean playLockSound(Entity user, boolean locked) {
        String soundPath = locked ? "mcheli:ir_basic_tone" : "mcheli:ir_lock_tone";
        SoundEvent sound = SoundEvent.REGISTRY.getObject(new ResourceLocation(soundPath));
        if(sound != null)
            this.world.playSound(user.posX, user.posY, user.posZ, Objects.requireNonNull(sound), SoundCategory.PLAYERS, 1.0F, 1.0F, false);
        else
            MCH_Logger.warn("Missing audio:" + soundPath);
        if (locked) this.lastLockEntity = targetEntity;
        return locked;
    }

    public void clearLock() {
        this.targetEntity = null;
        this.lockCount = 0;
        this.continueLockCount = 0;
        this.lockSoundCount = 0;
    }

    public Entity getLockEntity(Entity entity) {
        if (entity.getRidingEntity() instanceof IUavStation us) {
            if (us.getControlled() != null) {
                return us.getControlled();
            }
        }

        return entity;
    }

    public boolean canLockEntity(Entity entity) {
        switch (entity) {
            case null -> {
                return false;
            }
            case EntityPlayer _ when this.ridableOnly && entity.getRidingEntity() == null -> {
                return false;
            }
            case EntityPlayer _ when entity.getRidingEntity() != null -> {
                return false;
            }
            default -> {
            }
        }

        String className = entity.getClass().getName();
        if (className.contains("EntityCamera")) return false;

        Boolean specializedResult = switch (entity) {
            case MCH_EntityFlare _ -> isHeatSeekerMissile ? true : null;
            case MCH_EntityChaff _ -> isRadarMissile ? true : null;

            case MCH_EntityAircraft ac -> {
                if (isRadarMissile && ac.chaffUseTime > 0) yield false;
                if (ac.isFlareUsing()) yield false;
                yield null;
            }

            case MCH_EntityBaseBullet bullet -> {
                if (canLockMissile && (bullet instanceof MCH_EntityAAMissile || bullet instanceof MCH_EntityATMissile || bullet instanceof MCH_EntityASMissile || bullet instanceof MCH_EntityTvMissile)) {
                    yield !W_Entity.isEqual(user, bullet.shootingEntity);
                }
                yield null;
            }

            default -> null;
        };

        if (specializedResult != null) return specializedResult;

        boolean isTargetable = entity instanceof EntityLivingBase || entity instanceof MCH_EntityAircraft || className.contains("EntityVehicle") || className.contains("EntityPlane") || className.contains("EntityMecha") || className.contains("EntityAAGun");

        if (!isTargetable) return false;

        if (!this.canLockInWater && entity.isInWater()) return false;
        if (this.checker != null && !this.checker.canLockEntity(entity)) return false;

        boolean onGround = isEntityOnGround(entity, lockMinHeight);
        return (this.canLockOnGround || !onGround) && (this.canLockInAir || onGround);
    }

    @Override
    public double getLockPosX() {
        return targetEntity.posX;
    }

    @Override
    public double getLockPosY() {
        return targetEntity.posY;
    }

    @Override
    public double getLockPosZ() {
        return targetEntity.posZ;
    }
}

