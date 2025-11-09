package com.norwood.mcheli.weapon;

import static com.norwood.mcheli.compat.ModCompatManager.MODID_HBM;
import static com.norwood.mcheli.compat.ModCompatManager.isLoaded;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayerMP;
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

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_Explosion;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_EntityHitBox;
import com.norwood.mcheli.aircraft.MCH_EntitySeat;
import com.norwood.mcheli.chain.MCH_EntityChain;
import com.norwood.mcheli.compat.hbm.HBMUtil;
import com.norwood.mcheli.helper.MCH_CriteriaTriggers;
import com.norwood.mcheli.helper.world.MCH_ExplosionV2;
import com.norwood.mcheli.networking.packet.PacketNotifyHit;
import com.norwood.mcheli.particles.MCH_ParticleParam;
import com.norwood.mcheli.particles.MCH_ParticlesUtil;
import com.norwood.mcheli.wrapper.W_Entity;
import com.norwood.mcheli.wrapper.W_EntityPlayer;
import com.norwood.mcheli.wrapper.W_MovingObjectPosition;
import com.norwood.mcheli.wrapper.W_WorldFunc;

import lombok.Getter;
import lombok.Setter;

public abstract class MCH_EntityBaseBullet extends W_Entity {

    private static final DataParameter<Integer> TARGET_ID = EntityDataManager.createKey(MCH_EntityBaseBullet.class,
            DataSerializers.VARINT);
    private static final DataParameter<String> INFO_NAME = EntityDataManager.createKey(MCH_EntityBaseBullet.class,
            DataSerializers.STRING);
    private static final DataParameter<String> BULLET_MODEL = EntityDataManager.createKey(MCH_EntityBaseBullet.class,
            DataSerializers.STRING);
    private static final DataParameter<Byte> BOMBLET_FLAG = EntityDataManager.createKey(MCH_EntityBaseBullet.class,
            DataSerializers.BYTE);
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
    private int countOnUpdate = 0;
    @Getter
    @Setter
    private int power;
    private MCH_WeaponInfo weaponInfo;
    private MCH_BulletModel model;

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

