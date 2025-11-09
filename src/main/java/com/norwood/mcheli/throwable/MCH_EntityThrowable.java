package com.norwood.mcheli.throwable;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IThrowableEntity;

import org.jetbrains.annotations.NotNull;

import com.norwood.mcheli.MCH_Explosion;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.particles.MCH_ParticleParam;
import com.norwood.mcheli.particles.MCH_ParticlesUtil;
import com.norwood.mcheli.wrapper.W_WorldFunc;

public class MCH_EntityThrowable extends EntityThrowable implements IThrowableEntity {

    private static final DataParameter<String> INFO_NAME = EntityDataManager.createKey(MCH_EntityThrowable.class,
            DataSerializers.STRING);
    public double boundPosX;
    public double boundPosY;
    public double boundPosZ;
    public RayTraceResult lastOnImpact;
    public int noInfoCount;
    private int countOnUpdate;
    private MCH_ThrowableInfo throwableInfo;

    public MCH_EntityThrowable(World par1World) {
        super(par1World);
        this.init();
    }

    public MCH_EntityThrowable(World par1World, EntityLivingBase par2EntityLivingBase, float acceleration) {
        super(par1World, par2EntityLivingBase);
        this.motionX *= acceleration;
        this.motionY *= acceleration;
        this.motionZ *= acceleration;
        this.init();
    }

    public MCH_EntityThrowable(World par1World, double par2, double par4, double par6) {
        super(par1World, par2, par4, par6);
        this.init();
    }

    public MCH_EntityThrowable(World worldIn, double x, double y, double z, float yaw, float pitch) {
        this(worldIn);
        this.setSize(0.25F, 0.25F);
        this.setLocationAndAngles(x, y, z, yaw, pitch);
        this.posX = this.posX - MathHelper.cos(this.rotationYaw / 180.0F * (float) Math.PI) * 0.16F;
        this.posY -= 0.1F;
        this.posZ = this.posZ - MathHelper.sin(this.rotationYaw / 180.0F * (float) Math.PI) * 0.16F;
        this.setPosition(this.posX, this.posY, this.posZ);
        this.shoot(null, pitch, yaw, 0.0F, 1.5F, 1.0F);
    }

    public void init() {
        this.lastOnImpact = null;
        this.countOnUpdate = 0;
        this.setInfo(null);
        this.noInfoCount = 0;
        this.dataManager.register(INFO_NAME, "");
    }

    public void shoot(@NotNull Entity entityThrower, float rotationPitchIn, float rotationYawIn, float pitchOffset,
                      float velocity, float inaccuracy) {
        float f = 0.4F;
        this.motionX = -MathHelper.sin(rotationYawIn / 180.0F * (float) Math.PI) *
                MathHelper.cos(rotationPitchIn / 180.0F * (float) Math.PI) * f;
        this.motionZ = MathHelper.cos(rotationYawIn / 180.0F * (float) Math.PI) *
                MathHelper.cos(rotationPitchIn / 180.0F * (float) Math.PI) * f;
        this.motionY = -MathHelper.sin((rotationPitchIn + pitchOffset) / 180.0F * (float) Math.PI) * f;
        this.shoot(this.motionX, this.motionY, this.motionZ, velocity, 1.0F);
    }

    public void setDead() {
        String s = this.getInfo() != null ? this.getInfo().name : "null";
        MCH_Lib.DbgLog(this.world, "MCH_EntityThrowable.setDead(%s)", s);
        super.setDead();
    }

