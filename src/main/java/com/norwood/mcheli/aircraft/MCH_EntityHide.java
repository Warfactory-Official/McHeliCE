package com.norwood.mcheli.aircraft;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockWall;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.PooledMutableBlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.jetbrains.annotations.NotNull;

import com.norwood.mcheli.helper.entity.IEntitySinglePassenger;
import com.norwood.mcheli.wrapper.W_Entity;
import com.norwood.mcheli.wrapper.W_Lib;

public class MCH_EntityHide extends W_Entity implements IEntitySinglePassenger {

    private static final DataParameter<Integer> ROPE_INDEX = EntityDataManager.createKey(MCH_EntityHide.class,
            DataSerializers.VARINT);
    private static final DataParameter<Integer> AC_ID = EntityDataManager.createKey(MCH_EntityHide.class,
            DataSerializers.VARINT);
    private MCH_EntityAircraft ac;
    private Entity user;
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

    public MCH_EntityHide(World par1World) {
        super(par1World);
        this.setSize(1.0F, 1.0F);
        this.preventEntitySpawning = true;
        this.user = null;
        this.motionX = this.motionY = this.motionZ = 0.0;
    }

    public MCH_EntityHide(World par1World, double x, double y, double z) {
        this(par1World);
        this.posX = x;
        this.posY = y;
        this.posZ = z;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.createRopeIndex(-1);
        this.dataManager.register(AC_ID, 0);
    }

    public void setParent(MCH_EntityAircraft ac, Entity user, int ropeIdx) {
        this.ac = ac;
        this.setRopeIndex(ropeIdx);
        this.user = user;
    }

    protected boolean canTriggerWalking() {
        return false;
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
        return this.height * 0.0 - 0.3;
    }

    @Override
    public boolean attackEntityFrom(DamageSource par1DamageSource, float par2) {
        return false;
    }

    public boolean canBeCollidedWith() {
        return !this.isDead;
    }

    protected void writeEntityToNBT(@NotNull NBTTagCompound nbt) {}

    protected void readEntityFromNBT(@NotNull NBTTagCompound nbt) {}

    @SideOnly(Side.CLIENT)
    public float getShadowSize() {
        return 0.0F;
    }

    public boolean processInitialInteract(@NotNull EntityPlayer player, @NotNull EnumHand hand) {
        return false;
    }

    public void createRopeIndex(int defaultValue) {
        this.dataManager.register(ROPE_INDEX, defaultValue);
    }

    public int getRopeIndex() {
        return this.dataManager.get(ROPE_INDEX);
    }

    public void setRopeIndex(int value) {
        this.dataManager.set(ROPE_INDEX, value);
    }

    @SideOnly(Side.CLIENT)
    public void setPositionAndRotationDirect(double par1, double par3, double par5, float par7, float par8, int par9,
                                             boolean teleport) {
        this.paraPosRotInc = par9 + 10;
        this.paraX = par1;
        this.paraY = par3;
        this.paraZ = par5;
        this.paraYaw = par7;
        this.paraPitch = par8;
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
        if (this.user != null && !this.world.isRemote) {
            if (this.ac != null) {
                this.dataManager.set(AC_ID, this.ac.getEntityId());
            }

            this.user.startRiding(this);
            this.user = null;
        }

        if (this.ac == null && this.world.isRemote) {
            int id = this.dataManager.get(AC_ID);
            if (id > 0) {
                Entity entity = this.world.getEntityByID(id);
                if (entity instanceof MCH_EntityAircraft) {
                    this.ac = (MCH_EntityAircraft) entity;
                }
            }
        }

        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        this.fallDistance = 0.0F;
        Entity riddenByEntity = this.getRiddenByEntity();
        if (riddenByEntity != null) {
            riddenByEntity.fallDistance = 0.0F;
        }

        if (this.ac != null) {
            if (!this.ac.isRepelling()) {
                this.setDead();
            }

            int id = this.getRopeIndex();
            if (id >= 0) {
                Vec3d v = this.ac.getRopePos(id);
                this.posX = v.x;
                this.posZ = v.z;
            }
        }

        this.setPosition(this.posX, this.posY, this.posZ);
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
            Entity riddenByEntity = this.getRiddenByEntity();
            if (riddenByEntity != null) {
                this.setRotation(riddenByEntity.prevRotationYaw, this.rotationPitch);
            }
        } else {
            this.setPosition(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
            this.motionX *= 0.99;
            this.motionY *= 0.95;
            this.motionZ *= 0.99;
        }
    }

    public void onUpdateServer() {
        this.motionY = this.motionY - (this.onGround ? 0.01 : 0.03);
        if (this.onGround) {
            this.onGroundAndDead();
        } else {
            this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
            this.motionY *= 0.9;
            this.motionX *= 0.95;
            this.motionZ *= 0.95;
            int id = this.getRopeIndex();
            if (this.ac != null && id >= 0) {
                Vec3d v = this.ac.getRopePos(id);
                if (Math.abs(this.posY - v.y) > Math.abs(this.ac.ropesLength) + 5.0F) {
                    this.onGroundAndDead();
                }
            }

            Entity riddenByEntity = this.getRiddenByEntity();
            if (riddenByEntity != null && riddenByEntity.isDead) {
                this.setDead();
            }
        }
    }

