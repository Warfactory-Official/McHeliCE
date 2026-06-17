package com.norwood.mcheli.weapon;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_Explosion;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_EntityHitBox;
import com.norwood.mcheli.aircraft.MCH_EntitySeat;
import com.norwood.mcheli.chain.MCH_EntityChain;
import com.norwood.mcheli.compat.hbm.HBMUtil;
import com.norwood.mcheli.flare.MCH_EntityChaff;
import com.norwood.mcheli.flare.MCH_EntityFlare;
import com.norwood.mcheli.helper.MCH_CriteriaTriggers;
import com.norwood.mcheli.helper.MCH_Logger;
import com.norwood.mcheli.helper.world.MCH_ExplosionV2;
import com.norwood.mcheli.networking.PacketLockTarget;
import com.norwood.mcheli.networking.packet.PacketClientSound;
import com.norwood.mcheli.networking.packet.PacketNotifyHit;
import com.norwood.mcheli.particles.MCH_ParticleParam;
import com.norwood.mcheli.particles.MCH_ParticlesUtil;
import com.norwood.mcheli.wingman.handler.ProjectileChunkLoader;
import com.norwood.mcheli.wrapper.W_Entity;
import com.norwood.mcheli.wrapper.W_EntityPlayer;
import com.norwood.mcheli.wrapper.W_WorldFunc;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleCloud;
import net.minecraft.client.particle.ParticleDigging;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

import static com.norwood.mcheli.compat.ModCompatManager.MODID_HBM;
import static com.norwood.mcheli.compat.ModCompatManager.isLoaded;

public abstract class MCH_EntityBaseBullet extends W_Entity {

    private static final DataParameter<Integer> TARGET_ID = EntityDataManager.createKey(MCH_EntityBaseBullet.class, DataSerializers.VARINT);
    private static final DataParameter<String> INFO_NAME = EntityDataManager.createKey(MCH_EntityBaseBullet.class, DataSerializers.STRING);
    private static final DataParameter<String> BULLET_MODEL = EntityDataManager.createKey(MCH_EntityBaseBullet.class, DataSerializers.STRING);
    private static final DataParameter<Byte> BOMBLET_FLAG = EntityDataManager.createKey(MCH_EntityBaseBullet.class, DataSerializers.BYTE);
    public Entity shootingEntity;
    public Entity shootingAircraft;
    public int explosionPower;
    public int explosionPowerInWater;
    public double acceleration;
    public double accelerationFactor;
    public Entity targetEntity;
    public int piercing;
    public int delayFuse;
    public int sprinkleTime;
    public byte isBomblet;
    public double prevPosX2;
    public double prevPosY2;
    public double prevPosZ2;
    public double prevMotionX;
    public double prevMotionY;
    public double prevMotionZ;
    public boolean antiFlareUse;
    public int antiFlareTick;
    public int numLockedChaff = 0;
    public int airburstDist = 0;
    boolean doingTopAttack = false;
    boolean speedAddedFromAircraft = false;
    @Getter
    private int countOnUpdate = 0;
    @Getter
    @Setter
    private int power;
    private MCH_WeaponInfo weaponInfo;
    private MCH_BulletModel model;
    private double airburstTravelled = 0.0D;
    private boolean airburstTriggered = false;
    private boolean aheadTriggered = false;
    private int spawnedBulletNum = 0;

    public MCH_EntityBaseBullet(World par1World) {
        super(par1World);
        this.setSize(1.0F, 1.0F);
        this.prevRotationYaw = this.rotationYaw;
        this.prevRotationPitch = this.rotationPitch;
        this.targetEntity = null;
        this.setPower(1);
        this.acceleration = 1.0;
        this.accelerationFactor = 1.0;
        this.piercing = 0;
        this.explosionPower = 0;
        this.explosionPowerInWater = 0;
        this.delayFuse = 0;
        this.sprinkleTime = 0;
        this.isBomblet = -1;
        this.weaponInfo = null;
        this.ignoreFrustumCheck = true;
        if (par1World.isRemote) {
            this.model = null;
        }
    }

    public MCH_EntityBaseBullet(World par1World, double px, double py, double pz, double mx, double my, double mz, float yaw, float pitch, double acceleration) {
        this(par1World);
        this.setSize(1.0F, 1.0F);
        this.setLocationAndAngles(px, py, pz, yaw, pitch);
        this.setPosition(px, py, pz);
        this.prevRotationYaw = yaw;
        this.prevRotationPitch = pitch;
        if (acceleration > 3.9) {
            acceleration = 3.9;
        }

        double d = MathHelper.sqrt(mx * mx + my * my + mz * mz);
        this.motionX = mx * acceleration / d;
        this.motionY = my * acceleration / d;
        this.motionZ = mz * acceleration / d;
        this.prevMotionX = this.motionX;
        this.prevMotionY = this.motionY;
        this.prevMotionZ = this.motionZ;
        this.acceleration = acceleration;
    }

    /**
     * @return true if this projectile should force-load the chunks along its path — either its weapon
     * definition opts in ({@code CanLoadChunks} -> {@link MCH_WeaponInfo#enableChunkLoader}) or chunk
     * loading is enabled globally for all projectiles. Off by default for both.
     */
    public boolean isChunkLoadingEnabled() {
        MCH_WeaponInfo info = this.getInfo();
        return info != null && (info.enableChunkLoader || MCH_Config.ProjectileChunkLoad.prmBool);
    }

    public void setLocationAndAngles(double par1, double par3, double par5, float par7, float par8) {
        super.setLocationAndAngles(par1, par3, par5, par7, par8);
        this.prevPosX2 = par1;
        this.prevPosY2 = par3;
        this.prevPosZ2 = par5;
    }

    public void setPositionAndRotation(double x, double y, double z, float yaw, float pitch) {
        super.setPositionAndRotation(x, y, z, yaw, pitch);
    }

    protected void setRotation(float yaw, float pitch) {
        super.setRotation(yaw, this.rotationPitch);
    }

