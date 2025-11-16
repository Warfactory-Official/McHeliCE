package com.norwood.mcheli.parachute;

import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.helper.entity.IEntitySinglePassenger;
import com.norwood.mcheli.particles.MCH_ParticleParam;
import com.norwood.mcheli.particles.MCH_ParticlesUtil;
import com.norwood.mcheli.wrapper.*;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class MCH_EntityParachute extends W_Entity implements IEntitySinglePassenger {

    private static final DataParameter<Byte> TYPE = EntityDataManager.createKey(MCH_EntityParachute.class,
            DataSerializers.BYTE);
    public Entity user;
    public int onGroundCount;
    private double speedMultiplier = 0.07;
    private int paraPosRotInc;
    private double paraX;
    private double paraY;
    private double paraZ;
    private double paraYaw;
    private double paraPitch;
    @SideOnly(Side.CLIENT)
    private double velocityX;
    @SideOnly(Side.CLIENT)
    private double velocityY;
    @SideOnly(Side.CLIENT)
    private double velocityZ;

    public MCH_EntityParachute(World par1World) {
        super(par1World);
        this.preventEntitySpawning = true;
        this.setSize(1.5F, 0.6F);
        this.user = null;
        this.onGroundCount = 0;
    }

    public MCH_EntityParachute(World par1World, double par2, double par4, double par6) {
        this(par1World);
        this.setPosition(par2, par4, par6);
        this.motionX = 0.0;
        this.motionY = 0.0;
        this.motionZ = 0.0;
        this.prevPosX = par2;
        this.prevPosY = par4;
        this.prevPosZ = par6;
    }

    protected boolean canTriggerWalking() {
        return false;
    }

    @Override
    protected void entityInit() {
        this.dataManager.register(TYPE, (byte) 0);
    }

    public int getType() {
        return this.dataManager.get(TYPE);
    }

    public void setType(int n) {
        this.dataManager.set(TYPE, (byte) n);
    }

    public AxisAlignedBB getCollisionBox(Entity par1Entity) {
        return par1Entity.getEntityBoundingBox();
    }

    public AxisAlignedBB getCollisionBoundingBox() {
        return this.getEntityBoundingBox();
    }

    public boolean canBePushed() {
        return true;
    }

    public double getMountedYOffset() {
        return this.height * 0.0 - 0.3F;
    }

    @Override
    public boolean attackEntityFrom(DamageSource par1DamageSource, float par2) {
        return false;
    }

    public boolean canBeCollidedWith() {
        return !this.isDead;
    }

    @SideOnly(Side.CLIENT)
    public void setPositionAndRotationDirect(double x, double y, double z, float yaw, float pitch,
                                             int posRotationIncrements, boolean teleport) {
        this.paraPosRotInc = posRotationIncrements + 10;
        this.paraX = x;
        this.paraY = y;
        this.paraZ = z;
        this.paraYaw = yaw;
        this.paraPitch = pitch;
        this.motionX = this.velocityX;
        this.motionY = this.velocityY;
        this.motionZ = this.velocityZ;
    }

    @SideOnly(Side.CLIENT)
    public void setVelocity(double par1, double par3, double par5) {
        this.velocityX = this.motionX = par1;
        this.velocityY = this.motionY = par3;
        this.velocityZ = this.motionZ = par5;
    }

    public void setDead() {
        super.setDead();
    }

    public void onUpdate() {
        super.onUpdate();
        if (!this.world.isRemote && this.ticksExisted % 10 == 0) {
            MCH_Lib.DbgLog(this.world, "MCH_EntityParachute.onUpdate %d, %.3f", this.ticksExisted, this.motionY);
        }

        if (this.isOpenParachute() && this.motionY > -0.3 && this.ticksExisted > 20) {
            this.fallDistance = (float) (this.fallDistance * 0.85);
        }

        if (!this.world.isRemote && this.user != null && this.user.getRidingEntity() == null) {
            this.user.startRiding(this);
            this.rotationYaw = this.prevRotationYaw = this.user.rotationYaw;
            this.user = null;
        }

        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        double d1 = this.getEntityBoundingBox().minY +
                (this.getEntityBoundingBox().maxY - this.getEntityBoundingBox().minY) * 0.0 / 5.0 - 0.125;
        double d2 = this.getEntityBoundingBox().minY +
                (this.getEntityBoundingBox().maxY - this.getEntityBoundingBox().minY) / 5.0 - 0.125;
        AxisAlignedBB axisalignedbb = W_AxisAlignedBB.getAABB(
                this.getEntityBoundingBox().minX, d1, this.getEntityBoundingBox().minZ,
                this.getEntityBoundingBox().maxX, d2, this.getEntityBoundingBox().maxZ);
        if (this.world.isMaterialInBB(axisalignedbb, Material.WATER)) {
            this.onWaterSetBoat();
            this.setDead();
        }

        if (this.world.isRemote) {
            this.onUpdateClient();
        } else {
            this.onUpdateServer();
        }
    }

    public void onUpdateClient() {
        if (this.paraPosRotInc > 0) {
            double x = this.posX + (this.paraX - this.posX) / this.paraPosRotInc;
            double y = this.posY + (this.paraY - this.posY) / this.paraPosRotInc;
            double z = this.posZ + (this.paraZ - this.posZ) / this.paraPosRotInc;
            double yaw = MathHelper.wrapDegrees(this.paraYaw - this.rotationYaw);
            this.rotationYaw = (float) (this.rotationYaw + yaw / this.paraPosRotInc);
            this.rotationPitch = (float) (this.rotationPitch +
                    (this.paraPitch - this.rotationPitch) / this.paraPosRotInc);
            this.paraPosRotInc--;
            this.setPosition(x, y, z);
            this.setRotation(this.rotationYaw, this.rotationPitch);
            if (this.getRiddenByEntity() != null) {
                this.setRotation(this.getRiddenByEntity().prevRotationYaw, this.rotationPitch);
            }
        } else {
            this.setPosition(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
            if (this.onGround) {}

            this.motionX *= 0.99;
            this.motionY *= 0.95;
            this.motionZ *= 0.99;
        }

        if (!this.isOpenParachute() && this.motionY > 0.01) {
            float color = 0.6F + this.rand.nextFloat() * 0.2F;
            double dx = this.prevPosX - this.posX;
            double dy = this.prevPosY - this.posY;
            double dz = this.prevPosZ - this.posZ;
            int num = 1 + (int) (MathHelper.sqrt(dx * dx + dy * dy + dz * dz) * 2.0);

            for (double i = 0.0; i < num; i++) {
                MCH_ParticleParam prm = new MCH_ParticleParam(
                        this.world,
                        "smoke",
                        this.prevPosX + (this.posX - this.prevPosX) * (i / num) * 0.8,
                        this.prevPosY + (this.posY - this.prevPosY) * (i / num) * 0.8,
                        this.prevPosZ + (this.posZ - this.prevPosZ) * (i / num) * 0.8);
                prm.motionX = this.motionX * 0.5 + (this.rand.nextDouble() - 0.5) * 0.5;
                prm.motionX = this.motionY * -0.5 + (this.rand.nextDouble() - 0.5) * 0.5;
                prm.motionX = this.motionZ * 0.5 + (this.rand.nextDouble() - 0.5) * 0.5;
                prm.size = 5.0F;
                prm.setColor(0.8F + this.rand.nextFloat(), color, color, color);
                MCH_ParticlesUtil.spawnParticle(prm);
            }
        }
    }

    public void onUpdateServer() {
        double prevSpeed = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
        double gravity = this.onGround ? 0.01 : 0.03;
        if (this.getType() == 2 && this.ticksExisted < 20) {
            gravity = 0.01;
        }

        this.motionY -= gravity;
        if (this.isOpenParachute()) {
            if (W_Lib.isEntityLivingBase(this.getRiddenByEntity())) {
                double mv = W_Lib.getEntityMoveDist(this.getRiddenByEntity());
                if (!this.isOpenParachute()) {
                    mv = 0.0;
                }

                if (mv > 0.0) {
                    double mx = -Math.sin(this.getRiddenByEntity().rotationYaw * (float) Math.PI / 180.0F);
                    double mz = Math.cos(this.getRiddenByEntity().rotationYaw * (float) Math.PI / 180.0F);
                    this.motionX = this.motionX + mx * this.speedMultiplier * 0.05;
                    this.motionZ = this.motionZ + mz * this.speedMultiplier * 0.05;
                }
            }

            double speed = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
            if (speed > 0.35) {
                this.motionX *= 0.35 / speed;
                this.motionZ *= 0.35 / speed;
                speed = 0.35;
            }

            if (speed > prevSpeed && this.speedMultiplier < 0.35) {
                this.speedMultiplier = this.speedMultiplier + (0.35 - this.speedMultiplier) / 35.0;
                if (this.speedMultiplier > 0.35) {
                    this.speedMultiplier = 0.35;
                }
            } else {
                this.speedMultiplier = this.speedMultiplier - (this.speedMultiplier - 0.07) / 35.0;
                if (this.speedMultiplier < 0.07) {
                    this.speedMultiplier = 0.07;
                }
            }
        }

        if (this.onGround) {
            this.onGroundCount++;
            if (this.onGroundCount > 5) {
                this.onGroundAndDead();
                return;
            }
        }

        this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
        if (this.getType() == 2 && this.ticksExisted < 20) {
            this.motionY *= 0.95;
        } else {
            this.motionY *= 0.9;
        }

        if (this.isOpenParachute()) {
            this.motionX *= 0.95;
            this.motionZ *= 0.95;
        } else {
            this.motionX *= 0.99;
            this.motionZ *= 0.99;
        }

        this.rotationPitch = 0.0F;
        double yaw = this.rotationYaw;
        double dx = this.prevPosX - this.posX;
        double dz = this.prevPosZ - this.posZ;
        if (dx * dx + dz * dz > 0.001) {
            yaw = (float) (Math.atan2(dx, dz) * 180.0 / Math.PI);
        }

        double yawDiff = MathHelper.wrapDegrees(yaw - this.rotationYaw);
        if (yawDiff > 20.0) {
            yawDiff = 20.0;
        }

        if (yawDiff < -20.0) {
            yawDiff = -20.0;
        }

        if (this.getRiddenByEntity() != null) {
            this.setRotation(this.getRiddenByEntity().rotationYaw, this.rotationPitch);
        } else {
            this.rotationYaw = (float) (this.rotationYaw + yawDiff);
            this.setRotation(this.rotationYaw, this.rotationPitch);
        }

        List<Entity> list = this.world.getEntitiesWithinAABBExcludingEntity(this,
                this.getEntityBoundingBox().grow(0.2, 0.0, 0.2));
        if (!list.isEmpty()) {
            for (Entity entity : list) {
                if (entity != this.getRiddenByEntity() && entity.canBePushed() &&
                        entity instanceof MCH_EntityParachute) {
                    entity.applyEntityCollision(this);
                }
            }
        }

        if (this.getRiddenByEntity() != null && this.getRiddenByEntity().isDead) {
            this.setDead();
        }
    }

    public void onGroundAndDead() {
        this.posY++;
        this.updatePassenger(this.getRiddenByEntity());
        this.setDead();
    }

    public void onWaterSetBoat() {
        if (!this.world.isRemote) {
            if (this.getType() == 2) {
                if (this.getRiddenByEntity() != null) {
                    int px = (int) (this.posX + 0.5);
                    int py = (int) (this.posY + 0.5);
                    int pz = (int) (this.posZ + 0.5);
                    boolean foundBlock = false;

                    for (int y = 0; y < 5 && py + y >= 0 && py + y <= 255; y++) {
                        Block block = W_WorldFunc.getBlock(this.world, px, py - y, pz);
                        if (block == W_Block.getWater()) {
                            py -= y;
                            foundBlock = true;
                            break;
                        }
                    }

                    if (foundBlock) {
                        int countWater = 0;

                        for (int yx = 0; yx < 3 && py + yx >= 0 && py + yx <= 255; yx++) {
                            for (int x = -2; x <= 2; x++) {
                                for (int z = -2; z <= 2; z++) {
                                    Block block = W_WorldFunc.getBlock(this.world, px + x, py - yx, pz + z);
                                    if (block == W_Block.getWater()) {
                                        if (++countWater > 37) {
                                            break;
                                        }
                                    }
                                }
                            }
                        }

                        if (countWater > 37) {
                            EntityBoat entityboat = new EntityBoat(this.world, px, py + 1.0F, pz);
                            entityboat.rotationYaw = this.rotationYaw - 90.0F;
                            this.world.spawnEntity(entityboat);
                            this.getRiddenByEntity().startRiding(entityboat);
                        }
                    }
                }
            }
        }
    }

    public boolean isOpenParachute() {
        return this.getType() != 2 || this.motionY < -0.1;
    }

    public void updatePassenger(@NotNull Entity passenger) {
        if (this.isPassenger(passenger)) {
            double x = -Math.sin(this.rotationYaw * Math.PI / 180.0) * 0.1;
            double z = Math.cos(this.rotationYaw * Math.PI / 180.0) * 0.1;
            passenger.setPosition(this.posX + x, this.posY + this.getMountedYOffset() + passenger.getYOffset(),
                    this.posZ + z);
        }
    }

    protected void writeEntityToNBT(NBTTagCompound nbt) {
        nbt.setByte("ParachuteModelType", (byte) this.getType());
    }

    protected void readEntityFromNBT(NBTTagCompound nbt) {
        this.setType(nbt.getByte("ParachuteModelType"));
    }

    @SideOnly(Side.CLIENT)
    public float getShadowSize() {
        return 4.0F;
    }

    public boolean processInitialInteract(@NotNull EntityPlayer player, @NotNull EnumHand hand) {
        return false;
    }

    @Nullable
    @Override
    public Entity getRiddenByEntity() {
        List<Entity> passengers = this.getPassengers();
        return passengers.isEmpty() ? null : passengers.get(0);
    }
}