    private boolean getCollisionBoxes(@Nullable Entity entityIn, AxisAlignedBB aabb, List<AxisAlignedBB> outList) {
        int i = MathHelper.floor(aabb.minX) - 1;
        int j = MathHelper.ceil(aabb.maxX) + 1;
        int k = MathHelper.floor(aabb.minY) - 1;
        int l = MathHelper.ceil(aabb.maxY) + 1;
        int i1 = MathHelper.floor(aabb.minZ) - 1;
        int j1 = MathHelper.ceil(aabb.maxZ) + 1;
        WorldBorder worldborder = this.world.getWorldBorder();
        boolean flag = entityIn != null && entityIn.isOutsideBorder();
        boolean flag1 = entityIn != null && this.world.isInsideWorldBorder(entityIn);
        IBlockState iblockstate = Blocks.STONE.getDefaultState();
        PooledMutableBlockPos blockpos$pooledmutableblockpos = PooledMutableBlockPos.retain();

        try {
            for (int k1 = i; k1 < j; k1++) {
                for (int l1 = i1; l1 < j1; l1++) {
                    boolean flag2 = k1 == i || k1 == j - 1;
                    boolean flag3 = l1 == i1 || l1 == j1 - 1;
                    if ((!flag2 || !flag3) &&
                            this.world.isBlockLoaded(blockpos$pooledmutableblockpos.setPos(k1, 64, l1))) {
                        for (int i2 = k; i2 < l; i2++) {
                            if (!flag2 && !flag3 || i2 != l - 1) {
                                if (entityIn != null && flag == flag1) {
                                    entityIn.setOutsideBorder(!flag1);
                                }

                                blockpos$pooledmutableblockpos.setPos(k1, i2, l1);
                                IBlockState iblockstate1;
                                if (!worldborder.contains(blockpos$pooledmutableblockpos) && flag1) {
                                    iblockstate1 = iblockstate;
                                } else {
                                    iblockstate1 = this.world.getBlockState(blockpos$pooledmutableblockpos);
                                }

                                iblockstate1.addCollisionBoxToList(this.world, blockpos$pooledmutableblockpos, aabb,
                                        outList, entityIn, false);
                            }
                        }
                    }
                }
            }
        } finally {
            blockpos$pooledmutableblockpos.release();
        }

        return !outList.isEmpty();
    }

    public List<AxisAlignedBB> getCollidingBoundingBoxes(Entity par1Entity, AxisAlignedBB par2AxisAlignedBB) {
        List<AxisAlignedBB> list = new ArrayList<>();
        this.getCollisionBoxes(par1Entity, par2AxisAlignedBB, list);
        if (par1Entity != null) {
            List<Entity> list1 = this.world.getEntitiesWithinAABBExcludingEntity(par1Entity,
                    par2AxisAlignedBB.grow(0.25));

            for (Entity entity : list1) {
                if (!W_Lib.isEntityLivingBase(entity) && !(entity instanceof MCH_EntitySeat) &&
                        !(entity instanceof MCH_EntityHitBox)) {
                    AxisAlignedBB axisalignedbb = entity.getCollisionBoundingBox();
                    if (axisalignedbb != null && axisalignedbb.intersects(par2AxisAlignedBB)) {
                        list.add(axisalignedbb);
                    }

                    axisalignedbb = par1Entity.getCollisionBox(entity);
                    if (axisalignedbb != null && axisalignedbb.intersects(par2AxisAlignedBB)) {
                        list.add(axisalignedbb);
                    }
                }
            }
        }

        return list;
    }