    @SideOnly(Side.CLIENT)
    public void setPositionAndRotationDirect(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {
        this.setPosition(x, (y + this.posY * 2.0) / 3.0, z);
        this.setRotation(yaw, pitch);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(TARGET_ID, 0);
        this.dataManager.register(INFO_NAME, "");
        this.dataManager.register(BULLET_MODEL, "");
        this.dataManager.register(BOMBLET_FLAG, (byte) 0);
    }

    public @NotNull String getName() {
        return this.dataManager.get(INFO_NAME);
    }

    public void setName(String s) {
        if (s != null && !s.isEmpty()) {
            this.weaponInfo = MCH_WeaponInfoManager.get(s);
            if (this.weaponInfo != null) {
                if (!this.world.isRemote) {
                    this.dataManager.set(INFO_NAME, s);
                }

                this.onSetWeasponInfo();
            }
        }
    }

    @Nullable
    public MCH_WeaponInfo getInfo() {
        return this.weaponInfo;
    }

    public void onSetWeasponInfo() {
        if (!this.world.isRemote) {
            this.isBomblet = 0;
        }

        if (this.getInfo().bomblet > 0) {
            this.sprinkleTime = this.getInfo().bombletSTime;
        }

        this.piercing = this.getInfo().piercing;
        if (this instanceof MCH_EntityBullet) {
            if (this.getInfo().acceleration > 4.0F) {
                this.accelerationFactor = this.getInfo().acceleration / 4.0F;
            }
        } else if (this instanceof MCH_EntityRocket && this.isBomblet == 0 && this.getInfo().acceleration > 4.0F) {
            this.accelerationFactor = this.getInfo().acceleration / 4.0F;
        }
    }

    public void setDead() {
        super.setDead();
    }

    public void setBomblet() {
        this.isBomblet = 1;
        this.sprinkleTime = 0;
        this.dataManager.set(BOMBLET_FLAG, (byte) 1);
    }

    public byte getBomblet() {
        return this.dataManager.get(BOMBLET_FLAG);
    }

    public void setTargetEntity(@Nullable Entity entity) {
        this.targetEntity = entity;
        if (!this.world.isRemote) {
            if (this.targetEntity instanceof EntityPlayerMP) {
                String format = "MCH_EntityBaseBullet.setTargetEntity alert" + this.targetEntity + " / " + this.targetEntity.getRidingEntity();
                MCH_Logger.debugLog(this.world, format);
                if (this.targetEntity.getRidingEntity() != null && !(this.targetEntity.getRidingEntity() instanceof MCH_EntityAircraft) && !(this.targetEntity.getRidingEntity() instanceof MCH_EntitySeat)) {
                    W_WorldFunc.playSoundAt(this.targetEntity, "alert", 2.0F, 1.0F);
                }
            }

            if (entity != null) {
                this.dataManager.set(TARGET_ID, W_Entity.getEntityId(entity));
            } else {
                this.dataManager.set(TARGET_ID, 0);
            }
        }
    }

    public int getTargetEntityID() {
        return this.targetEntity != null ? W_Entity.getEntityId(this.targetEntity) : this.dataManager.get(TARGET_ID);
    }

    public MCH_BulletModel getBulletModel() {
        if (this.getInfo() == null) {
            return null;
        } else if (this.isBomblet < 0) {
            return null;
        } else {
            if (this.model == null) {
                if (this.isBomblet == 1) {
                    this.model = this.getInfo().bombletModel;
                } else {
                    this.model = this.getInfo().bulletModel;
                }

                if (this.model == null) {
                    this.model = this.getDefaultBulletModel();
                }
            }

            return this.model;
        }
    }

    public abstract MCH_BulletModel getDefaultBulletModel();

    public void sprinkleBomblet() {
    }

    public void spawnParticle(String name, int num, float size) {
        if (this.world.isRemote) {
            if (name.isEmpty() || num < 1 || num > 50) {
                return;
            }

            double x = (this.posX - this.prevPosX) / num;
            double y = (this.posY - this.prevPosY) / num;
            double z = (this.posZ - this.prevPosZ) / num;
            double x2 = (this.prevPosX - this.prevPosX2) / num;
            double y2 = (this.prevPosY - this.prevPosY2) / num;
            double z2 = (this.prevPosZ - this.prevPosZ2) / num;
            if (name.equals("explode")) {
                for (int i = 0; i < num; i++) {
                    MCH_ParticleParam prm = new MCH_ParticleParam(this.world, "smoke", (this.prevPosX + x * i + (this.prevPosX2 + x2 * i)) / 2.0, (this.prevPosY + y * i + (this.prevPosY2 + y2 * i)) / 2.0, (this.prevPosZ + z * i + (this.prevPosZ2 + z2 * i)) / 2.0);
                    prm.size = size + this.rand.nextFloat();
                    MCH_ParticlesUtil.spawnParticle(prm);
                }
            } else {
                for (int i = 0; i < num; i++) {
                    MCH_ParticlesUtil.DEF_spawnParticle(name, (this.prevPosX + x * i + (this.prevPosX2 + x2 * i)) / 2.0, (this.prevPosY + y * i + (this.prevPosY2 + y2 * i)) / 2.0, (this.prevPosZ + z * i + (this.prevPosZ2 + z2 * i)) / 2.0, 0.0, 0.0, 0.0, 50.0F);
                }
            }
        }
    }

    public void clearCountOnUpdate() {
        this.countOnUpdate = 0;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean isInRangeToRenderDist(double par1) {
        double d1 = this.getEntityBoundingBox().getAverageEdgeLength() * 4.0;
        d1 *= 64.0;
        return par1 < d1 * d1;
    }

    public void setParameterFromWeapon(MCH_WeaponBase w, Entity entity, Entity user) {
        this.explosionPower = w.explosionPower;
        this.explosionPowerInWater = w.explosionPowerInWater;
        this.setPower(w.power);
        this.piercing = w.piercing;
        this.shootingAircraft = entity;
        this.shootingEntity = user;
        this.airburstDist = w.airburstDist;
    }

    public void setParameterFromWeapon(MCH_EntityBaseBullet b, Entity entity, Entity user) {
        this.explosionPower = b.explosionPower;
        this.explosionPowerInWater = b.explosionPowerInWater;
        this.setPower(b.getPower());
        this.piercing = b.piercing;
        this.shootingAircraft = entity;
        this.shootingEntity = user;
        this.airburstDist = b.airburstDist;
    }

    public void setAirburstDist(int airburstDist) {
        this.airburstDist = airburstDist;
    }

    public void setMotion(double targetX, double targetY, double targetZ) {
        double d6 = MathHelper.sqrt(targetX * targetX + targetY * targetY + targetZ * targetZ);
        this.motionX = targetX * this.acceleration / d6;
        this.motionY = targetY * this.acceleration / d6;
        this.motionZ = targetZ * this.acceleration / d6;
    }

    public boolean usingFlareOfTarget(Entity entity) {
        if (this.getCountOnUpdate() % 3 == 0) {
            List<Entity> list = this.world.getEntitiesWithinAABBExcludingEntity(this, entity.getEntityBoundingBox().grow(15.0, 15.0, 15.0));

            for (Entity value : list) {
                if (value.getEntityData().getBoolean("FlareUsing")) {
                    return true;
                }
            }
        }

        return false;
    }

    public void guidanceToTarget(double targetPosX, double targetPosY, double targetPosZ) {
        this.guidanceToTarget(targetPosX, targetPosY, targetPosZ, 1.0F);
    }

    public void guidanceToTarget(double targetPosX, double targetPosY, double targetPosZ, float accelerationFactor) {
        if (getInfo().tickEndHoming > 0 && ticksExisted > getInfo().tickEndHoming) return;
        if (targetEntity == null || targetEntity.isDead) return;

        if (getInfo().predictTargetPos) {
            double currentDistance = Math.sqrt(getDistanceSq(targetPosX, targetPosY, targetPosZ));
            double missileSpeed = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);

            if (missileSpeed < 0.0001D) missileSpeed = this.acceleration;

            double timeToTarget = currentDistance / missileSpeed;
            targetPosX += targetEntity.motionX * timeToTarget;
            targetPosY += targetEntity.motionY * timeToTarget;
            targetPosZ += targetEntity.motionZ * timeToTarget;
        }

        double tx = targetPosX - this.posX;
        double ty = targetPosY - this.posY;
        double tz = targetPosZ - this.posZ;
        double d = Math.sqrt(tx * tx + ty * ty + tz * tz);

        double mx = tx * this.acceleration / d;
        double my = ty * this.acceleration / d;
        double mz = tz * this.acceleration / d;

        Vector3f missileDir = new Vector3f((float) motionX, (float) motionY, (float) motionZ);
        Vector3f targetDir = new Vector3f((float) tx, (float) ty, (float) tz);

        float angle = missileDir.angle(targetDir);
        if (angle > Math.toRadians(getInfo().maxDegreeOfMissile) && !doingTopAttack) {
            setTargetEntity(null);
            return;
        }

        Vector3f targetVel = new Vector3f((float) targetEntity.motionX, (float) targetEntity.motionY, (float) targetEntity.motionZ);
        if (missileDir.angle(targetVel) > Math.toRadians(getInfo().pdHDNMaxDegree)) {
            setTargetEntity(null);
            return;
        }

        if (this instanceof MCH_EntityAAMissile && MCH_WeaponGuidanceSystem.isEntityOnGround(targetEntity, weaponInfo.lockMinHeight)) {
            setTargetEntity(null);
            return;
        }

        double factor = getInfo().turningFactor;
        this.motionX += (mx - this.motionX) * factor;
        this.motionY += (my - this.motionY) * factor;
        this.motionZ += (mz - this.motionZ) * factor;

        this.rotationYaw = (float) (Math.atan2(motionZ, motionX) * 180.0D / Math.PI) - 90.0F;
        double r = Math.sqrt(motionX * motionX + motionZ * motionZ);
    }

    public boolean checkValid() {
        if (this.shootingEntity == null && this.shootingAircraft == null) {
            return false;
        } else {
            Entity shooter = this.shootingAircraft != null && this.shootingAircraft.isDead && this.shootingEntity == null ? this.shootingAircraft : this.shootingEntity;
            double x = this.posX - shooter.posX;
            double z = this.posZ - shooter.posZ;
            return x * x + z * z < 3.38724E7 && this.posY > -10.0;
        }
    }

    public float getGravity() {
        return this.getInfo() != null ? this.getInfo().gravity : 0.0F;
    }

    public float getGravityInWater() {
        return this.getInfo() != null ? this.getInfo().gravityInWater : 0.0F;
    }

    public void onUpdate() {
        if (!this.world.isRemote) {
            if (this.getInfo() != null && this.shootingAircraft instanceof MCH_EntityAircraft ac && !this.speedAddedFromAircraft && this.getInfo().speedDependsAircraft) {
                double s = Math.sqrt(ac.motionX * ac.motionX + ac.motionY * ac.motionY + ac.motionZ * ac.motionZ);
                this.acceleration += s;
                double d = Math.sqrt(this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ);
                if (d > 0) {
                    this.motionX = (this.motionX * this.acceleration) / d;
                    this.motionY = (this.motionY * this.acceleration) / d;
                    this.motionZ = (this.motionZ * this.acceleration) / d;
                }
                this.speedAddedFromAircraft = true;
            }

            if (this.antiFlareUse) {
                if (this.antiFlareTick > 0) {
                    this.antiFlareTick--;
                } else {
                    this.setTargetEntity(null);
                    this.antiFlareUse = false;
                }
            }
        }

        if (!this.world.isRemote && this.isChunkLoadingEnabled()) {
            ProjectileChunkLoader loader = ProjectileChunkLoader.instance();
            if (loader != null) {
                loader.requestPath(this);
            }
        }

        if (this.world.isRemote && this.countOnUpdate == 0) {
            int tgtEttId = this.getTargetEntityID();
            if (tgtEttId > 0) {
                this.setTargetEntity(this.world.getEntityByID(tgtEttId));
            }
        }

        if (!this.world.isRemote && this.getCountOnUpdate() % 20 == 19 && this.targetEntity instanceof EntityPlayerMP) {
            String format = "MCH_EntityBaseBullet.onUpdate alert" + this.targetEntity + " / " + this.targetEntity.getRidingEntity();
            MCH_Logger.debugLog(this.world, format);

            var ridingEntity = this.targetEntity.getRidingEntity();
            if (ridingEntity != null && !(ridingEntity instanceof MCH_EntityAircraft) && !(ridingEntity instanceof MCH_EntitySeat)) {
                W_WorldFunc.playSoundAt(this.targetEntity, "alert", 2.0F, 1.0F);
            }
        }

        this.prevMotionX = this.motionX;
        this.prevMotionY = this.motionY;
        this.prevMotionZ = this.motionZ;
        this.prevPosX2 = this.prevPosX;
        this.prevPosY2 = this.prevPosY;
        this.prevPosZ2 = this.prevPosZ;

        if (++this.countOnUpdate > 10_000_000) {
            this.clearCountOnUpdate();
        }

        super.onUpdate();

        boolean motionChanged = this.prevMotionX != this.motionX || this.prevMotionY != this.motionY || this.prevMotionZ != this.motionZ;
        if (motionChanged && (this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ > 0.1)) {
            this.rotationYaw = (float) Math.toDegrees(Math.atan2(this.motionZ, this.motionX)) - 90.0F;
            double r = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
            this.rotationPitch = -(float) Math.toDegrees(Math.atan2(this.motionY, r));
        }

        if (this.getInfo() == null) {
            if (this.countOnUpdate >= 2) {
                MCH_Logger.log(this, "##### MCH_EntityBaseBullet onUpdate() Weapon info null %d, %s, Name=%s", W_Entity.getEntityId(this), this.getEntityName(), this.getName());
                this.setDead();
                return;
            }
            this.setName(this.getName());
            if (this.getInfo() == null) return;
        }

        if (this.getInfo().bound <= 0.0F && this.onGround) {
            this.motionX *= 0.9;
            this.motionZ *= 0.9;
        }

        if (this.world.isRemote && this.isBomblet < 0) {
            this.isBomblet = this.getBomblet();
        }

        if (!this.world.isRemote) {
            var blockpos = new BlockPos(this.posX, this.posY, this.posZ);
            if (this.posY <= 255 && !this.world.isBlockLoaded(blockpos)) {
                if (this.getInfo().delayFuse <= 0) {
                    this.setDead();
                    return;
                }
                if (this.delayFuse == 0) {
                    this.delayFuse = this.getInfo().delayFuse;
                }
            }

            if (this.delayFuse > 0 && --this.delayFuse == 0) {
                this.onUpdateTimeout();
                this.setDead();
                return;
            }

            if (!this.checkValid() || (this.getInfo().timeFuse > 0 && this.getCountOnUpdate() > this.getInfo().timeFuse)) {
                if (this.getInfo().timeFuse > 0) this.onUpdateTimeout();
                this.setDead();
                return;
            }

            if (this.getInfo().explosionAltitude > 0 && MCH_Lib.getBlockIdY(this, 3, -this.getInfo().explosionAltitude) != 0) {
                var mop = new RayTraceResult(new Vec3d(this.posX, this.posY, this.posZ), EnumFacing.DOWN, blockpos);
                this.onImpact(mop, 1.0F);
            }
        }

        if (!this.isInWater()) {
            if (this.ticksExisted > this.getInfo().speedFactorStartTick && this.ticksExisted < this.getInfo().speedFactorEndTick) {
                double currentSpeed = Math.sqrt(this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ);
                if (currentSpeed > 0) {
                    this.motionX += (this.motionX / currentSpeed) * this.getInfo().speedFactor;
                    this.motionY += (this.motionY / currentSpeed) * this.getInfo().speedFactor;
                    this.motionZ += (this.motionZ / currentSpeed) * this.getInfo().speedFactor;
                    this.acceleration += this.getInfo().speedFactor;
                }
            }
            this.motionY += this.getGravity();
            // Reforged: horizontal air drag (slows the projectile along its travel direction).
            double dragInAir = this.getInfo().dragInAir;
            if (dragInAir != 0.0) {
                double speed = Math.sqrt(this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ);
                if (speed > 0.0) {
                    this.motionX -= (this.motionX / speed) * dragInAir;
                    this.motionZ -= (this.motionZ / speed) * dragInAir;
                }
            }
        } else {
            this.motionY += this.getGravityInWater();
        }

        if (!this.isDead) {
            this.onUpdateCollided();
            this.onUpdateAirburst();
            this.onUpdateProximityFuse();
        }

        this.posX += this.motionX * this.accelerationFactor;
        this.posY += this.motionY * this.accelerationFactor;
        this.posZ += this.motionZ * this.accelerationFactor;

        if (this.world.isRemote) {
            this.updateSplash();
            if (this.isInWater()) {
                this.world.spawnParticle(EnumParticleTypes.WATER_BUBBLE, this.posX - this.motionX * 0.25D, this.posY - this.motionY * 0.25D, this.posZ - this.motionZ * 0.25D, this.motionX, this.motionY, this.motionZ);
            }
        }

        this.setPosition(this.posX, this.posY, this.posZ);

        this.onUpdateSpreader();
    }

    private void onUpdateAirburst() {
        int abDist = this.airburstDist;
        if (this.airburstTriggered || abDist <= 5 || abDist >= 3000) return;

        double targetDist = abDist + 3.0D;
        double dx = this.motionX * this.accelerationFactor;
        double dy = this.motionY * this.accelerationFactor;
        double dz = this.motionZ * this.accelerationFactor;

        double segLen = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double newTravel = this.airburstTravelled + segLen;

        if (segLen > 0.0D && newTravel >= targetDist) {
            double t = (targetDist - this.airburstTravelled) / segLen;
            double ex = this.posX + dx * t;
            double ey = this.posY + dy * t;
            double ez = this.posZ + dz * t;

            if (!this.world.isRemote) {
                // AHEAD rounds don't detonate at the airburst point: they arm and then stream
                // sub-projectiles forward (see onUpdateSpreader). The proximityFuseTick gate
                // keeps them from arming before the configured flight time.
                if (Objects.requireNonNull(this.getInfo()).ahead) {
                    if (this.getInfo().proximityFuseTick < 0 || this.ticksExisted > this.getInfo().proximityFuseTick) {
                        this.aheadTriggered = true;
                    }
                    this.airburstTriggered = true;
                    this.airburstTravelled = 0.0D;
                    return;
                }
                if (this.getInfo().explosion > 0) {
                    this.newExplosion(ex, ey, ez, this.getInfo().explosionAirburst, (float) this.getInfo().explosionBlock, false);
                } else if (this.explosionPower < 0) {
                    this.playExplosionSound();
                }

                if (this.getInfo() != null) {
                    PacketClientSound.sendSoundPacket(ex, ey, ez, this.getInfo().hitSoundRange, this.world, this.getInfo().hitSound != null ? this.getInfo().hitSound.toString() : null, true);
                }
                this.setDead();
            }

            this.airburstTriggered = true;
            this.airburstTravelled = 0.0D;
        } else {
            this.airburstTravelled = newTravel;
        }
    }

    /**
     * Proximity ("near-burst") fuse: detonates near aircraft (and locked-on missile targets) using a
     * predictive intercept along this frame's travel segment. Ported from Reforged. AHEAD-capable
     * rounds only proximity-trigger once a (radar or manual) airburst solution exists.
     */
    private void onUpdateProximityFuse() {
        if (this.isDead || this.getInfo() == null) return;
        if (this.getInfo().proximityFuseTick < 0 || this.ticksExisted <= this.getInfo().proximityFuseTick) return;
        if (this.getInfo().proximityFuseDist <= 0.0F) return;

        if (this.getInfo().ahead) {
            int abDist = this.airburstDist;
            if (abDist <= 5 || abDist >= 3000) return;
        }

        float searchRange = this.getInfo().proximityFuseDist * 5.0F;
        List<Entity> nearby = this.world.getEntitiesWithinAABBExcludingEntity(this, this.getEntityBoundingBox().grow(searchRange));
        if (nearby.isEmpty()) return;

        double dx = this.motionX * this.accelerationFactor;
        double dy = this.motionY * this.accelerationFactor;
        double dz = this.motionZ * this.accelerationFactor;
        double segLen = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (segLen <= 0.0D) return;

        double dirX = dx / segLen;
        double dirY = dy / segLen;
        double dirZ = dz / segLen;

        for (Entity entity : nearby) {
            if (!this.canBeCollidedEntity(entity)) continue;

            boolean isAircraftTarget = entity instanceof MCH_EntityAircraft;
            boolean isLockedTarget = entity instanceof MCH_EntityBaseBullet
                    && this.targetEntity != null && !this.targetEntity.isDead
                    && W_Entity.isEqual(entity, this.targetEntity);
            if (!isAircraftTarget && !isLockedTarget) continue;
            if (isAircraftTarget && MCH_WeaponGuidanceSystem.isEntityOnGround(entity, this.getInfo().proximityFuseHeight))
                continue;

            // Lead the target by its velocity over the (rough) time-to-intercept.
            double currentDistance = Math.sqrt(this.getDistanceSq(entity.posX, entity.posY, entity.posZ));
            double timeToPos = currentDistance / segLen;
            double predX = entity.posX + entity.motionX * timeToPos;
            double predY = entity.posY + entity.motionY * timeToPos;
            double predZ = entity.posZ + entity.motionZ * timeToPos;

            double toPredX = predX - this.posX;
            double toPredY = predY - this.posY;
            double toPredZ = predZ - this.posZ;
            double dot = toPredX * dirX + toPredY * dirY + toPredZ * dirZ;
            if (dot <= 0.0D) continue;

            double projX = this.posX + dirX * dot;
            double projY = this.posY + dirY * dot;
            double projZ = this.posZ + dirZ * dot;
            double perpX = predX - projX;
            double perpY = predY - projY;
            double perpZ = predZ - projZ;
            double predictedDistance = Math.sqrt(perpX * perpX + perpY * perpY + perpZ * perpZ);
            if (predictedDistance > this.getInfo().proximityFuseDist) continue;

            double distToProj = Math.sqrt((projX - this.posX) * (projX - this.posX)
                    + (projY - this.posY) * (projY - this.posY) + (projZ - this.posZ) * (projZ - this.posZ));
            if (distToProj > segLen + 0.1D) continue;

            double t = Math.max(0.0D, Math.min(1.0D, distToProj / segLen));
            double ex = this.posX + dx * t;
            double ey = this.posY + dy * t;
            double ez = this.posZ + dz * t;

            if (!this.world.isRemote) {
                if (this.getInfo().explosion > 0) {
                    this.newExplosion(ex, ey, ez, this.getInfo().explosionAirburst, (float) this.getInfo().explosionBlock, false);
                } else if (this.explosionPower < 0) {
                    this.playExplosionSound();
                }
                PacketClientSound.sendSoundPacket(ex, ey, ez, this.getInfo().hitSoundRange, this.world, this.getInfo().hitSound != null ? this.getInfo().hitSound.toString() : null, true);

                if (!entity.isDead) {
                    MCH_Lib.applyEntityHurtResistantTimeConfig(entity);
                    // CE's newExplosion is void (no ExplosionResult), so the direct proximity-fuse
                    // damage uses the standard thrown-damage source rather than an explosion source.
                    DamageSource ds = DamageSource.causeThrownDamage(this, this.shootingEntity);
                    float damage = MCH_Config.applyDamageVsEntity(entity, ds, this.getInfo().proximityFuseDamage);
                    damage *= this.getInfo().getDamageFactor(entity);
                    entity.attackEntityFrom(ds, damage);
                    if (isLockedTarget) ((MCH_EntityBaseBullet) entity).setDead();
                    if (damage > 0.0F) this.notifyHitBullet();
                }
                this.setDead();
            }
            return;
        }
    }

    /**
     * Streams sub-projectiles forward, either as a generic in-air spawner ({@code spawnBulletInAir})
     * or as AHEAD cluster-burst slugs after {@link #aheadTriggered} arms. Each child is a plain
     * {@link MCH_EntityBullet} carrying the configured {@code bombletModelName} weapon. Ported from
     * Reforged's onUpdateSpreader (CE has no MCH_WeaponCreator.createEntity, so children are bullets).
     */
    private void onUpdateSpreader() {
        if (this.world.isRemote || this.getInfo() == null) return;

        boolean canSpawnInAir = this.getInfo().spawnBulletInAir;
        boolean canSpawnAhead = this.getInfo().ahead && this.aheadTriggered;
        if ((canSpawnInAir || canSpawnAhead) && this.spawnedBulletNum < this.getInfo().spawnBulletMaxNum && !this.isDead) {
            if (this.ticksExisted > 5 && this.ticksExisted % this.getInfo().spawnBulletIntervalTick == 0) {
                this.spawnedBulletNum++;
                MCH_WeaponInfo info = MCH_WeaponInfoManager.get(this.getInfo().bombletModelName);
                if (info != null) {
                    for (int i = 0; i < this.getInfo().spawnBulletPerNum; i++) {
                        double mX = 1.0E-6D;
                        double mY = 1.0E-6D;
                        double mZ = 1.0E-6D;
                        double speed = 0.001D;
                        if (this.getInfo().spawnBulletInheritSpeed) {
                            mX = this.motionX;
                            mY = this.motionY;
                            mZ = this.motionZ;
                            speed = this.acceleration;
                        }
                        MCH_EntityBullet e = new MCH_EntityBullet(this.world, this.posX, this.posY, this.posZ, mX, mY, mZ, this.rotationYaw, this.rotationPitch, speed);
                        e.setName(this.getInfo().bombletModelName);
                        e.setParameterFromWeapon(this, this.shootingAircraft, this.shootingEntity);
                        // Use the bomblet's own warhead stats; never re-airburst.
                        e.setPower(info.power);
                        e.explosionPower = info.explosion;
                        e.explosionPowerInWater = info.explosionInWater;
                        e.airburstDist = 0;
                        float spread = this.getInfo().bombletDiff;
                        e.motionX += (this.rand.nextFloat() - 0.5D) * spread;
                        e.motionY += (this.rand.nextFloat() - 0.5D) * spread;
                        e.motionZ += (this.rand.nextFloat() - 0.5D) * spread;
                        this.world.spawnEntity(e);
                    }
                }
            }
        }

        if (this.getInfo().destructAfterSpawnBullet && this.spawnedBulletNum >= this.getInfo().spawnBulletMaxNum) {
            this.setDead();
        }
    }

    public void updateSplash() {
        if (this.getInfo() != null) {
            if (this.getInfo().power > 0) {
                if (!W_WorldFunc.isBlockWater(this.world, (int) (this.prevPosX + 0.5), (int) (this.prevPosY + 0.5), (int) (this.prevPosZ + 0.5)) && W_WorldFunc.isBlockWater(this.world, (int) (this.posX + 0.5), (int) (this.posY + 0.5), (int) (this.posZ + 0.5))) {
                    double x = this.posX - this.prevPosX;
                    double y = this.posY - this.prevPosY;
                    double z = this.posZ - this.prevPosZ;
                    double d = Math.sqrt(x * x + y * y + z * z);
                    if (d <= 0.15) {
                        return;
                    }

                    x /= d;
                    y /= d;
                    z /= d;
                    double px = this.prevPosX;
                    double py = this.prevPosY;
                    double pz = this.prevPosZ;

                    for (int i = 0; i <= d; i++) {
                        px += x;
                        py += y;
                        pz += z;
                        if (W_WorldFunc.isBlockWater(this.world, (int) (px + 0.5), (int) (py + 0.5), (int) (pz + 0.5))) {
                            float pwr = this.getInfo().power < 20 ? this.getInfo().power : 20.0F;
                            int n = this.rand.nextInt(1 + (int) pwr / 3) + (int) pwr / 2 + 1;
                            pwr *= 0.03F;

                            for (int j = 0; j < n; j++) {
                                MCH_ParticleParam prm = new MCH_ParticleParam(this.world, "splash", px, py + 0.5, pz, pwr * (this.rand.nextDouble() - 0.5) * 0.3, pwr * (this.rand.nextDouble() * 0.5 + 0.5) * 1.8, pwr * (this.rand.nextDouble() - 0.5) * 0.3, pwr * 5.0F);
                                MCH_ParticlesUtil.spawnParticle(prm);
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    public void onUpdateTimeout() {
        if (this.isInWater()) {
            if (this.explosionPowerInWater > 0) {
                this.newExplosion(this.posX, this.posY, this.posZ, this.explosionPowerInWater, this.explosionPowerInWater, true);
            }
        } else if (this.explosionPower > 0) {
            this.newExplosion(this.posX, this.posY, this.posZ, this.explosionPower, this.getInfo().explosionBlock, false);
        } else if (this.explosionPower < 0) {
            this.playExplosionSound();
        }
    }

    public void onUpdateBomblet() {
        if (!this.world.isRemote && this.sprinkleTime > 0 && !this.isDead) {
            this.sprinkleTime--;
            if (this.sprinkleTime == 0) {
                for (int i = 0; i < this.getInfo().bomblet; i++) {
                    this.sprinkleBomblet();
                }

                this.setDead();
            }
        }
    }

    public void boundBullet(EnumFacing sideHit) {
        switch (sideHit) {
            case DOWN -> {
                if (this.motionY > 0.0) {
                    this.motionY = -this.motionY * this.getInfo().bound;
                }
            }
            case UP -> {
                if (this.motionY < 0.0) {
                    this.motionY = -this.motionY * this.getInfo().bound;
                }
            }
            case NORTH -> {
                if (this.motionZ > 0.0) {
                    this.motionZ = -this.motionZ * this.getInfo().bound;
                } else {
                    this.posZ = this.posZ + this.motionZ;
                }
            }
            case SOUTH -> {
                if (this.motionZ < 0.0) {
                    this.motionZ = -this.motionZ * this.getInfo().bound;
                } else {
                    this.posZ = this.posZ + this.motionZ;
                }
            }
            case WEST -> {
                if (this.motionX > 0.0) {
                    this.motionX = -this.motionX * this.getInfo().bound;
                } else {
                    this.posX = this.posX + this.motionX;
                }
            }
            case EAST -> {
                if (this.motionX < 0.0) {
                    this.motionX = -this.motionX * this.getInfo().bound;
                } else {
                    this.posX = this.posX + this.motionX;
                }
            }
        }

        if (this.getInfo().bound <= 0.0F) {
            this.motionX *= 0.25;
            this.motionY *= 0.25;
            this.motionZ *= 0.25;
        }
    }

    protected void onUpdateCollided() {
        float damageFator = 1.0F;
        double mx = this.motionX * this.accelerationFactor;
        double my = this.motionY * this.accelerationFactor;
        double mz = this.motionZ * this.accelerationFactor;
        RayTraceResult m = null;

        for (int i = 0; i < 5; i++) {
            Vec3d vec3 = new Vec3d(this.posX, this.posY, this.posZ);
            Vec3d vec31 = new Vec3d(this.posX + mx, this.posY + my, this.posZ + mz);
            m = W_WorldFunc.clip(this.world, vec3, vec31);
            boolean continueClip = false;
            if (this.shootingEntity != null && m != null && m.typeOfHit == RayTraceResult.Type.BLOCK) {
                BlockPos blockpos1 = m.getBlockPos();
                Block block = this.world.getBlockState(blockpos1).getBlock();
                if (MCH_Config.bulletBreakableBlocks.contains(block)) {
                    BlockPos blockpos = m.getBlockPos();
                    this.world.destroyBlock(blockpos, true);
                    continueClip = true;
                }
            }

            if (!continueClip) {
                break;
            }
        }

        Vec3d vec3x = new Vec3d(this.posX, this.posY, this.posZ);
        Vec3d vec31x = new Vec3d(this.posX + mx, this.posY + my, this.posZ + mz);
        if (this.getInfo().delayFuse > 0) {
            if (m != null) {
                this.boundBullet(m.sideHit);
                if (this.delayFuse == 0) {
                    this.delayFuse = this.getInfo().delayFuse;
                }
            }
        } else {
            if (m != null) {
                vec31x = new Vec3d(m.hitVec.x, m.hitVec.y, m.hitVec.z);
            }

            Entity entity = null;
            List<Entity> list = this.world.getEntitiesWithinAABBExcludingEntity(this, this.getEntityBoundingBox().expand(mx, my, mz).grow(21.0, 21.0, 21.0));
            double d0 = 0.0;

            for (Entity entity1 : list) {
                if (this.canBeCollidedEntity(entity1)) {
                    float f = 0.3F;
                    AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().grow(f, f, f);
                    RayTraceResult m1 = axisalignedbb.calculateIntercept(vec3x, vec31x);
                    if (m1 != null) {
                        double d1 = vec3x.distanceTo(m1.hitVec);
                        if (d1 < d0 || d0 == 0.0) {
                            entity = entity1;
                            d0 = d1;
                        }
                    }
                }
            }

            if (entity != null) {
                m = new RayTraceResult(entity);
            }

            if (m != null) {
                this.onImpact(m, damageFator);
            }
        }
    }

    public boolean canBeCollidedEntity(Entity entity) {
        if (entity == null || !entity.canBeCollidedWith()) return false;
        // Type-based exclusions
        boolean isExcluded = switch (entity) {
            case MCH_EntityChain _, MCH_EntitySeat _, MCH_EntityHitBox _ -> true;

            case MCH_EntityBaseBullet blt -> {
                if (world.isRemote) yield true;
                if (W_Entity.isEqual(blt.shootingAircraft, this.shootingAircraft)) yield true;
                yield W_Entity.isEqual(blt.shootingEntity, this.shootingEntity);
            }

            default -> false;
        };

        if (isExcluded) return false;

        // Relationship-based exclusions
        if (W_Entity.isEqual(entity, this.shootingEntity)) return false;

        if (this.shootingAircraft instanceof MCH_EntityAircraft ac) {
            if (W_Entity.isEqual(entity, ac) || ac.isMountedEntity(entity)) return false;
        }

        // Config-based exclusions
        String className = entity.getClass().getName().toLowerCase();
        for (String s : MCH_Config.IgnoreBulletHitList) {
            if (className.contains(s.toLowerCase())) return false;
        }

        return true;
    }

    public void notifyHitBullet() {
        if (this.shootingAircraft instanceof MCH_EntityAircraft && W_EntityPlayer.isPlayer(this.shootingEntity)) {
            PacketNotifyHit.send((MCH_EntityAircraft) this.shootingAircraft, (EntityPlayerMP) this.shootingEntity);
        }

        if (W_EntityPlayer.isPlayer(this.shootingEntity)) {
            PacketNotifyHit.send(null, (EntityPlayerMP) this.shootingEntity);
        }
    }


    @SideOnly(Side.CLIENT)
    public void spawnBlockPar(RayTraceResult raytraceResult, BlockPos blockPos) {

        var mc = Minecraft.getMinecraft();
        var effectRenderer = mc.effectRenderer;
        var state = world.getBlockState(blockPos);

        int crackCount = getInfo().flakParticlesCrack + rand.nextInt(3);
        for (int i = 0; i < crackCount; i++) {
            var fx = (ParticleDigging) effectRenderer.spawnEffectParticle(EnumParticleTypes.BLOCK_CRACK.getParticleID(), raytraceResult.hitVec.x + (rand.nextFloat() - 0.5D) * width, raytraceResult.hitVec.y + 0.1D, raytraceResult.hitVec.z + (rand.nextFloat() - 0.5D) * width, 0, 0, 0, Block.getStateId(state));

            if (fx != null) {
                float diff = getInfo().flakParticlesDiff;
                fx.motionX += (diff / 2.0F) * rand.nextGaussian();
                fx.motionZ += (diff / 2.0F) * rand.nextGaussian();
                fx.motionY += diff * Math.abs(rand.nextGaussian());
                fx.multipleParticleScaleBy(1.0F);
            }
        }

        // Cloud/Smoke particles
        for (int i = 0; i < getInfo().numParticlesFlak; i++) {
            effectRenderer.spawnEffectParticle(EnumParticleTypes.CLOUD.getParticleID(), raytraceResult.hitVec.x + rand.nextGaussian(), raytraceResult.hitVec.y + rand.nextGaussian(), raytraceResult.hitVec.z + rand.nextGaussian(), rand.nextGaussian() / 200.0D, rand.nextGaussian() / 200.0D, rand.nextGaussian() / 200.0D);


        }
    }

    protected void onImpact(RayTraceResult m, float damageFactor) {
        if (this.world.isRemote) {
            if (getInfo() != null) {
                if (m.entityHit == null) {
                    spawnBlockPar(m, m.getBlockPos());
                } else if (m.entityHit instanceof MCH_EntityAircraft ac && ac.ironCurtainRunningTick > 0) {
                    spawnIronCurtainParticle(m, m.getBlockPos());
                }
            }
            return;
        }

        if (m.entityHit != null) {
            if (m.entityHit instanceof MCH_EntityBaseBullet && !Objects.requireNonNull(this.getInfo()).canBeIntercepted)
                return;
            if (m.entityHit instanceof MCH_EntityFlare || m.entityHit instanceof MCH_EntityChaff) return;

            this.onImpactEntity(m.entityHit, damageFactor);
            this.piercing--;
        }

        if (m.typeOfHit == RayTraceResult.Type.BLOCK) {
            IBlockState state = this.world.getBlockState(m.getBlockPos());
            Block block = state.getBlock();
            Material mat = state.getMaterial();
            if (mat == Material.LEAVES || mat == Material.PLANTS || block == Blocks.IRON_BARS || block instanceof BlockDoublePlant) {
                return;
            }
        }

        float p = (float) this.explosionPower * damageFactor;
        float i = (float) this.explosionPowerInWater * damageFactor;

        if (this.piercing > 0) {
            this.piercing--;
            if (p > 0.0F) {
                this.newExplosion(m.hitVec.x, m.hitVec.y, m.hitVec.z, 1.0F, 1.0F, false);
            }
        } else {
            if (i == 0.0F) {
                if (Objects.requireNonNull(this.getInfo()).isFAE) {
                    this.newFAExplosion(this.posX, this.posY, this.posZ, p, (float) this.getInfo().explosionBlock);
                } else if (p > 0.0F) {
                    this.newExplosion(m.hitVec.x, m.hitVec.y, m.hitVec.z, p, (float) this.getInfo().explosionBlock, false);
                } else if (p < 0.0F) {
                    this.playExplosionSound();
                }
            } else if (m.entityHit != null) {
                if (this.isInWater()) {
                    this.newExplosion(m.hitVec.x, m.hitVec.y, m.hitVec.z, i, i, true);
                } else {
                    this.newExplosion(m.hitVec.x, m.hitVec.y, m.hitVec.z, p, (float) Objects.requireNonNull(this.getInfo()).explosionBlock, false);
                }
            } else if (!this.isInWater() && !MCH_Lib.isBlockInWater(this.world, m.getBlockPos())) {
                if (p > 0.0F) {
                    this.newExplosion(m.hitVec.x, m.hitVec.y, m.hitVec.z, p, (float) Objects.requireNonNull(this.getInfo()).explosionBlock, false);
                } else if (p < 0.0F) {
                    this.playExplosionSound();
                }
            } else {
                this.newExplosion(m.getBlockPos().getX(), m.getBlockPos().getY(), m.getBlockPos().getZ(), i, i, true);
            }

            if (getInfo() != null) {
                PacketClientSound.sendSoundPacket(this.posX, this.posY, this.posZ, getInfo().hitSoundRange, this.world, getInfo().hitSound != null ? getInfo().hitSound.toString() : null, true);
            }

            this.setDead();
        }
    }

    @SideOnly(Side.CLIENT)
    public void spawnIronCurtainParticle(RayTraceResult result, BlockPos pos) {
        final float r = 0.5f, g = 0.1f, b = 0.1f;
        var effectRenderer = Minecraft.getMinecraft().effectRenderer;
        var state = world.getBlockState(pos);

        int crackNum = Objects.requireNonNull(getInfo()).flakParticlesCrack + rand.nextInt(3);
        for (int i = 0; i < crackNum; i++) {
            var fx = (ParticleDigging) effectRenderer.spawnEffectParticle(EnumParticleTypes.BLOCK_CRACK.getParticleID(), result.hitVec.x + (rand.nextFloat() - 0.5D) * width, result.hitVec.y + 0.1D, result.hitVec.z + (rand.nextFloat() - 0.5D) * width, 0, 0, 0, Block.getStateId(state));

            if (fx != null) {
                fx.setRBGColorF(r, g, b);
                fx.multipleParticleScaleBy(0.8f);
                fx.motionX += getInfo().flakParticlesDiff * (rand.nextGaussian() * 0.5);
                fx.motionZ += getInfo().flakParticlesDiff * (rand.nextGaussian() * 0.5);
                fx.motionY += getInfo().flakParticlesDiff * Math.abs(rand.nextGaussian());
            }
        }

        int cloudNum = 50 + (int) getInfo().flakParticlesDiff;
        for (int i = 0; i < cloudNum; i++) {
            var cloud = (ParticleCloud) effectRenderer.spawnEffectParticle(EnumParticleTypes.CLOUD.getParticleID(), result.hitVec.x + (rand.nextFloat() - 0.5D) * width, result.hitVec.y + rand.nextGaussian() * height, result.hitVec.z + (rand.nextFloat() - 0.5D) * width, rand.nextGaussian() / 100, rand.nextGaussian() / 100, rand.nextGaussian() / 100);

            if (cloud != null) {
                cloud.setRBGColorF(r, g, b);
            }
        }
    }

    public void onImpactEntity(Entity entity, float damageFactor) {
        if (!entity.isDead) {
            MCH_Logger.debugLog(this.world, "MCH_EntityBaseBullet.onImpactEntity:Damage=%d:" + entity.getClass(), this.getPower());
            MCH_Lib.applyEntityHurtResistantTimeConfig(entity);
            DamageSource ds = DamageSource.causeThrownDamage(this, this.shootingEntity);
            float damage = MCH_Config.applyDamageVsEntity(entity, ds, this.getPower() * damageFactor);
            damage *= this.getInfo() != null ? this.getInfo().getDamageFactor(entity) : 1.0F;
            entity.attackEntityFrom(ds, damage);
            if (this instanceof MCH_EntityBullet && entity instanceof EntityVillager && this.shootingEntity != null && this.shootingEntity instanceof EntityPlayerMP && this.shootingEntity.getRidingEntity() instanceof MCH_EntitySeat) {
                MCH_CriteriaTriggers.VILLAGER_HURT_BULLET.trigger((EntityPlayerMP) this.shootingEntity);
            }

        }

        this.notifyHitBullet();
    }

    public void newFAExplosion(double x, double y, double z, float exp, float expBlock) {
        MCH_Explosion.ExplosionResult result = MCH_Explosion.newExplosion(this.world, this, this.shootingEntity, x, y, z, exp, expBlock, true, true, this.getInfo().flaming, false, 15);
        if (result != null && result.hitEntity) {
            this.notifyHitBullet();
        }
    }

    public void newExplosion(double x, double y, double z, float exp, float expBlock, boolean inWater) {
        MCH_Explosion.ExplosionResult result;

        if (isLoaded(MODID_HBM) && getInfo().useHBM) {
            processHBMExplosion(x, y, z, exp, expBlock);
        }
        if (!getInfo().effectOnly) {
            if (!inWater) {
                result = MCH_Explosion.newExplosion(this.world, this, this.shootingEntity, x, y, z, exp, expBlock, this.rand.nextInt(3) == 0, true, this.getInfo().flaming, true, 0, this.getInfo() != null ? this.getInfo().damageFactor : null);
            } else {
                result = MCH_Explosion.newExplosionInWater(this.world, this, this.shootingEntity, x, y, z, exp, expBlock, this.rand.nextInt(3) == 0, true, this.getInfo().flaming, true, 0, this.getInfo() != null ? this.getInfo().damageFactor : null);
            }

            if (result != null && result.hitEntity) {
                this.notifyHitBullet();
            }
        }
    }

    public void playExplosionSound() {
        MCH_ExplosionV2.playExplosionSound(this.world, this.posX, this.posY, this.posZ);
    }

    private void processHBMExplosion(double x, double y, double z, float exp, float expBlock) {
        final boolean effectOnly = getInfo().effectOnly;
        switch (getInfo().payloadNTM) {
            case NONE -> {
            }
            case NTM_VNT -> {
                if (getInfo().vntSettingContainer == null) break;
                var vnt = getInfo().vntSettingContainer;
                vnt.buildExplosion(this.world, x, y, z, expBlock, this, effectOnly);

            }
            case NTM_NT -> {
                if (getInfo().ntSettingContainer == null) break;
                var nt = getInfo().ntSettingContainer;
                if (!effectOnly) nt.explode(this.world, this, x, y, z, (int) expBlock);

            }
            case NTM_MINI_NUKE -> {
                if (getInfo().mukeContainer == null) break;
                var muke = getInfo().mukeContainer;
                muke.explode(world, x, y, z, effectOnly);
            }
            case NTM_NUKE -> {
                HBMUtil.nukeMK5(this.world, (int) expBlock, x, y, z, effectOnly);
            }
            case NTM_CHEMICAL -> {
                HBMUtil.spawnChlorine(world, x, y, z, getInfo().chemicalContainer);
            }
            case NTM_MIST -> {
                if (getInfo().mistContainer == null) break;
                var mist = getInfo().mistContainer;
                if (!effectOnly) mist.execute(world, x, y, z);
            }
            case NTM_FIRE -> {
                if (getInfo().fireContainer == null) break;
                getInfo().fireContainer.execute(world, x, y, z, effectOnly);
            }
        }
    }

    public void writeEntityToNBT(NBTTagCompound par1NBTTagCompound) {
        par1NBTTagCompound.setTag("direction", this.newDoubleNBTList(this.motionX, this.motionY, this.motionZ));
        par1NBTTagCompound.setString("WeaponName", this.getName());
    }

    public void readEntityFromNBT(@NotNull NBTTagCompound par1NBTTagCompound) {
        this.setDead();
    }

    public boolean canBeCollidedWith() {
        return true;
    }

    public float getCollisionBorderSize() {
        return 1.0F;
    }

    @Override
    public boolean attackEntityFrom(DamageSource ds, float par2) {
        if (this.isEntityInvulnerable(ds)) {
            return false;
        } else if (!this.world.isRemote && par2 > 0.0F && ds.getDamageType().equalsIgnoreCase("thrown")) {
            this.markVelocityChanged();
            Vec3d pos = new Vec3d(this.posX, this.posY, this.posZ);
            RayTraceResult m = new RayTraceResult(pos, EnumFacing.DOWN, new BlockPos(pos));
            this.onImpact(m, 1.0F);
            return true;
        } else {
            return false;
        }
    }

    @SideOnly(Side.CLIENT)
    public float getShadowSize() {
        return 0.0F;
    }

    @Override
    public float getBrightness() {
        return 1.0F;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getBrightnessForRender() {
        return 15728880;
    }


    protected void scanForTargets() {
        if (numLockedChaff >= getInfo().numLockedChaffMax) {
            setTargetEntity(null);
            return;
        }

        final double range = getInfo().maxLockOnRange;
        final double maxAngleRad = Math.toRadians(getInfo().maxLockOnAngle);
        final Vec3d missileDir = new Vec3d(this.motionX, this.motionY, this.motionZ);

        List<Entity> entities = world.getEntitiesWithinAABB(Entity.class, getEntityBoundingBox().grow(range));

        if (entities.isEmpty()) return;

        Entity closestTarget = null;
        Entity nearestChaff = null;
        double minAngle = Double.MAX_VALUE;
        double minChaffDistSq = Double.MAX_VALUE;

        for (Entity entity : entities) {
            if (isFriendlyFire(entity)) continue;

            switch (this) {
                case MCH_EntityAAMissile aa -> {
                    if (entity instanceof MCH_EntityChaff) {
                        double distSq = handleChaff(entity, missileDir, maxAngleRad);
                        if (distSq < minChaffDistSq) {
                            minChaffDistSq = distSq;
                            nearestChaff = entity;
                        }
                    } else if (isValidAATarget(entity, missileDir, maxAngleRad)) {
                        double angle = calculateAngle(entity, missileDir);
                        if (angle < minAngle) {
                            minAngle = angle;
                            closestTarget = entity;
                        }
                    }
                }
                case MCH_EntityATMissile at -> {
                    if (isValidATTarget(entity, missileDir, maxAngleRad)) {
                        double angle = calculateAngle(entity, missileDir);
                        if (angle < minAngle) {
                            minAngle = angle;
                            closestTarget = entity;
                        }
                    }
                }
                default -> {
                }
            }
        }

        // Target Selection Priority: Chaff > Target
        if (nearestChaff != null) {
            this.targetEntity = nearestChaff;
            this.numLockedChaff++;
        } else {
            this.targetEntity = closestTarget;
        }
    }

    public void clientSetTargetEntity(Entity entity) {
        if (!super.world.isRemote) return;

        this.targetEntity = entity;
        if (entity != null) new PacketLockTarget(entity.getEntityId(), this.getEntityId()).sendToServer();
        else new PacketLockTarget(0, this.getEntityId()).sendToServer();
    }


    private boolean isFriendlyFire(Entity target) {
        if (W_Entity.isEqual(target, shootingAircraft) || W_Entity.isEqual(target, shootingEntity)) return true;
        if (shootingEntity instanceof EntityLivingBase && target.getControllingPassenger() instanceof EntityPlayer player) {
            return player.isOnSameTeam(shootingEntity);
        }
        if (shootingEntity instanceof EntityLivingBase shooter && target instanceof EntityLivingBase living) {
            return living.isOnSameTeam(shooter);
        }
        return false;
    }

    private double handleChaff(Entity chaff, Vec3d missileDir, double maxAngle) {
        double angle = calculateAngle(chaff, missileDir);
        return (angle <= maxAngle) ? getDistanceSq(chaff) : Double.MAX_VALUE;
    }

    private boolean isValidAATarget(Entity entity, Vec3d missileDir, double maxAngle) {
        if (!(entity instanceof MCH_EntityAircraft ac)) return false;
        if (ac.chaffUseTime > 0) return false;
        if (MCH_WeaponGuidanceSystem.isEntityOnGround(entity, getInfo().lockMinHeight)) return false;

        return calculateAngle(entity, missileDir) <= maxAngle;
    }

    private boolean isValidATTarget(Entity entity, Vec3d missileDir, double maxAngle) {
        boolean onGround = MCH_WeaponGuidanceSystem.isEntityOnGround(entity, getInfo().lockMinHeight);
        if (!onGround) return false;

        if (entity instanceof MCH_EntityAircraft) return calculateAngle(entity, missileDir) <= maxAngle;

        if (!getInfo().ridableOnly && entity instanceof EntityLivingBase && !entity.isRiding()) {
            return calculateAngle(entity, missileDir) <= maxAngle;
        }
        return false;
    }

    private double calculateAngle(Entity target, Vec3d missileDir) {
        Vec3d targetVec = new Vec3d(target.posX - this.posX, target.posY - this.posY, target.posZ - this.posZ);
        double dot = missileDir.normalize().dotProduct(targetVec.normalize());
        return Math.acos(MathHelper.clamp(dot, -1.0, 1.0));
    }
}