    public MCH_EntityBaseBullet(World par1World, double px, double py, double pz, double mx, double my, double mz,
                                float yaw, float pitch, double acceleration) {
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
    public void setPositionAndRotationDirect(double x, double y, double z, float yaw, float pitch,
                                             int posRotationIncrements, boolean teleport) {
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
                MCH_Lib.DbgLog(this.world, "MCH_EntityBaseBullet.setTargetEntity alert" + this.targetEntity + " / " +
                        this.targetEntity.getRidingEntity());
                if (this.targetEntity.getRidingEntity() != null &&
                        !(this.targetEntity.getRidingEntity() instanceof MCH_EntityAircraft) &&
                        !(this.targetEntity.getRidingEntity() instanceof MCH_EntitySeat)) {
                    W_WorldFunc.MOD_playSoundAtEntity(this.targetEntity, "alert", 2.0F, 1.0F);
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

    public void sprinkleBomblet() {}

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
                    MCH_ParticleParam prm = new MCH_ParticleParam(
                            this.world,
                            "smoke",
                            (this.prevPosX + x * i + (this.prevPosX2 + x2 * i)) / 2.0,
                            (this.prevPosY + y * i + (this.prevPosY2 + y2 * i)) / 2.0,
                            (this.prevPosZ + z * i + (this.prevPosZ2 + z2 * i)) / 2.0);
                    prm.size = size + this.rand.nextFloat();
                    MCH_ParticlesUtil.spawnParticle(prm);
                }
            } else {
                for (int i = 0; i < num; i++) {
                    MCH_ParticlesUtil.DEF_spawnParticle(
                            name,
                            (this.prevPosX + x * i + (this.prevPosX2 + x2 * i)) / 2.0,
                            (this.prevPosY + y * i + (this.prevPosY2 + y2 * i)) / 2.0,
                            (this.prevPosZ + z * i + (this.prevPosZ2 + z2 * i)) / 2.0,
                            0.0,
                            0.0,
                            0.0,
                            50.0F);
                }
            }
        }
    }

    public int getCountOnUpdate() {
        return this.countOnUpdate;
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
    }

    public void setParameterFromWeapon(MCH_EntityBaseBullet b, Entity entity, Entity user) {
        this.explosionPower = b.explosionPower;
        this.explosionPowerInWater = b.explosionPowerInWater;
        this.setPower(b.getPower());
        this.piercing = b.piercing;
        this.shootingAircraft = entity;
        this.shootingEntity = user;
    }

    public void setMotion(double targetX, double targetY, double targetZ) {
        double d6 = MathHelper.sqrt(targetX * targetX + targetY * targetY + targetZ * targetZ);
        this.motionX = targetX * this.acceleration / d6;
        this.motionY = targetY * this.acceleration / d6;
        this.motionZ = targetZ * this.acceleration / d6;
    }

    public boolean usingFlareOfTarget(Entity entity) {
        if (this.getCountOnUpdate() % 3 == 0) {
            List<Entity> list = this.world.getEntitiesWithinAABBExcludingEntity(this,
                    entity.getEntityBoundingBox().grow(15.0, 15.0, 15.0));

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
        double tx = targetPosX - this.posX;
        double ty = targetPosY - this.posY;
        double tz = targetPosZ - this.posZ;
        double d = MathHelper.sqrt(tx * tx + ty * ty + tz * tz);
        double mx = tx * this.acceleration / d;
        double my = ty * this.acceleration / d;
        double mz = tz * this.acceleration / d;
        this.motionX = (this.motionX * 6.0 + mx) / 7.0;
        this.motionY = (this.motionY * 6.0 + my) / 7.0;
        this.motionZ = (this.motionZ * 6.0 + mz) / 7.0;
        double a = (float) Math.atan2(this.motionZ, this.motionX);
        this.rotationYaw = (float) (a * 180.0 / Math.PI) - 90.0F;
        double r = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
        this.rotationPitch = -((float) (Math.atan2(this.motionY, r) * 180.0 / Math.PI));
    }

    public boolean checkValid() {
        if (this.shootingEntity == null && this.shootingAircraft == null) {
            return false;
        } else {
            Entity shooter = this.shootingAircraft != null && this.shootingAircraft.isDead &&
                    this.shootingEntity == null ? this.shootingAircraft : this.shootingEntity;
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
        if (this.world.isRemote && this.countOnUpdate == 0) {
            int tgtEttId = this.getTargetEntityID();
            if (tgtEttId > 0) {
                this.setTargetEntity(this.world.getEntityByID(tgtEttId));
            }
        }

        if (!this.world.isRemote && this.getCountOnUpdate() % 20 == 19 && this.targetEntity instanceof EntityPlayerMP) {
            MCH_Lib.DbgLog(this.world, "MCH_EntityBaseBullet.onUpdate alert" + this.targetEntity + " / " +
                    this.targetEntity.getRidingEntity());
            if (this.targetEntity.getRidingEntity() != null &&
                    !(this.targetEntity.getRidingEntity() instanceof MCH_EntityAircraft) &&
                    !(this.targetEntity.getRidingEntity() instanceof MCH_EntitySeat)) {
                W_WorldFunc.MOD_playSoundAtEntity(this.targetEntity, "alert", 2.0F, 1.0F);
            }
        }

        this.prevMotionX = this.motionX;
        this.prevMotionY = this.motionY;
        this.prevMotionZ = this.motionZ;
        this.countOnUpdate++;
        if (this.countOnUpdate > 10000000) {
            this.clearCountOnUpdate();
        }

        this.prevPosX2 = this.prevPosX;
        this.prevPosY2 = this.prevPosY;
        this.prevPosZ2 = this.prevPosZ;
        super.onUpdate();
        if ((this.prevMotionX != this.motionX || this.prevMotionY != this.motionY ||
                this.prevMotionZ != this.motionZ) &&
                this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ > 0.1) {
            double a = (float) Math.atan2(this.motionZ, this.motionX);
            this.rotationYaw = (float) (a * 180.0 / Math.PI) - 90.0F;
            double r = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
            this.rotationPitch = -((float) (Math.atan2(this.motionY, r) * 180.0 / Math.PI));
        }

        if (this.getInfo() == null) {
            if (this.countOnUpdate >= 2) {
                MCH_Lib.Log(
                        this, "##### MCH_EntityBaseBullet onUpdate() Weapon info null %d, %s, Name=%s",
                        W_Entity.getEntityId(this), this.getEntityName(), this.getName());
                this.setDead();
                return;
            }

            this.setName(this.getName());
            if (this.getInfo() == null) {
                return;
            }
        }

        if (this.getInfo().bound <= 0.0F && this.onGround) {
            this.motionX *= 0.9;
            this.motionZ *= 0.9;
        }

        if (this.world.isRemote && this.isBomblet < 0) {
            this.isBomblet = this.getBomblet();
        }

        if (!this.world.isRemote) {
            BlockPos blockpos = new BlockPos(this.posX, this.posY, this.posZ);
            if ((int) this.posY <= 255 && !this.world.isBlockLoaded(blockpos)) {
                if (this.getInfo().delayFuse <= 0) {
                    this.setDead();
                    return;
                }

                if (this.delayFuse == 0) {
                    this.delayFuse = this.getInfo().delayFuse;
                }
            }

            if (this.delayFuse > 0) {
                this.delayFuse--;
                if (this.delayFuse == 0) {
                    this.onUpdateTimeout();
                    this.setDead();
                    return;
                }
            }

            if (!this.checkValid()) {
                this.setDead();
                return;
            }

            if (this.getInfo().timeFuse > 0 && this.getCountOnUpdate() > this.getInfo().timeFuse) {
                this.onUpdateTimeout();
                this.setDead();
                return;
            }

            if (this.getInfo().explosionAltitude > 0 &&
                    MCH_Lib.getBlockIdY(this, 3, -this.getInfo().explosionAltitude) != 0) {
                RayTraceResult mop = new RayTraceResult(new Vec3d(this.posX, this.posY, this.posZ), EnumFacing.DOWN,
                        new BlockPos(this.posX, this.posY, this.posZ));
                this.onImpact(mop, 1.0F);
            }
        }

        if (!this.isInWater()) {
            this.motionY = this.motionY + this.getGravity();
        } else {
            this.motionY = this.motionY + this.getGravityInWater();
        }

        if (!this.isDead) {
            this.onUpdateCollided();
        }

        this.posX = this.posX + this.motionX * this.accelerationFactor;
        this.posY = this.posY + this.motionY * this.accelerationFactor;
        this.posZ = this.posZ + this.motionZ * this.accelerationFactor;
        if (this.world.isRemote) {
            this.updateSplash();
        }

        if (this.isInWater()) {
            float f3 = 0.25F;
            this.world
                    .spawnParticle(
                            EnumParticleTypes.WATER_BUBBLE,
                            this.posX - this.motionX * f3,
                            this.posY - this.motionY * f3,
                            this.posZ - this.motionZ * f3,
                            this.motionX,
                            this.motionY,
                            this.motionZ);
        }

        this.setPosition(this.posX, this.posY, this.posZ);
    }

    public void updateSplash() {
        if (this.getInfo() != null) {
            if (this.getInfo().power > 0) {
                if (!W_WorldFunc.isBlockWater(this.world, (int) (this.prevPosX + 0.5), (int) (this.prevPosY + 0.5),
                        (int) (this.prevPosZ + 0.5)) &&
                        W_WorldFunc.isBlockWater(this.world, (int) (this.posX + 0.5), (int) (this.posY + 0.5),
                                (int) (this.posZ + 0.5))) {
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
                        if (W_WorldFunc.isBlockWater(this.world, (int) (px + 0.5), (int) (py + 0.5),
                                (int) (pz + 0.5))) {
                            float pwr = this.getInfo().power < 20 ? this.getInfo().power : 20.0F;
                            int n = this.rand.nextInt(1 + (int) pwr / 3) + (int) pwr / 2 + 1;
                            pwr *= 0.03F;

                            for (int j = 0; j < n; j++) {
                                MCH_ParticleParam prm = new MCH_ParticleParam(
                                        this.world,
                                        "splash",
                                        px,
                                        py + 0.5,
                                        pz,
                                        pwr * (this.rand.nextDouble() - 0.5) * 0.3,
                                        pwr * (this.rand.nextDouble() * 0.5 + 0.5) * 1.8,
                                        pwr * (this.rand.nextDouble() - 0.5) * 0.3,
                                        pwr * 5.0F);
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
                this.newExplosion(this.posX, this.posY, this.posZ, this.explosionPowerInWater,
                        this.explosionPowerInWater, true);
            }
        } else if (this.explosionPower > 0) {
            this.newExplosion(this.posX, this.posY, this.posZ, this.explosionPower, this.getInfo().explosionBlock,
                    false);
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
            case DOWN:
                if (this.motionY > 0.0) {
                    this.motionY = -this.motionY * this.getInfo().bound;
                }
                break;
            case UP:
                if (this.motionY < 0.0) {
                    this.motionY = -this.motionY * this.getInfo().bound;
                }
                break;
            case NORTH:
                if (this.motionZ > 0.0) {
                    this.motionZ = -this.motionZ * this.getInfo().bound;
                } else {
                    this.posZ = this.posZ + this.motionZ;
                }
                break;
            case SOUTH:
                if (this.motionZ < 0.0) {
                    this.motionZ = -this.motionZ * this.getInfo().bound;
                } else {
                    this.posZ = this.posZ + this.motionZ;
                }
                break;
            case WEST:
                if (this.motionX > 0.0) {
                    this.motionX = -this.motionX * this.getInfo().bound;
                } else {
                    this.posX = this.posX + this.motionX;
                }
                break;
            case EAST:
                if (this.motionX < 0.0) {
                    this.motionX = -this.motionX * this.getInfo().bound;
                } else {
                    this.posX = this.posX + this.motionX;
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
            if (this.shootingEntity != null && W_MovingObjectPosition.isHitTypeTile(m)) {
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
            List<Entity> list = this.world.getEntitiesWithinAABBExcludingEntity(this,
                    this.getEntityBoundingBox().expand(mx, my, mz).grow(21.0, 21.0, 21.0));
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
        if (entity instanceof MCH_EntityChain) return false;
        if (!entity.canBeCollidedWith()) return false;
        if (entity instanceof MCH_EntitySeat) return false;
        if (entity instanceof MCH_EntityHitBox) return false;

        // Bullet vs. bullet
        if (entity instanceof MCH_EntityBaseBullet blt) {
            if (world.isRemote) return false;
            if (W_Entity.isEqual(blt.shootingAircraft, this.shootingAircraft)) return false;
            if (W_Entity.isEqual(blt.shootingEntity, this.shootingEntity)) return false;
        }

        // Don’t hit self or own aircraft
        if (W_Entity.isEqual(entity, this.shootingEntity)) return false;
        if (this.shootingAircraft instanceof MCH_EntityAircraft aircraft) {
            if (W_Entity.isEqual(entity, aircraft)) return false;
            if (aircraft.isMountedEntity(entity)) return false;
        }

        // Config-based exclusions
        String cls = entity.getClass().getName().toLowerCase();
        for (String s : MCH_Config.IgnoreBulletHitList) {
            if (cls.contains(s.toLowerCase())) return false;
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

    protected void onImpact(RayTraceResult m, float damageFactor) {
        if (!world.isRemote) {

            try {
                MCH_WeaponInfo info = getInfo();

                // --- Entity Impact ---
                if (m.entityHit != null) {
                    onImpactEntity(m.entityHit, damageFactor);
                    piercing = 0;
                }

                // --- Explosion Setup ---
                float expPower = explosionPower * damageFactor;
                float expPowerInWater = explosionPowerInWater * damageFactor;
                Vec3d hit = m.hitVec;

                // --- Piercing Logic ---
                if (piercing > 0) {
                    piercing--;
                    if (expPower > 0.0F) {
                        newExplosion(hit.x, hit.y, hit.z, 1.0F, 1.0F, false);
                    }
                    return;
                }

                // --- Explosion Behavior ---
                if (expPowerInWater == 0.0F) {
                    if (info.isFAE) {
                        newFAExplosion(posX, posY, posZ, expPower, info.explosionBlock);
                    } else if (expPower > 0.0F) {
                        newExplosion(hit.x, hit.y, hit.z, expPower, info.explosionBlock, false);
                    } else if (expPower < 0.0F) {
                        playExplosionSound();
                    }
                } else if (m.entityHit != null) {
                    if (isInWater()) {
                        newExplosion(hit.x, hit.y, hit.z, expPowerInWater, expPowerInWater, true);
                    } else {
                        newExplosion(hit.x, hit.y, hit.z, expPower, info.explosionBlock, false);
                    }
                } else {
                    boolean inWater = isInWater() || MCH_Lib.isBlockInWater(
                            world, m.getBlockPos().getX(), m.getBlockPos().getY(), m.getBlockPos().getZ());

                    if (inWater) {
                        newExplosion(hit.x, hit.y, hit.z, expPowerInWater, expPowerInWater, true);
                    } else if (expPower > 0.0F) {
                        newExplosion(hit.x, hit.y, hit.z, expPower, info.explosionBlock, false);
                    } else if (expPower < 0.0F) {
                        playExplosionSound();
                    }
                }

                setDead();

            } catch (Exception e) {
                MCH_Lib.Log(this, "Error in onImpact: %s", e.getMessage());
                e.printStackTrace();
            }

        } else if (getInfo() != null &&
                (getInfo().explosion == 0 || getInfo().modeNum >= 2) &&
                W_MovingObjectPosition.isHitTypeTile(m)) {

                    // --- Client-side Particle Effects ---
                    float p = getInfo().power;
                    BlockPos pos = m.getBlockPos();

                    for (int i = 0; i < p / 3.0F; i++) {
                        MCH_ParticlesUtil.spawnParticleTileCrack(
                                world, pos.getX(), pos.getY(), pos.getZ(),
                                m.hitVec.x + (rand.nextFloat() - 0.5) * p / 10.0,
                                m.hitVec.y + 0.1,
                                m.hitVec.z + (rand.nextFloat() - 0.5) * p / 10.0,
                                -motionX * p / 2.0, p / 2.0F, -motionZ * p / 2.0);
                    }
                }
    }

    public void onImpactEntity(Entity entity, float damageFactor) {
        if (!entity.isDead) {
            MCH_Lib.DbgLog(this.world, "MCH_EntityBaseBullet.onImpactEntity:Damage=%d:" + entity.getClass(),
                    this.getPower());
            MCH_Lib.applyEntityHurtResistantTimeConfig(entity);
            DamageSource ds = DamageSource.causeThrownDamage(this, this.shootingEntity);
            float damage = MCH_Config.applyDamageVsEntity(entity, ds, this.getPower() * damageFactor);
            damage *= this.getInfo() != null ? this.getInfo().getDamageFactor(entity) : 1.0F;
            entity.attackEntityFrom(ds, damage);
            if (this instanceof MCH_EntityBullet && entity instanceof EntityVillager && this.shootingEntity != null &&
                    this.shootingEntity instanceof EntityPlayerMP &&
                    this.shootingEntity.getRidingEntity() instanceof MCH_EntitySeat) {
                MCH_CriteriaTriggers.VILLAGER_HURT_BULLET.trigger((EntityPlayerMP) this.shootingEntity);
            }

            if (!entity.isDead) {}
        }

        this.notifyHitBullet();
    }

    public void newFAExplosion(double x, double y, double z, float exp, float expBlock) {
        MCH_Explosion.ExplosionResult result = MCH_Explosion.newExplosion(
                this.world, this, this.shootingEntity, x, y, z, exp, expBlock, true, true, this.getInfo().flaming,
                false, 15);
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
                result = MCH_Explosion.newExplosion(
                        this.world,
                        this,
                        this.shootingEntity,
                        x,
                        y,
                        z,
                        exp,
                        expBlock,
                        this.rand.nextInt(3) == 0,
                        true,
                        this.getInfo().flaming,
                        true,
                        0,
                        this.getInfo() != null ? this.getInfo().damageFactor : null);
            } else {
                result = MCH_Explosion.newExplosionInWater(
                        this.world,
                        this,
                        this.shootingEntity,
                        x,
                        y,
                        z,
                        exp,
                        expBlock,
                        this.rand.nextInt(3) == 0,
                        true,
                        this.getInfo().flaming,
                        true,
                        0,
                        this.getInfo() != null ? this.getInfo().damageFactor : null);
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
            case NONE -> {}
            case NTM_VNT -> {
                if (getInfo().vntSettingContainer == null) break;
                var vnt = getInfo().vntSettingContainer;
                vnt.buildExplosion(this.world, x, y, z, expBlock, this, effectOnly);

            }
            case NTM_NT -> {
                if (getInfo().ntSettingContainer == null) break;
                var nt = getInfo().ntSettingContainer;
                if (!effectOnly)
                    nt.explode(this.world, this, x, y, z, (int) expBlock);

            }
            case NTM_MINI_NUKE -> {
                if (getInfo().mukeContainer == null) break;
                var muke = getInfo().mukeContainer;
                muke.explode(world, x, y, z, effectOnly);
            }
            case NTM_NUKE -> {
                HBMUtil.EntityNukeExplosionMK5(this.world, (int) expBlock, x, y, z, effectOnly);
            }
            case NTM_CHEMICAL -> {
                HBMUtil.ExplosionChaos_spawnChlorine(world, x, y, z, getInfo().chemicalContainer);
            }
            case NTM_MIST -> {
                if (getInfo().mistContainer == null) break;
                var mist = getInfo().mistContainer;
                if (!effectOnly)
                    mist.execute(world, x, y, z);
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
}