    public void move(@NotNull MoverType type, double x, double y, double z) {
        this.world.profiler.startSection("move");
        double d2 = x;
        double d3 = y;
        double d4 = z;
        List<AxisAlignedBB> list1 = this.getCollidingBoundingBoxes(this, this.getEntityBoundingBox().expand(x, y, z));
        AxisAlignedBB axisalignedbb = this.getEntityBoundingBox();
        if (y != 0.0) {
            int k = 0;

            for (int l = list1.size(); k < l; k++) {
                y = list1.get(k).calculateYOffset(this.getEntityBoundingBox(), y);
            }

            this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0, y, 0.0));
        }

        boolean flag = this.onGround || d3 != y && d3 < 0.0;
        if (x != 0.0) {
            int j5 = 0;

            for (int l5 = list1.size(); j5 < l5; j5++) {
                x = list1.get(j5).calculateXOffset(this.getEntityBoundingBox(), x);
            }

            if (x != 0.0) {
                this.setEntityBoundingBox(this.getEntityBoundingBox().offset(x, 0.0, 0.0));
            }
        }

        if (z != 0.0) {
            int k5 = 0;

            for (int i6 = list1.size(); k5 < i6; k5++) {
                z = list1.get(k5).calculateZOffset(this.getEntityBoundingBox(), z);
            }

            if (z != 0.0) {
                this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0, 0.0, z));
            }
        }

        if (this.stepHeight > 0.0F && flag && (d2 != x || d4 != z)) {
            double d14 = x;
            double d6 = y;
            double d7 = z;
            y = this.stepHeight;
            AxisAlignedBB axisalignedbb1 = this.getEntityBoundingBox();
            this.setEntityBoundingBox(axisalignedbb);
            List<AxisAlignedBB> list = this.getCollidingBoundingBoxes(this,
                    this.getEntityBoundingBox().expand(d2, y, d4));
            AxisAlignedBB axisalignedbb2 = this.getEntityBoundingBox();
            AxisAlignedBB axisalignedbb3 = axisalignedbb2.expand(d2, 0.0, d4);
            double d8 = y;

            for (AxisAlignedBB axisAlignedBB1 : list) {
                d8 = axisAlignedBB1.calculateYOffset(axisalignedbb3, d8);
            }

            axisalignedbb2 = axisalignedbb2.offset(0.0, d8, 0.0);
            double d18 = d2;

            for (AxisAlignedBB element : list) {
                d18 = element.calculateXOffset(axisalignedbb2, d18);
            }

            axisalignedbb2 = axisalignedbb2.offset(d18, 0.0, 0.0);
            double d19 = d4;

            for (AxisAlignedBB item : list) {
                d19 = item.calculateZOffset(axisalignedbb2, d19);
            }

            axisalignedbb2 = axisalignedbb2.offset(0.0, 0.0, d19);
            AxisAlignedBB axisalignedbb4 = this.getEntityBoundingBox();
            double d20 = y;

            for (AxisAlignedBB value : list) {
                d20 = value.calculateYOffset(axisalignedbb4, d20);
            }

            axisalignedbb4 = axisalignedbb4.offset(0.0, d20, 0.0);
            double d21 = d2;

            for (AxisAlignedBB bb : list) {
                d21 = bb.calculateXOffset(axisalignedbb4, d21);
            }

            axisalignedbb4 = axisalignedbb4.offset(d21, 0.0, 0.0);
            double d22 = d4;

            for (AxisAlignedBB alignedBB : list) {
                d22 = alignedBB.calculateZOffset(axisalignedbb4, d22);
            }

            axisalignedbb4 = axisalignedbb4.offset(0.0, 0.0, d22);
            double d23 = d18 * d18 + d19 * d19;
            double d9 = d21 * d21 + d22 * d22;
            if (d23 > d9) {
                x = d18;
                z = d19;
                y = -d8;
                this.setEntityBoundingBox(axisalignedbb2);
            } else {
                x = d21;
                z = d22;
                y = -d20;
                this.setEntityBoundingBox(axisalignedbb4);
            }

            for (AxisAlignedBB axisAlignedBB : list) {
                y = axisAlignedBB.calculateYOffset(this.getEntityBoundingBox(), y);
            }

            this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0, y, 0.0));
            if (d14 * d14 + d7 * d7 >= x * x + z * z) {
                x = d14;
                y = d6;
                z = d7;
                this.setEntityBoundingBox(axisalignedbb1);
            }
        }

        this.world.profiler.endSection();
        this.world.profiler.startSection("rest");
        this.resetPositionToBB();
        this.collidedHorizontally = d2 != x || d4 != z;
        this.collidedVertically = d3 != y;
        this.onGround = this.collidedVertically && d3 < 0.0;
        this.collided = this.collidedHorizontally || this.collidedVertically;
        int j6 = MathHelper.floor(this.posX);
        int i1 = MathHelper.floor(this.posY - 0.2F);
        int k6 = MathHelper.floor(this.posZ);
        BlockPos blockpos = new BlockPos(j6, i1, k6);
        IBlockState iblockstate = this.world.getBlockState(blockpos);
        if (iblockstate.getMaterial() == Material.AIR) {
            BlockPos blockpos1 = blockpos.down();
            IBlockState iblockstate1 = this.world.getBlockState(blockpos1);
            Block block1 = iblockstate1.getBlock();
            if (block1 instanceof BlockFence || block1 instanceof BlockWall || block1 instanceof BlockFenceGate) {
                iblockstate = iblockstate1;
                blockpos = blockpos1;
            }
        }

        this.updateFallState(y, this.onGround, iblockstate, blockpos);
        if (d2 != x) {
            this.motionX = 0.0;
        }

        if (d4 != z) {
            this.motionZ = 0.0;
        }

        Block block = iblockstate.getBlock();
        if (d3 != y) {
            block.onLanded(this.world, this);
        }

        try {
            this.doBlockCollisions();
        } catch (Throwable var45) {
            CrashReport crashreport = CrashReport.makeCrashReport(var45, "Checking entity block collision");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Entity being checked for collision");
            this.addEntityCrashInfo(crashreportcategory);
            throw new ReportedException(crashreport);
        }

        this.world.profiler.endSection();
    }

    public void onGroundAndDead() {
        this.posY += 0.5;
        this.updatePassenger(this.getRiddenByEntity());
        this.setDead();
    }

    @Nullable
    @Override
    public Entity getRiddenByEntity() {
        List<Entity> passengers = this.getPassengers();
        return passengers.isEmpty() ? null : passengers.get(0);
    }
}