    public void onUpdate() {
        this.boundPosX = this.posX;
        this.boundPosY = this.posY;
        this.boundPosZ = this.posZ;
        if (this.getInfo() != null) {
            Block block = W_WorldFunc.getBlock(this.world, (int) (this.posX + 0.5), (int) this.posY,
                    (int) (this.posZ + 0.5));
            Material mat = W_WorldFunc.getBlockMaterial(
                    this.world, (int) (this.posX + 0.5), (int) this.posY, (int) (this.posZ + 0.5));
            if (mat == Material.WATER) {
                this.motionY = this.motionY + this.getInfo().gravityInWater;
            } else {
                this.motionY = this.motionY + this.getInfo().gravity;
            }
        }

        super.onUpdate();
        if (this.lastOnImpact != null) {
            this.boundBullet(this.lastOnImpact);
            this.setPosition(this.boundPosX + this.motionX, this.boundPosY + this.motionY,
                    this.boundPosZ + this.motionZ);
            this.lastOnImpact = null;
        }

        this.countOnUpdate++;
        if (this.countOnUpdate >= 2147483632) {
            this.setDead();
        } else {
            if (this.getInfo() == null) {
                String s = this.dataManager.get(INFO_NAME);
                if (!s.isEmpty()) {
                    this.setInfo(MCH_ThrowableInfoManager.get(s));
                }

                if (this.getInfo() == null) {
                    this.noInfoCount++;
                    if (this.noInfoCount > 10) {
                        this.setDead();
                    }

                    return;
                }
            }

            if (!this.isDead) {
                if (!this.world.isRemote) {
                    if (this.countOnUpdate == this.getInfo().timeFuse && this.getInfo().explosion > 0) {
                        MCH_Explosion.newExplosion(
                                this.world,
                                null,
                                null,
                                this.posX,
                                this.posY,
                                this.posZ,
                                this.getInfo().explosion,
                                this.getInfo().explosion,
                                true,
                                true,
                                false,
                                true,
                                0);
                        this.setDead();
                        return;
                    }

                    if (this.countOnUpdate >= this.getInfo().aliveTime) {
                        this.setDead();
                    }
                } else if (this.countOnUpdate >= this.getInfo().timeFuse && this.getInfo().explosion <= 0) {
                    for (int i = 0; i < this.getInfo().smokeNum; i++) {
                        float r = this.getInfo().smokeColor.r * 0.9F + this.rand.nextFloat() * 0.1F;
                        float g = this.getInfo().smokeColor.g * 0.9F + this.rand.nextFloat() * 0.1F;
                        float b = this.getInfo().smokeColor.b * 0.9F + this.rand.nextFloat() * 0.1F;
                        if (this.getInfo().smokeColor.r == this.getInfo().smokeColor.g) {
                            g = r;
                        }

                        if (this.getInfo().smokeColor.r == this.getInfo().smokeColor.b) {
                            b = r;
                        }

                        if (this.getInfo().smokeColor.g == this.getInfo().smokeColor.b) {
                            b = g;
                        }

                        this.spawnParticle(
                                "explode",
                                4,
                                this.getInfo().smokeSize + this.rand.nextFloat() * this.getInfo().smokeSize / 3.0F,
                                r,
                                g,
                                b,
                                this.getInfo().smokeVelocityHorizontal * (this.rand.nextFloat() - 0.5F),
                                this.getInfo().smokeVelocityVertical * this.rand.nextFloat(),
                                this.getInfo().smokeVelocityHorizontal * (this.rand.nextFloat() - 0.5F));
                    }
                }
            }
        }
    }

    public void spawnParticle(String name, int num, float size, float r, float g, float b, float mx, float my,
                              float mz) {
        if (this.world.isRemote) {
            if (name.isEmpty() || num < 1) {
                return;
            }

            double x = (this.posX - this.prevPosX) / num;
            double y = (this.posY - this.prevPosY) / num;
            double z = (this.posZ - this.prevPosZ) / num;

            for (int i = 0; i < num; i++) {
                MCH_ParticleParam prm = new MCH_ParticleParam(
                        this.world, "smoke", this.prevPosX + x * i, 1.0 + this.prevPosY + y * i, this.prevPosZ + z * i);
                prm.setMotion(mx, my, mz);
                prm.size = size;
                prm.setColor(1.0F, r, g, b);
                prm.isEffectWind = true;
                prm.toWhite = true;
                MCH_ParticlesUtil.spawnParticle(prm);
            }
        }
    }

    protected float getGravityVelocity() {
        return 0.0F;
    }

    public void boundBullet(RayTraceResult m) {
        if (m.sideHit != null) {
            float bound = this.getInfo().bound;
            switch (m.sideHit) {
                case DOWN:
                case UP:
                    this.motionX *= 0.9F;
                    this.motionZ *= 0.9F;
                    this.boundPosY = m.hitVec.y;
                    if ((m.sideHit != EnumFacing.DOWN || !(this.motionY > 0.0)) &&
                            (m.sideHit != EnumFacing.UP || !(this.motionY < 0.0))) {
                        this.motionY = 0.0;
                    } else {
                        this.motionY = -this.motionY * bound;
                    }
                    break;
                case NORTH:
                    if (this.motionZ > 0.0) {
                        this.motionZ = -this.motionZ * bound;
                    }
                    break;
                case SOUTH:
                    if (this.motionZ < 0.0) {
                        this.motionZ = -this.motionZ * bound;
                    }
                    break;
                case WEST:
                    if (this.motionX > 0.0) {
                        this.motionX = -this.motionX * bound;
                    }
                    break;
                case EAST:
                    if (this.motionX < 0.0) {
                        this.motionX = -this.motionX * bound;
                    }
            }
        }
    }

    protected void onImpact(@NotNull RayTraceResult m) {
        if (this.getInfo() != null) {
            this.lastOnImpact = m;
        }
    }

    @Nullable
    public MCH_ThrowableInfo getInfo() {
        return this.throwableInfo;
    }

    public void setInfo(MCH_ThrowableInfo info) {
        this.throwableInfo = info;
        if (info != null && !this.world.isRemote) {
            this.dataManager.set(INFO_NAME, info.name);
        }
    }

    public void setThrower(Entity entity) {
        if (entity instanceof EntityLivingBase) {
            this.thrower = (EntityLivingBase) entity;
        }
    }
}
