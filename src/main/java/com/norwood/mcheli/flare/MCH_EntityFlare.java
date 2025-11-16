package com.norwood.mcheli.flare;

import com.norwood.mcheli.particles.MCH_ParticleParam;
import com.norwood.mcheli.particles.MCH_ParticlesUtil;
import com.norwood.mcheli.wrapper.W_Entity;
import com.norwood.mcheli.wrapper.W_WorldFunc;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

public class MCH_EntityFlare extends W_Entity implements IEntityAdditionalSpawnData {

    public double gravity = -0.013;
    public double airResistance = 0.992;
    public float size;
    public int fuseCount;

    public MCH_EntityFlare(World par1World) {
        super(par1World);
        this.setSize(1.0F, 1.0F);
        this.prevRotationYaw = this.rotationYaw;
        this.prevRotationPitch = this.rotationPitch;
        this.size = 6.0F;
        this.fuseCount = 0;
    }

    public MCH_EntityFlare(World par1World, double pX, double pY, double pZ, double mX, double mY, double mZ,
                           float size, int fuseCount) {
        this(par1World);
        this.setLocationAndAngles(pX, pY, pZ, 0.0F, 0.0F);
        this.motionX = mX;
        this.motionY = mY;
        this.motionZ = mZ;
        this.size = size;
        this.fuseCount = fuseCount;
    }

    public boolean isEntityInvulnerable(@NotNull DamageSource source) {
        return true;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean isInRangeToRenderDist(double par1) {
        double d1 = this.getEntityBoundingBox().getAverageEdgeLength() * 4.0;
        d1 *= 64.0;
        return par1 < d1 * d1;
    }

    public void setDead() {
        super.setDead();
        if (this.fuseCount > 0 && this.world.isRemote) {
            this.fuseCount = 0;

            for (int i = 0; i < 20; i++) {
                double x = (this.rand.nextDouble() - 0.5) * 10.0;
                double y = (this.rand.nextDouble() - 0.5) * 10.0;
                double z = (this.rand.nextDouble() - 0.5) * 10.0;
                MCH_ParticleParam prm = new MCH_ParticleParam(this.world, "smoke", this.posX + x, this.posY + y,
                        this.posZ + z);
                prm.age = 200 + this.rand.nextInt(100);
                prm.size = 20 + this.rand.nextInt(25);
                prm.motionX = (this.rand.nextDouble() - 0.5) * 0.45;
                prm.motionY = (this.rand.nextDouble() - 0.5) * 0.01;
                prm.motionZ = (this.rand.nextDouble() - 0.5) * 0.45;
                prm.a = this.rand.nextFloat() * 0.1F + 0.85F;
                prm.b = this.rand.nextFloat() * 0.2F + 0.5F;
                prm.g = prm.b + 0.05F;
                prm.r = prm.b + 0.1F;
                MCH_ParticlesUtil.spawnParticle(prm);
            }
        }
    }

    public void writeSpawnData(ByteBuf buffer) {
        try {
            buffer.writeByte(this.fuseCount);
        } catch (Exception var3) {
            var3.printStackTrace();
        }
    }

    public void readSpawnData(ByteBuf additionalData) {
        try {
            this.fuseCount = additionalData.readByte();
        } catch (Exception var3) {
            var3.printStackTrace();
        }
    }

    public void onUpdate() {
        if (this.fuseCount > 0 && this.ticksExisted >= this.fuseCount) {
            this.setDead();
        } else if (!this.world.isRemote && !this.world.isBlockLoaded(new BlockPos(this.posX, this.posY, this.posZ))) {
            this.setDead();
        } else if (this.ticksExisted > 300 && !this.world.isRemote) {
            this.setDead();
        } else {
            super.onUpdate();
            if (!this.world.isRemote) {
                this.onUpdateCollided();
            }

            this.posX = this.posX + this.motionX;
            this.posY = this.posY + this.motionY;
            this.posZ = this.posZ + this.motionZ;
            if (this.world.isRemote) {
                double x = (this.posX - this.prevPosX) / 2.0;
                double y = (this.posY - this.prevPosY) / 2.0;
                double z = (this.posZ - this.prevPosZ) / 2.0;

                for (int i = 0; i < 2; i++) {
                    MCH_ParticleParam prm = new MCH_ParticleParam(this.world, "smoke", this.prevPosX + x * i,
                            this.prevPosY + y * i, this.prevPosZ + z * i);
                    prm.size = 6.0F + this.rand.nextFloat();
                    if (this.size < 5.0F) {
                        prm.a = (float) (prm.a * 0.75);
                        if (this.rand.nextInt(2) == 0) {}
                    }

                    if (this.fuseCount > 0) {
                        prm.a = this.rand.nextFloat() * 0.1F + 0.85F;
                        prm.b = this.rand.nextFloat() * 0.1F + 0.5F;
                        prm.g = prm.b + 0.05F;
                        prm.r = prm.b + 0.1F;
                    }

                    MCH_ParticlesUtil.spawnParticle(prm);
                }
            }

            this.motionY = this.motionY + this.gravity;
            this.motionX = this.motionX * this.airResistance;
            this.motionZ = this.motionZ * this.airResistance;
            if (this.isInWater() && !this.world.isRemote) {
                this.setDead();
            }

            if (this.onGround && !this.world.isRemote) {
                this.setDead();
            }

            this.setPosition(this.posX, this.posY, this.posZ);
        }
    }

    protected void onUpdateCollided() {
        Vec3d vec3 = new Vec3d(this.posX, this.posY, this.posZ);
        Vec3d vec31 = new Vec3d(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
        RayTraceResult mop = W_WorldFunc.clip(this.world, vec3, vec31);
        if (mop != null) {
            this.onImpact(mop);
        }
    }

    protected void onImpact(RayTraceResult par1MovingObjectPosition) {
        if (!this.world.isRemote) {
            this.setDead();
        }
    }

    public void writeEntityToNBT(NBTTagCompound par1NBTTagCompound) {
        par1NBTTagCompound.setTag("direction", this.newDoubleNBTList(this.motionX, this.motionY, this.motionZ));
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
    public boolean attackEntityFrom(DamageSource par1DamageSource, float par2) {
        return false;
    }

    @SideOnly(Side.CLIENT)
    public float getShadowSize() {
        return 0.0F;
    }
}
